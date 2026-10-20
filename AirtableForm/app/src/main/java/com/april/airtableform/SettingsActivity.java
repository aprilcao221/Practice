package com.april.airtableform;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Enter the Airtable token, then pick which base and table the form fills. */
public class SettingsActivity extends Activity {

    private Prefs prefs;
    private EditText tokenInput;
    private Spinner baseSpinner;
    private Spinner tableSpinner;
    private TextView status;
    private Button saveButton;

    private final List<JSONObject> bases = new ArrayList<>();
    private JSONArray tables = new JSONArray();
    /** Bumped on each base change so a slow, stale table load is ignored. */
    private int tableLoadGeneration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        Ui ui = new Ui(this);

        LinearLayout root = ui.column();

        root.addView(ui.heading("1. Airtable personal access token"));
        root.addView(ui.hint("Create one at airtable.com/create/tokens with the scopes "
                + "data.records:read, data.records:write and schema.bases:read, "
                + "and give it access to the bases you want to use."));
        Button createToken = ui.button("Open airtable.com/create/tokens");
        createToken.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://airtable.com/create/tokens"))));
        root.addView(createToken);

        tokenInput = ui.editText("pat...");
        tokenInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        tokenInput.setTypeface(Typeface.MONOSPACE);
        tokenInput.setText(prefs.token());
        root.addView(tokenInput);

        CheckBox show = new CheckBox(this);
        show.setText("Show token");
        show.setOnCheckedChangeListener((b, checked) -> {
            tokenInput.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    | (checked ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                               : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            tokenInput.setTypeface(Typeface.MONOSPACE);
            tokenInput.setSelection(tokenInput.length());
        });
        root.addView(show);

        Button load = ui.button("Load my bases");
        load.setOnClickListener(v -> loadBases());
        root.addView(load);

        root.addView(ui.heading("2. Base"));
        baseSpinner = new Spinner(this);
        root.addView(baseSpinner);

        root.addView(ui.heading("3. Table to fill in"));
        tableSpinner = new Spinner(this);
        root.addView(tableSpinner);

        status = ui.hint("");
        root.addView(status);

        saveButton = ui.primaryButton("Save and open form");
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(v -> save());
        root.addView(saveButton);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        baseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadTables(bases.get(position).optString("id"));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (!prefs.token().isEmpty()) {
            loadBases();
        }
    }

    private String token() {
        return tokenInput.getText().toString().trim();
    }

    private void loadBases() {
        String token = token();
        if (token.isEmpty()) {
            status.setText("Paste your token first.");
            return;
        }
        status.setText("Loading bases…");
        saveButton.setEnabled(false);
        Background.run(() -> new AirtableApi(token).listBases(), (result, error) -> {
            if (error != null) {
                status.setText(Background.describe(error));
                return;
            }
            prefs.saveToken(token);
            bases.clear();
            bases.addAll(result);
            if (bases.isEmpty()) {
                status.setText("This token cannot see any bases. Add bases to the token "
                        + "on airtable.com.");
                return;
            }
            List<String> names = new ArrayList<>();
            int selected = 0;
            for (int i = 0; i < bases.size(); i++) {
                JSONObject base = bases.get(i);
                names.add(base.optString("name"));
                if (base.optString("id").equals(prefs.baseId())) selected = i;
            }
            baseSpinner.setAdapter(spinnerAdapter(names));
            // Triggers onItemSelected, which loads the tables.
            baseSpinner.setSelection(selected);
            status.setText("");
        });
    }

    private void loadTables(String baseId) {
        int generation = ++tableLoadGeneration;
        String token = token();
        status.setText("Loading tables…");
        saveButton.setEnabled(false);
        tableSpinner.setAdapter(spinnerAdapter(new ArrayList<>()));
        Background.run(() -> new AirtableApi(token).listTables(baseId), (result, error) -> {
            if (generation != tableLoadGeneration) return;
            if (error != null) {
                status.setText(Background.describe(error));
                return;
            }
            tables = result;
            List<String> names = new ArrayList<>();
            int selected = 0;
            for (int i = 0; i < tables.length(); i++) {
                JSONObject table = tables.optJSONObject(i);
                names.add(table.optString("name"));
                if (table.optString("id").equals(prefs.tableId())) selected = i;
            }
            tableSpinner.setAdapter(spinnerAdapter(names));
            tableSpinner.setSelection(selected);
            status.setText("");
            saveButton.setEnabled(tables.length() > 0);
        });
    }

    private void save() {
        int b = baseSpinner.getSelectedItemPosition();
        int t = tableSpinner.getSelectedItemPosition();
        if (b < 0 || b >= bases.size() || t < 0 || t >= tables.length()) {
            Toast.makeText(this, "Choose a base and a table", Toast.LENGTH_SHORT).show();
            return;
        }
        JSONObject base = bases.get(b);
        prefs.saveToken(token());
        prefs.saveSelection(base.optString("id"), base.optString("name"),
                tables.optJSONObject(t).optString("id"), tables);
        setResult(RESULT_OK);
        finish();
    }

    private ArrayAdapter<String> spinnerAdapter(List<String> items) {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }
}
