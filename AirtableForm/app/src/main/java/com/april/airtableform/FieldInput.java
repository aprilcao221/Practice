package com.april.airtableform;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** One Airtable field on the form: its input view and how to turn it into an API value. */
abstract class FieldInput {

    /** Loads the records of a linked table, for linked-record fields. */
    interface LinkedRecords {
        void load(String tableId, Background.Callback<List<AirtableApi.RecordName>> callback);
    }

    /** Thrown when what was typed cannot be sent, with a message for the user. */
    static final class InvalidValue extends Exception {
        InvalidValue(String message) {
            super(message);
        }
    }

    final String fieldId;
    final String name;

    FieldInput(JSONObject field) {
        this.fieldId = field.optString("id");
        this.name = field.optString("name");
    }

    abstract View view();

    /** The value to send, or null to leave the field empty. */
    abstract Object value() throws InvalidValue;

    abstract void clear();

    /**
     * Creates the input for a field, or returns null for fields that cannot be
     * written (formulas, lookups, auto numbers...) or are not supported yet.
     */
    static FieldInput create(Activity activity, JSONObject field, LinkedRecords linked) {
        JSONObject options = field.optJSONObject("options");
        if (options == null) options = new JSONObject();
        switch (field.optString("type")) {
            case "singleLineText":
                return new Text(activity, field, InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false);
            case "multilineText":
            case "richText":
                return new Text(activity, field, InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE, false);
            case "email":
                return new Text(activity, field, InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, false);
            case "url":
                return new Text(activity, field, InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_URI, false);
            case "phoneNumber":
                return new Text(activity, field, InputType.TYPE_CLASS_PHONE, false);
            case "barcode":
                return new Text(activity, field, InputType.TYPE_CLASS_TEXT, true);
            case "singleCollaborator":
                return new Collaborator(activity, field);
            case "number":
            case "currency":
                return new NumberInput(activity, field, options.optInt("precision", 2) > 0
                        ? "Number" : "Whole number", 1);
            case "percent":
                return new NumberInput(activity, field, "Percent, e.g. 25 for 25%", 100);
            case "rating":
                return new NumberInput(activity, field,
                        "1 to " + options.optInt("max", 5), 1);
            case "duration":
                return new Duration(activity, field);
            case "checkbox":
                return new Check(activity, field);
            case "singleSelect":
                return new SingleSelect(activity, field, choiceNames(options));
            case "multipleSelects":
                return new MultiSelect(activity, field, choiceNames(options));
            case "date":
                return new DateInput(activity, field, false);
            case "dateTime":
                return new DateInput(activity, field, true);
            case "multipleRecordLinks":
                return new RecordLinks(activity, field, options.optString("linkedTableId"),
                        options.optBoolean("prefersSingleRecordLink", false), linked);
            default:
                return null;
        }
    }

    private static List<String> choiceNames(JSONObject options) {
        List<String> names = new ArrayList<>();
        JSONArray choices = options.optJSONArray("choices");
        if (choices != null) {
            for (int i = 0; i < choices.length(); i++) {
                JSONObject choice = choices.optJSONObject(i);
                if (choice != null) names.add(choice.optString("name"));
            }
        }
        return names;
    }

    // ---- Text-like fields -------------------------------------------------------------

    static class Text extends FieldInput {
        final EditText edit;
        private final boolean barcode;

        Text(Activity activity, JSONObject field, int inputType, boolean barcode) {
            super(field);
            this.barcode = barcode;
            edit = new EditText(activity);
            edit.setInputType(inputType);
            if ((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0) {
                edit.setMinLines(3);
                edit.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
            }
        }

        @Override View view() { return edit; }

        String text() {
            return edit.getText().toString().trim();
        }

        @Override
        Object value() throws InvalidValue {
            String text = text();
            if (text.isEmpty()) return null;
            if (barcode) {
                try {
                    return new JSONObject().put("text", text);
                } catch (org.json.JSONException e) {
                    throw new InvalidValue(e.getMessage());
                }
            }
            return text;
        }

        @Override void clear() { edit.setText(""); }
    }

    static final class Collaborator extends Text {
        Collaborator(Activity activity, JSONObject field) {
            super(activity, field, InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, false);
            edit.setHint("Collaborator's email");
        }

        @Override
        Object value() throws InvalidValue {
            String text = text();
            if (text.isEmpty()) return null;
            try {
                return new JSONObject().put("email", text);
            } catch (org.json.JSONException e) {
                throw new InvalidValue(e.getMessage());
            }
        }
    }

    static final class NumberInput extends Text {
        private final double divisor;

        NumberInput(Activity activity, JSONObject field, String hint, double divisor) {
            super(activity, field, InputType.TYPE_CLASS_NUMBER
                    | InputType.TYPE_NUMBER_FLAG_DECIMAL
                    | InputType.TYPE_NUMBER_FLAG_SIGNED, false);
            this.divisor = divisor;
            edit.setHint(hint);
        }

