package com.april.airtableform;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Sends one form submission: creates the record, then uploads its photos one by one.
 *
 * A submission is a JSON object {id, baseId, tableId, fields, photos: [{fieldId, path}],
 * recordId?}. It is updated as it goes (recordId once created, photos removed once
 * uploaded), so if sending stops part way, the queued copy resumes where it left off.
 */
final class Delivery {

    private Delivery() {}

    static JSONObject newSubmission(String baseId, String tableId, JSONObject fields,
                                    JSONArray photos) throws JSONException {
        return new JSONObject()
                .put("id", UUID.randomUUID().toString())
                .put("baseId", baseId)
                .put("tableId", tableId)
                .put("fields", fields)
                .put("photos", photos);
    }

    static void addPhotos(JSONArray photos, String fieldId, List<File> files)
            throws JSONException {
        for (File file : files) {
            photos.put(new JSONObject().put("fieldId", fieldId).put("path", file.getPath()));
        }
    }

    static boolean recordCreated(JSONObject submission) {
        return submission.has("recordId");
    }

    /** Sends whatever is left of the submission. Call off the main thread. */
    static void send(AirtableApi api, JSONObject submission)
            throws IOException, AirtableApi.ApiException, JSONException {
        String baseId = submission.getString("baseId");
        if (!recordCreated(submission)) {
            String recordId = api.createRecord(baseId, submission.getString("tableId"),
                    submission.getJSONObject("fields"));
            submission.put("recordId", recordId);
        }
        String recordId = submission.getString("recordId");
        JSONArray photos = submission.optJSONArray("photos");
        while (photos != null && photos.length() > 0) {
            JSONObject photo = photos.getJSONObject(0);
            File file = new File(photo.getString("path"));
            if (file.exists()) {
                api.uploadAttachment(baseId, recordId, photo.getString("fieldId"), file);
                file.delete();
            }
            photos.remove(0);
        }
    }

    /** Deletes the photo files of a submission that will not be sent. */
    static void discard(JSONObject submission) {
        JSONArray photos = submission.optJSONArray("photos");
        for (int i = 0; photos != null && i < photos.length(); i++) {
            JSONObject photo = photos.optJSONObject(i);
            if (photo != null) new File(photo.optString("path")).delete();
        }
    }

    static int photoCount(JSONObject submission) {
        JSONArray photos = submission.optJSONArray("photos");
        return photos == null ? 0 : photos.length();
    }
}
