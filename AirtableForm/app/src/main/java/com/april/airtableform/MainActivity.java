package com.april.airtableform;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The data-entry form for the chosen table. Each submit creates one Airtable record. */
public class MainActivity extends Activity implements FieldInput.LinkedRecords {

    private static final int REQUEST_SETTINGS = 1;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        ui = new Ui(this);
        buildScreen();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK) {
            linkedCache.clear();
            buildScreen();
        }
    }

    private void openSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_SETTINGS);
    }

    // ---- Building the form ------------------------------------------------------------

    private void buildScreen() {
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
                    + "yet): " + String.join(", ", skipped)));
        }
    }

    private void clearForm() {
        for (FieldInput input : inputs) input.clear();
        scroll.post(() -> scroll.fullScroll(View.FOCUS_UP));
    }

    // ---- Submitting -------------------------------------------------------------------

    private void submit() {
        JSONObject fields = new JSONObject();
        try {
            for (FieldInput input : inputs) {
                Object value = input.value();
                if (value != null) fields.put(input.fieldId, value);
            }
        } catch (FieldInput.InvalidValue e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        } catch (JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        if (fields.length() == 0) {
            Toast.makeText(this, "Fill in at least one field", Toast.LENGTH_SHORT).show();
            return;
        }

        String baseId = prefs.baseId();
        String tableId = prefs.tableId();
        submitButton.setEnabled(false);
        submitButton.setText("Sending…");
        Background.run(() -> new AirtableApi(prefs.token()).createRecord(baseId, tableId, fields),
                (recordId, error) -> {
                    submitButton.setEnabled(true);
                    submitButton.setText("Submit to Airtable");
                    if (error == null) {
                        Toast.makeText(this, "Saved to Airtable ✓", Toast.LENGTH_SHORT).show();
                        clearForm();
                        sendPending(false);
                    } else if (error instanceof IOException) {
                        // No connection: keep the record on the phone and send it later.
                        try {
                            prefs.addPending(baseId, tableId, fields);
                            clearForm();
                            updatePendingBanner();
                            Toast.makeText(this, "No connection. Saved on the phone; it will "
                                    + "be sent when you're back online.", Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            showError(e);
                        }
                    } else {
                        showError(error);
                    }
                });
    }

    private void showError(Exception error) {
        new AlertDialog.Builder(this)
                .setTitle("Not saved")
                .setMessage(Background.describe(error))
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
        int left;
        /** Records Airtable refused, e.g. because a field was deleted. */
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
            List<String> sentItems = new ArrayList<>();
            for (int i = 0; i < pending.length(); i++) {
                JSONObject item = pending.getJSONObject(i);
                if (result.lastError instanceof IOException) break;
                try {
                    api.createRecord(item.getString("baseId"), item.getString("tableId"),
                            item.getJSONObject("fields"));
                    sentItems.add(item.toString());
                    result.sent++;
                } catch (AirtableApi.ApiException e) {
                    result.rejected.add(item.toString());
                    result.lastError = e;
                } catch (IOException e) {
                    result.lastError = e;
                }
            }
            result.left = removeFromPending(sentItems);
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
                            removeFromPending(result.rejected);
                            updatePendingBanner();
                        })
                        .show();
            } else if (userAsked && result.left > 0 && result.lastError != null) {
                showError(result.lastError);
            }
        });
    }

    /** Removes the given queued records (compared as JSON text); returns how many remain. */
    private int removeFromPending(List<String> items) {
        synchronized (Prefs.class) {
            JSONArray pending = prefs.pending();
            JSONArray kept = new JSONArray();
            List<String> toRemove = new ArrayList<>(items);
            for (int i = 0; i < pending.length(); i++) {
                Object item = pending.opt(i);
                if (!toRemove.remove(String.valueOf(item))) kept.put(item);
            }
            prefs.savePending(kept);
            return kept.length();
        }
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