        @Override
        Object value() throws InvalidValue {
            String text = text().replace(',', '.');
            if (text.isEmpty()) return null;
            try {
                return Double.parseDouble(text) / divisor;
            } catch (NumberFormatException e) {
                throw new InvalidValue("“" + name + "” must be a number");
            }
        }
    }

    /** Durations are sent as seconds; typed as h:mm, h:mm:ss or a number of hours. */
    static final class Duration extends Text {
        Duration(Activity activity, JSONObject field) {
            super(activity, field, InputType.TYPE_CLASS_DATETIME
                    | InputType.TYPE_DATETIME_VARIATION_TIME, false);
            edit.setHint("h:mm");
        }

        @Override
        Object value() throws InvalidValue {
            String text = text();
            if (text.isEmpty()) return null;
            try {
                String[] parts = text.split(":");
                if (parts.length == 1) return Double.parseDouble(text) * 3600;
                if (parts.length > 3) throw new NumberFormatException();
                double seconds = 0;
                for (String part : parts) {
                    seconds = seconds * 60 + Double.parseDouble(part.trim());
                }
                // h:mm has one fewer unit than h:mm:ss.
                return parts.length == 2 ? seconds * 60 : seconds;
            } catch (NumberFormatException e) {
                throw new InvalidValue("“" + name + "” must look like 1:30");
            }
        }
    }

    // ---- Checkbox and selects -------------------------------------------------------

    static final class Check extends FieldInput {
        private final CheckBox box;

        Check(Activity activity, JSONObject field) {
            super(field);
            box = new CheckBox(activity);
            box.setText(name);
        }

        @Override View view() { return box; }

        @Override Object value() { return box.isChecked() ? Boolean.TRUE : null; }

        @Override void clear() { box.setChecked(false); }
    }

    static final class SingleSelect extends FieldInput {
        private final Spinner spinner;

        SingleSelect(Activity activity, JSONObject field, List<String> choices) {
            super(field);
            List<String> items = new ArrayList<>();
            items.add("—");
            items.addAll(choices);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                    android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner = new Spinner(activity);
            spinner.setAdapter(adapter);
        }

        @Override View view() { return spinner; }

        @Override
        Object value() {
            return spinner.getSelectedItemPosition() <= 0
                    ? null : spinner.getSelectedItem().toString();
        }

        @Override void clear() { spinner.setSelection(0); }
    }

    /** A button that opens a picker and shows what is chosen. */
    abstract static class Picker extends FieldInput {
        final Activity activity;
        final Button button;
        private final String emptyLabel;

        Picker(Activity activity, JSONObject field, String emptyLabel) {
            super(field);
            this.activity = activity;
            this.emptyLabel = emptyLabel;
            button = new Button(activity);
            button.setAllCaps(false);
            button.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            button.setOnClickListener(v -> open());
            showLabel("");
        }

        abstract void open();

        void showLabel(String label) {
            button.setText(label.isEmpty() ? emptyLabel : label);
        }

        @Override View view() { return button; }
    }

    static final class MultiSelect extends Picker {
        private final String[] choices;
        private final boolean[] checked;

        MultiSelect(Activity activity, JSONObject field, List<String> choices) {
            super(activity, field, "Choose…");
            this.choices = choices.toArray(new String[0]);
            this.checked = new boolean[this.choices.length];
        }

