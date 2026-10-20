package com.april.airtableform;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * App settings and the offline queue, kept in private SharedPreferences.
 * The token never leaves the phone except in requests to api.airtable.com.
 */
final class Prefs {

    private static final String FILE = "airtable_form";
    private static final String TOKEN = "token";
    private static final String BASE_ID = "base_id";
    private static final String BASE_NAME = "base_name";
    private static final String TABLE_ID = "table_id";
    /** JSON of every table in the chosen base (needed for linked-record pickers). */
    private static final String TABLES_JSON = "tables_json";
    /** JSON array of {baseId, tableId, fields} records waiting to be sent. */
    private static final String PENDING = "pending";

    private final SharedPreferences sp;

    Prefs(Context context) {
        sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    String token() { return sp.getString(TOKEN, ""); }
    String baseId() { return sp.getString(BASE_ID, ""); }
    String baseName() { return sp.getString(BASE_NAME, ""); }
    String tableId() { return sp.getString(TABLE_ID, ""); }

    boolean isConfigured() {
        return !token().isEmpty() && !baseId().isEmpty() && !tableId().isEmpty()
                && tables() != null;
    }

    void saveToken(String token) {
        sp.edit().putString(TOKEN, token).apply();
    }

    void saveSelection(String baseId, String baseName, String tableId, JSONArray tables) {
        sp.edit()
                .putString(BASE_ID, baseId)
                .putString(BASE_NAME, baseName)
                .putString(TABLE_ID, tableId)
                .putString(TABLES_JSON, tables.toString())
                .apply();
    }

    void saveTables(JSONArray tables) {
        sp.edit().putString(TABLES_JSON, tables.toString()).apply();
    }

    JSONArray tables() {
        String json = sp.getString(TABLES_JSON, null);
        if (json == null) return null;
        try {
            return new JSONArray(json);
        } catch (JSONException e) {
            return null;
        }
    }

    /** The schema of the chosen table, or null if it is missing. */
    JSONObject selectedTable() {
        return findTable(tables(), tableId());
    }

    static JSONObject findTable(JSONArray tables, String tableId) {
        if (tables == null) return null;
        for (int i = 0; i < tables.length(); i++) {
            JSONObject table = tables.optJSONObject(i);
            if (table != null && tableId.equals(table.optString("id"))) {
                return table;
            }
        }
        return null;
    }

    JSONArray pending() {
        try {
            return new JSONArray(sp.getString(PENDING, "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private void savePending(JSONArray pending) {
        sp.edit().putString(PENDING, pending.toString()).commit();
    }

    /** Adds a submission to the queue, or replaces the queued copy with the same id. */
    void putPending(JSONObject submission) {
        synchronized (Prefs.class) {
            String id = submission.optString("id");
            JSONArray pending = pending();
            JSONArray updated = new JSONArray();
            boolean replaced = false;
            for (int i = 0; i < pending.length(); i++) {
                JSONObject item = pending.optJSONObject(i);
                if (item != null && id.equals(item.optString("id"))) {
                    updated.put(submission);
                    replaced = true;
                } else {
                    updated.put(item);
                }
            }
            if (!replaced) updated.put(submission);
            savePending(updated);
        }
    }

    /** Removes the queued submission with this id, returning it (or null). */
    JSONObject removePending(String id) {
        synchronized (Prefs.class) {
            JSONArray pending = pending();
            JSONArray kept = new JSONArray();
            JSONObject removed = null;
            for (int i = 0; i < pending.length(); i++) {
                JSONObject item = pending.optJSONObject(i);
                if (removed == null && item != null && id.equals(item.optString("id"))) {
                    removed = item;
                } else {
                    kept.put(item);
                }
            }
            savePending(kept);
            return removed;
        }
    }
}
