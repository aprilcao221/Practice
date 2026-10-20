package com.april.airtableform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Photos waiting to be uploaded. Each is re-encoded as a JPEG no larger than
 * {@link #MAX_EDGE} pixels, so it uploads quickly and stays under Airtable's 5 MB limit.
 * Files live in app storage until uploaded, so queued photos survive a restart.
 */
final class PhotoStore {

    private static final int MAX_EDGE = 2048;
    private static final int JPEG_QUALITY = 85;

    private PhotoStore() {}

    /** Where finished photos are kept until they are uploaded. */
    static File photoDir(Context context) {
        File dir = new File(context.getFilesDir(), "photos");
        dir.mkdirs();
        return dir;
    }

    /** Where the camera app writes the full-size picture; served by {@link PhotoProvider}. */
    static File cameraDir(Context context) {
        File dir = new File(context.getCacheDir(), "camera");
        dir.mkdirs();
        return dir;
    }

    static File newCameraFile(Context context) {
        return new File(cameraDir(context), "capture-" + UUID.randomUUID() + ".jpg");
    }

    /** Copies a picture into app storage, upright and resized. Call off the main thread. */
    static File importPhoto(Context context, Uri source) throws IOException {
        int rotation = readRotation(context, source);

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = open(context, source)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("This file is not a picture the phone can read");
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(Math.max(bounds.outWidth, bounds.outHeight), MAX_EDGE);
        Bitmap bitmap;
        try (InputStream in = open(context, source)) {
            bitmap = BitmapFactory.decodeStream(in, null, options);
        }
        if (bitmap == null) {
            throw new IOException("Could not read the picture");
        }

        float scale = Math.min(1f,
                (float) MAX_EDGE / Math.max(bitmap.getWidth(), bitmap.getHeight()));
        if (scale < 1f || rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            matrix.postRotate(rotation);
            Bitmap transformed = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (transformed != bitmap) bitmap.recycle();
            bitmap = transformed;
        }

        File out = new File(photoDir(context), "photo-" + UUID.randomUUID() + ".jpg");
        try (OutputStream stream = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream);
        } finally {
            bitmap.recycle();
        }
        return out;
    }

    /**
     * Deletes photo files no queued submission refers to, left behind when the app
     * was closed with photos on the form. Call at start-up, off the main thread.
     */
    static int deleteUnused(Context context, JSONArray pending) {
        Set<String> keep = new HashSet<>();
        for (int i = 0; i < pending.length(); i++) {
            JSONObject submission = pending.optJSONObject(i);
            JSONArray photos = submission == null ? null : submission.optJSONArray("photos");
            for (int j = 0; photos != null && j < photos.length(); j++) {
                JSONObject photo = photos.optJSONObject(j);
                if (photo != null) keep.add(photo.optString("path"));
            }
        }
        int deleted = 0;
        for (File dir : new File[] {photoDir(context), cameraDir(context)}) {
            File[] files = dir.listFiles();
            for (int i = 0; files != null && i < files.length; i++) {
                if (!keep.contains(files[i].getPath()) && files[i].delete()) deleted++;
            }
        }
        return deleted;
    }

    /** A small bitmap for the form's preview row. */
    static Bitmap thumbnail(File file, int size) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), bounds);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(
                Math.min(bounds.outWidth, bounds.outHeight), size);
        return BitmapFactory.decodeFile(file.getPath(), options);
    }

    private static int sampleSize(int edge, int target) {
        int sample = 1;
        while (edge / (sample * 2) >= target) sample *= 2;
        return sample;
    }

    private static int readRotation(Context context, Uri source) {
        try (InputStream in = open(context, source)) {
            int orientation = new ExifInterface(in).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException | RuntimeException e) {
            return 0;
        }
    }

    private static InputStream open(Context context, Uri uri) throws IOException {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) throw new IOException("Could not open the picture");
        return in;
    }
}