        @Override
        void open() {
            boolean[] draft = checked.clone();
            new AlertDialog.Builder(activity)
                    .setTitle(name)
                    .setMultiChoiceItems(choices, draft, (d, which, isChecked) ->
                            draft[which] = isChecked)
                    .setPositiveButton("Done", (d, w) -> {
                        System.arraycopy(draft, 0, checked, 0, checked.length);
                        refresh();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void refresh() {
            StringBuilder label = new StringBuilder();
            for (int i = 0; i < choices.length; i++) {
                if (!checked[i]) continue;
                if (label.length() > 0) label.append(", ");
                label.append(choices[i]);
            }
            showLabel(label.toString());
        }

        @Override
        Object value() {
            JSONArray array = new JSONArray();
            for (int i = 0; i < choices.length; i++) {
                if (checked[i]) array.put(choices[i]);
            }
            return array.length() == 0 ? null : array;
        }

        @Override
        void clear() {
            java.util.Arrays.fill(checked, false);
            refresh();
        }
    }

    // ---- Dates ------------------------------------------------------------------------

    static final class DateInput extends Picker {
        private final boolean withTime;
        private Calendar chosen;

        DateInput(Activity activity, JSONObject field, boolean withTime) {
            super(activity, field, withTime ? "Pick date and time…" : "Pick a date…");
            this.withTime = withTime;
            button.setOnLongClickListener(v -> {
                clear();
                return true;
            });
        }

        @Override
        void open() {
            Calendar start = chosen != null ? (Calendar) chosen.clone() : Calendar.getInstance();
            new DatePickerDialog(activity, (picker, year, month, day) -> {
                Calendar picked = (Calendar) start.clone();
                picked.set(year, month, day);
                if (!withTime) {
                    set(picked);
                    return;
                }
                new TimePickerDialog(activity, (tp, hour, minute) -> {
                    picked.set(Calendar.HOUR_OF_DAY, hour);
                    picked.set(Calendar.MINUTE, minute);
                    picked.set(Calendar.SECOND, 0);
                    set(picked);
                }, start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE),
                        DateFormat.is24HourFormat(activity)).show();
            }, start.get(Calendar.YEAR), start.get(Calendar.MONTH),
                    start.get(Calendar.DAY_OF_MONTH)).show();
        }

        private void set(Calendar calendar) {
            chosen = calendar;
            java.text.DateFormat display = withTime
                    ? java.text.DateFormat.getDateTimeInstance(
                            java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                    : java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM);
            showLabel(display.format(calendar.getTime()) + "   (hold to clear)");
        }

        @Override
        Object value() {
            if (chosen == null) return null;
            String pattern = withTime ? "yyyy-MM-dd'T'HH:mm:ssXXX" : "yyyy-MM-dd";
            return new SimpleDateFormat(pattern, Locale.US).format(chosen.getTime());
        }

        @Override
        void clear() {
            chosen = null;
            showLabel("");
        }
    }

    // ---- Linked records ---------------------------------------------------------------

    static final class RecordLinks extends Picker {
        private final String linkedTableId;
        private final boolean single;
        private final LinkedRecords linked;
        /** Chosen record ids mapped to their names, in the order chosen. */
        private final Set<String> selectedIds = new LinkedHashSet<>();
        private final List<String> selectedNames = new ArrayList<>();

        RecordLinks(Activity activity, JSONObject field, String linkedTableId, boolean single,
                    LinkedRecords linked) {
            super(activity, field, single ? "Choose a record…" : "Choose records…");
            this.linkedTableId = linkedTableId;
            this.single = single;
            this.linked = linked;
        }

        @Override
        void open() {
            button.setEnabled(false);
            button.setText("Loading…");
            linked.load(linkedTableId, (records, error) -> {
                button.setEnabled(true);
                refresh();
                if (error != null) {
                    new AlertDialog.Builder(activity)
                            .setTitle("Could not load " + name)
                            .setMessage(Background.describe(error))
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
                showDialog(records);
            });
        }

        private void showDialog(List<AirtableApi.RecordName> all) {
            Ui ui = new Ui(activity);
            Set<String> draft = new LinkedHashSet<>(selectedIds);
            List<AirtableApi.RecordName> shown = new ArrayList<>(all);

            EditText search = new EditText(activity);
            search.setHint("Search");
            search.setSingleLine(true);
            ListView list = new ListView(activity);
            list.setChoiceMode(single ? ListView.CHOICE_MODE_SINGLE
                                      : ListView.CHOICE_MODE_MULTIPLE);

            Runnable fill = () -> {
                String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
                shown.clear();
                for (AirtableApi.RecordName r : all) {
                    if (q.isEmpty() || r.name.toLowerCase(Locale.ROOT).contains(q)) {
                        shown.add(r);
                    }
                }
                List<String> labels = new ArrayList<>();
                for (AirtableApi.RecordName r : shown) labels.add(r.name);
                list.setAdapter(new ArrayAdapter<>(activity, single
                        ? android.R.layout.simple_list_item_single_choice
                        : android.R.layout.simple_list_item_multiple_choice, labels));
                for (int i = 0; i < shown.size(); i++) {
                    list.setItemChecked(i, draft.contains(shown.get(i).id));
                }
            };
            fill.run();

            list.setOnItemClickListener((parent, v, position, id) -> {
                String recordId = shown.get(position).id;
                if (single) {
                    draft.clear();
                    draft.add(recordId);
                } else if (list.isItemChecked(position)) {
                    draft.add(recordId);
                } else {
                    draft.remove(recordId);
                }
            });
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable s) { fill.run(); }
            });

            LinearLayout content = new LinearLayout(activity);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(ui.dp(20), ui.dp(8), ui.dp(20), 0);
            content.addView(search);
            if (all.isEmpty()) {
                TextView empty = ui.hint("The linked table has no records.");
                content.addView(empty);
            }
            content.addView(list, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, ui.dp(360)));

            new AlertDialog.Builder(activity)
                    .setTitle(name)
                    .setView(content)
                    .setPositiveButton("Done", (d, w) -> {
                        selectedIds.clear();
                        selectedIds.addAll(draft);
                        selectedNames.clear();
                        for (AirtableApi.RecordName r : all) {
                            if (selectedIds.contains(r.id)) selectedNames.add(r.name);
                        }
                        refresh();
                    })
                    .setNeutralButton("Clear", (d, w) -> clear())
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void refresh() {
            showLabel(String.join(", ", selectedNames));
        }

        @Override
        Object value() {
            if (selectedIds.isEmpty()) return null;
            JSONArray ids = new JSONArray();
            for (String id : selectedIds) ids.put(id);
            return ids;
        }

        @Override
        void clear() {
            selectedIds.clear();
            selectedNames.clear();
            refresh();
        }
    }
}
