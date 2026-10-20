package com.april.airtableform;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Minimal client for the Airtable Web API, using only the Android framework. */
final class AirtableApi {

    private static final String API_ROOT = "https://api.airtable.com/v0/";
    private static final String CONTENT_ROOT = "https://content.airtable.com/v0/";
    /** Upper bound on linked records loaded into a picker. */
    private static final int MAX_LINKED_RECORDS = 2000;

    /** An error Airtable reported (bad token, missing scope, invalid value...). */
    static final class ApiException extends Exception {
        final int status;

        ApiException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    /** A record in a linked table, shown by its primary field. */
    static final class RecordName {
        final String id;
        final String name;

        RecordName(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private final String token;

    AirtableApi(String token) {
        this.token = token;
    }

    /** Returns every base the token can see, as {id, name} objects. */
    List<JSONObject> listBases() throws IOException, ApiException, JSONException {
        List<JSONObject> bases = new ArrayList<>();
        String offset = null;
        do {
            String path = "meta/bases" + (offset == null ? "" : "?offset=" + enc(offset));
            JSONObject page = request("GET", path, null);
            JSONArray items = page.getJSONArray("bases");
            for (int i = 0; i < items.length(); i++) {
                bases.add(items.getJSONObject(i));
            }
            offset = page.optString("offset", null);
        } while (offset != null && !offset.isEmpty());
        return bases;
    }

    /** Returns the tables of a base, each with its fields and their options. */
    JSONArray listTables(String baseId) throws IOException, ApiException, JSONException {
        return request("GET", "meta/bases/" + baseId + "/tables", null).getJSONArray("tables");
    }

    /** Creates one record and returns its id. Field keys are field ids. */
    String createRecord(String baseId, String tableId, JSONObject fields)
            throws IOException, ApiException, JSONException {
        JSONObject body = new JSONObject();
        body.put("fields", fields);
        body.put("typecast", true);
        return request("POST", baseId + "/" + tableId, body).getString("id");
    }

    /** Adds a JPEG to an attachment field of an existing record (max 5 MB). */
    void uploadAttachment(String baseId, String recordId, String fieldId, File photo)
            throws IOException, ApiException, JSONException {
        byte[] bytes = new byte[(int) photo.length()];
        try (InputStream in = new FileInputStream(photo)) {
            int read = 0;
            while (read < bytes.length) {
                int n = in.read(bytes, read, bytes.length - read);
                if (n < 0) throw new IOException("Photo file was cut short");
                read += n;
            }
        }
        JSONObject body = new JSONObject();
        body.put("contentType", "image/jpeg");
        body.put("filename", photo.getName());
        body.put("file", Base64.encodeToString(bytes, Base64.NO_WRAP));
        request("POST", CONTENT_ROOT + baseId + "/" + recordId + "/" + fieldId
                + "/uploadAttachment", body);
    }

    /** Lists records of a table by their primary field, for linked-record pickers. */
    List<RecordName> listRecordNames(String baseId, String tableId, String primaryFieldId)
            throws IOException, ApiException, JSONException {
        List<RecordName> names = new ArrayList<>();
        String offset = null;
        do {
            StringBuilder path = new StringBuilder(baseId).append('/').append(tableId)
                    .append("?pageSize=100&returnFieldsByFieldId=true")
                    .append("&fields%5B%5D=").append(enc(primaryFieldId));
            if (offset != null) {
                path.append("&offset=").append(enc(offset));
            }
            JSONObject page = request("GET", path.toString(), null);
            JSONArray records = page.getJSONArray("records");
            for (int i = 0; i < records.length(); i++) {
                JSONObject record = records.getJSONObject(i);
                Object value = record.getJSONObject("fields").opt(primaryFieldId);
                String name = value == null ? "" : displayValue(value);
                names.add(new RecordName(record.getString("id"),
                        name.isEmpty() ? "(unnamed " + record.getString("id") + ")" : name));
            }
            offset = page.optString("offset", null);
        } while (offset != null && !offset.isEmpty() && names.size() < MAX_LINKED_RECORDS);
        return names;
    }

    private static String displayValue(Object value) {
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length(); i++) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(displayValue(array.opt(i)));
            }
            return sb.toString();
        }
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            return object.optString("name", object.optString("text", object.toString()));
        }
        return String.valueOf(value);
    }

    private JSONObject request(String method, String path, JSONObject body)
            throws IOException, ApiException, JSONException {
        String url = path.startsWith("https://") ? path : API_ROOT + path;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod(method);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) {
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(bytes);
                }
            }
            int status = conn.getResponseCode();
            boolean ok = status >= 200 && status < 300;
            String text = readAll(ok ? conn.getInputStream() : conn.getErrorStream());
            if (!ok) {
                throw new ApiException(status, errorMessage(status, text));
            }
            return new JSONObject(text);
        } finally {
            conn.disconnect();
        }
    }

    private static String errorMessage(int status, String text) {
        String detail = text;
        try {
            Object error = new JSONObject(text).opt("error");
            if (error instanceof JSONObject) {
                JSONObject e = (JSONObject) error;
                detail = e.optString("message", e.optString("type", text));
            } else if (error != null) {
                detail = error.toString();
            }
        } catch (JSONException ignored) {
            // Not JSON; show the raw body.
        }
        if (status == 401) {
            return "Invalid token (401). Check the token in Settings.";
        }
        if (status == 403) {
            return "Not allowed (403): " + detail
                    + "\nMake sure the token has the needed scopes and access to this base.";
        }
        return "Airtable error " + status + ": " + detail;
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        try (InputStream stream = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = stream.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toString("UTF-8");
        }
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }
}
