package com.april.airtableform;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The data-entry form for the chosen table. Each submit creates one Airtable record. */
public class MainActivity extends Activity implements FieldInput.Host {

    private static final int REQUEST_SETTINGS = 1;
    private static final int REQUEST_CAMERA = 2;
    private static final int REQUEST_GALLERY = 3;
    private static final String STATE_PHOTO_FIELD = "photo_field";
    private static final String STATE_CAMERA_FILE = "camera_file";
    private static final int MENU_SETTINGS = 1;
    private static final int MENU_REFRESH = 2;
    private static final int MENU_SEND_PENDING = 3;

    private Prefs prefs;
    private Ui ui;
    private final List<FieldInput> inputs = new ArrayList<>();
    private final Map<String, List<AirtableApi.RecordName>> linkedCache = new HashMap<>();
    private Button submitButton;
    private TextView pendingBanner;
    private ScrollView scroll;
    private boolean sendingPending;
    /** The attachment field waiting for the camera or gallery, and the camera's file. */
    private String photoFieldId;
    private File cameraFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        ui = new Ui(this);
        if (savedInstanceState != null) {
            photoFieldId = savedInstanceState.getString(STATE_PHOTO_FIELD);
            String path = savedInstanceState.getString(STATE_CAMERA_FILE);
            cameraFile = path == null ? null : new File(path);
        }
        buildScreen();
        if (savedInstanceState == null) {
            JSONArray pending = prefs.pending();
            Background.run(() -> PhotoStore.deleteUnused(this, pending), (n, e) -> {});
        }
        if (!prefs.isConfigured()) {
            openSettings();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendPending(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_PHOTO_FIELD, photoFieldId);
        if (cameraFile != null) outState.putString(STATE_CAMERA_FILE, cameraFile.getPath());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK) {
            linkedCache.clear();
            buildScreen();
        } else if (requestCode == REQUEST_CAMERA) {
            File captured = cameraFile;
            cameraFile = null;
            if (captured == null) return;
            if (resultCode == RESULT_OK && captured.length() > 0) {
                importPhotos(Collections.singletonList(Uri.fromFile(captured)), captured);
            } else {
                captured.delete();
            }
        } else if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK && data != null) {
            List<Uri> uris = new ArrayList<>();
            ClipData clip = data.getClipData();
            if (clip != null) {
                for (int i = 0; i < clip.getItemCount(); i++) uris.add(clip.getItemAt(i).getUri());
            } else if (data.getData() != null) {
                uris.add(data.getData());
            }
            if (!uris.isEmpty()) importPhotos(uris, null);
        }
    }

    private void openSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_SETTINGS);
    }

    // ---- Photos -----------------------------------------------------------------------

    @Override
    public void pickPhotos(FieldInput.Attachments target, boolean camera) {
        photoFieldId = target.fieldId;
        try {
            if (camera) {
                cameraFile = PhotoStore.newCameraFile(this);
                Uri output = PhotoProvider.uriFor(cameraFile);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, output);
                intent.setClipData(ClipData.newRawUri("photo", output));
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(intent, REQUEST_CAMERA);
            } else {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, "Choose photos"),
                        REQUEST_GALLERY);
            }
        } catch (ActivityNotFoundException e) {
            cameraFile = null;
            Toast.makeText(this, camera ? "No camera app found" : "No gallery app found",
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Resizes the chosen pictures into app storage and adds them to the waiting field. */
    private void importPhotos(List<Uri> uris, File deleteAfter) {
        String fieldId = photoFieldId;
        Toast.makeText(this, uris.size() == 1 ? "Adding photo…"
                : "Adding " + uris.size() + " photos…", Toast.LENGTH_SHORT).show();
        Background.run(() -> {
            List<File> files = new ArrayList<>();
            try {
                for (Uri uri : uris) files.add(PhotoStore.importPhoto(this, uri));
            } finally {
                if (deleteAfter != null) deleteAfter.delete();
            }
            return files;
        }, (files, error) -> {
            if (error != null) {
                showError(error);
                return;
            }
            FieldInput.Attachments target = findAttachments(fieldId);
            if (target == null) {
                for (File file : files) file.delete();
                return;
            }
            target.add(files);
        });
    }

    private FieldInput.Attachments findAttachments(String fieldId) {
        for (FieldInput input : inputs) {
            if (input instanceof FieldInput.Attachments && input.fieldId.equals(fieldId)) {
                return (FieldInput.Attachments) input;
            }
        }
        return null;
    }

    // ---- Building the form ------------------------------------------------------------

    private void buildScreen() {
        for (FieldInput input : inputs) {
            if (input instanceof FieldInput.Attachments) input.clear();
        }
        inputs.clear();
        LinearLayout root = ui.column();
        scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        JSONObject table = prefs.isConfigured() ? prefs.selectedTable() : null;
        if (table == null) {
            setTitle(getString(R.string.app_name));
            root.addView(ui.heading("Connect to Airtable"));
            root.addView(ui.hint("Add your Airtable token and choose the table to fill in."));
            Button setup = ui.primaryButton("Open settings");
            setup.setOnClickListener(v -> openSettings());
            root.addView(setup);
            return;
        }

        setTitle(table.optString("name"));
        root.addView(ui.hint(prefs.baseName() + " › " + table.optString("name")));

        pendingBanner = ui.hint("");
        pendingBanner.setTextColor(0xFFB25E00);
        pendingBanner.setOnClickListener(v -> sendPending(true));
        root.addView(pendingBanner);
        updatePendingBanner();

        String primaryId = table.optString("primaryFieldId");
        List<String> skipped = new ArrayList<>();
        JSONArray fields = table.optJSONArray("fields");
        for (int i = 0; fields != null && i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            FieldInput input = FieldInput.create(this, field, this);
            if (input == null) {
                skipped.add(field.optString("name"));
                continue;
            }
            inputs.add(input);
            if (!(input instanceof FieldInput.Check)) {
                String label = field.optString("name");
                if (field.optString("id").equals(primaryId)) label += "  (primary)";
                root.addView(ui.heading(label));
            }
            String description = field.optString("description");
            if (!description.isEmpty()) {
                root.addView(ui.hint(description));
            }
            root.addView(input.view(), new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        submitButton = ui.primaryButton("Submit to Airtable");
        submitButton.setOnClickListener(v -> submit());
        root.addView(submitButton);

        Button clear = ui.button("Clear form");
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage("Clear everything you've entered?")
                .setPositiveButton("Clear", (d, w) -> clearForm())
                .setNegativeButton("Cancel", null)
                .show());
        root.addView(clear);

        if (!skipped.isEmpty()) {
            root.addView(ui.hint("Not on this form (filled in by Airtable, or not supported "
                    + "yet): " + TextUtils.join(", ", skipped)));
        }
    }

    private void clearForm() {
        for (FieldInput input : inputs) input.clear();
        scroll.post(() -> scroll.fullScroll(View.FOCUS_UP));
    }

    // ---- Submitting -------------------------------------------------------------------

    private void submit() {
        JSONObject submission;
        try {
            JSONObject fields = new JSONObject();
            JSONArray photos = new JSONArray();
            for (FieldInput input : inputs) {
                if (input instanceof FieldInput.Attachments) {
                    Delivery.addPhotos(photos, input.fieldId,
                            ((FieldInput.Attachments) input).files());
                    continue;
                }
                Object value = input.value();
                if (value != null) fields.put(input.fieldId, value);
            }
            if (fields.length() == 0 && photos.length() == 0) {
                Toast.makeText(this, "Fill in at least one field", Toast.LENGTH_SHORT).show();
                return;
            }
            submission = Delivery.newSubmission(prefs.baseId(), prefs.tableId(), fields, photos);
        } catch (FieldInput.InvalidValue | JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        int photoCount = Delivery.photoCount(submission);
        submitButton.setEnabled(false);
        submitButton.setText(photoCount == 0 ? "Sending…"
                : "Sending (with " + photoCount + (photoCount == 1 ? " photo" : " photos")
                        + ")…");
        String token = prefs.token();
        Background.run(() -> {
            Delivery.send(new AirtableApi(token), submission);
            return Boolean.TRUE;
        }, (ok, error) -> {
            submitButton.setEnabled(true);
            submitButton.setText("Submit to Airtable");
            if (error == null) {
                Toast.makeText(this, "Saved to Airtable ✓", Toast.LENGTH_SHORT).show();
                releasePhotosAndClear();
                sendPending(false);
            } else if (error instanceof IOException) {
                // No connection: keep what is left on the phone and send it later.
                prefs.putPending(submission);
                releasePhotosAndClear();
                updatePendingBanner();
                Toast.makeText(this, "No connection. Saved on the phone; it will be sent "
                        + "when you're back online.", Toast.LENGTH_LONG).show();
            } else if (Delivery.recordCreated(submission)) {
                // The record exists but a photo was refused: keep the photos queued.
                prefs.putPending(submission);
                releasePhotosAndClear();
                updatePendingBanner();
                showMessage("The record was saved, but a photo could not be uploaded. "
                        + "It is kept on the phone to retry.\n\n" + Background.describe(error));
            } else {
                // Nothing was saved: leave the form as it is so it can be fixed.
                showError(error);
            }
        });
    }

    /** Clears the form after its photos were handed over to a submission. */
    private void releasePhotosAndClear() {
        for (FieldInput input : inputs) {
            if (input instanceof FieldInput.Attachments) {
                ((FieldInput.Attachments) input).release();
            }
        }
        clearForm();
    }

    private void showError(Exception error) {
        showMessage(Background.describe(error));
    }

    private void showMessage(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Not saved")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // ---- Offline queue ----------------------------------------------------------------

    private void updatePendingBanner() {
        if (pendingBanner == null) return;
        int count = prefs.pending().length();
        pendingBanner.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        pendingBanner.setText(count + (count == 1 ? " record" : " records")
                + " waiting to be sent. Tap to send now.");
        invalidateOptionsMenu();
    }

    /** Outcome of one pass over the offline queue. */
    private static final class PendingResult {
        int sent;
        /** Ids of submissions Airtable refused, e.g. because a field was deleted. */
        final List<String> rejected = new ArrayList<>();
        Exception lastError;
    }

    /** Sends records saved while offline. Records Airtable rejects stay until discarded. */
    private void sendPending(boolean userAsked) {
        JSONArray pending = prefs.pending();
        if (sendingPending || pending.length() == 0 || prefs.token().isEmpty()) {
            if (userAsked && pending.length() == 0) {
                Toast.makeText(this, "Nothing waiting to be sent", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        sendingPending = true;
        String token = prefs.token();
        Background.run(() -> {
            AirtableApi api = new AirtableApi(token);
            PendingResult result = new PendingResult();
            for (int i = 0; i < pending.length(); i++) {
                JSONObject submission = pending.getJSONObject(i);
                String id = submission.optString("id");
                try {
                    Delivery.send(api, submission);
                    prefs.removePending(id);
                    result.sent++;
                } catch (AirtableApi.ApiException e) {
                    prefs.putPending(submission);
                    result.rejected.add(id);
                    result.lastError = e;
                } catch (IOException e) {
                    // Still offline: keep any progress and stop for now.
                    prefs.putPending(submission);
                    result.lastError = e;
                    break;
                }
            }
            return result;
        }, (result, error) -> {
            sendingPending = false;
            updatePendingBanner();
            if (error != null) {
                if (userAsked) showError(error);
                return;
            }
            if (result.sent > 0) {
                Toast.makeText(this, "Sent " + result.sent + " saved "
                        + (result.sent == 1 ? "record" : "records"), Toast.LENGTH_SHORT).show();
            }
            if (!result.rejected.isEmpty()) {
                int n = result.rejected.size();
                new AlertDialog.Builder(this)
                        .setTitle(n + " saved " + (n == 1 ? "record was" : "records were")
                                + " rejected")
                        .setMessage(Background.describe(result.lastError))
                        .setPositiveButton("Keep for later", null)
                        .setNegativeButton("Discard", (d, w) -> {
                            for (String id : result.rejected) {
                                JSONObject removed = prefs.removePending(id);
                                if (removed != null) Delivery.discard(removed);
                            }
                            updatePendingBanner();
                        })
                        .show();
            } else if (userAsked && result.lastError != null) {
                showError(result.lastError);
            }
        });
    }

    // ---- Linked records ---------------------------------------------------------------

    @Override
    public void load(String tableId, Background.Callback<List<AirtableApi.RecordName>> callback) {
        List<AirtableApi.RecordName> cached = linkedCache.get(tableId);
        if (cached != null) {
            callback.onResult(cached, null);
            return;
        }
        JSONObject table = Prefs.findTable(prefs.tables(), tableId);
        if (table == null) {
            callback.onResult(null, new IllegalStateException(
                    "Linked table not found. Use “Refresh fields” in the menu."));
            return;
        }
        String baseId = prefs.baseId();
        String primaryFieldId = table.optString("primaryFieldId");
        Background.run(() -> new AirtableApi(prefs.token())
                .listRecordNames(baseId, tableId, primaryFieldId), (records, error) -> {
            if (error == null) linkedCache.put(tableId, records);
            callback.onResult(records, error);
        });
    }

    // ---- Menu -------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_REFRESH, 0, "Refresh fields");
        menu.add(0, MENU_SEND_PENDING, 0, "Send saved records");
        menu.add(0, MENU_SETTINGS, 0, "Settings");
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem send = menu.findItem(MENU_SEND_PENDING);
        if (send != null) send.setVisible(prefs.pending().length() > 0);
        MenuItem refresh = menu.findItem(MENU_REFRESH);
        if (refresh != null) refresh.setVisible(prefs.isConfigured());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SETTINGS:
                openSettings();
                return true;
            case MENU_SEND_PENDING:
                sendPending(true);
                return true;
            case MENU_REFRESH:
                refreshFields();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /** Re-reads the table's fields from Airtable, e.g. after adding a column or option. */
    private void refreshFields() {
        String baseId = prefs.baseId();
        Toast.makeText(this, "Refreshing fields…", Toast.LENGTH_SHORT).show();
        Background.run(() -> new AirtableApi(prefs.token()).listTables(baseId),
                (tables, error) -> {
                    if (error != null) {
                        showError(error);
                        return;
                    }
                    if (Prefs.findTable(tables, prefs.tableId()) == null) {
                        Toast.makeText(this, "The table no longer exists. Choose another.",
                                Toast.LENGTH_LONG).show();
                        prefs.saveTables(tables);
                        openSettings();
                        return;
                    }
                    prefs.saveTables(tables);
                    linkedCache.clear();
                    buildScreen();
                    Toast.makeText(this, "Fields updated", Toast.LENGTH_SHORT).show();
                });
    }
}
