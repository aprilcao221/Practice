# Airtable Form (Android)

A small Android app for filling in records on your phone. Each record goes
straight into an Airtable table. The app reads the table's fields from Airtable
and builds the form from them, so it works with any of your bases. If you add a
column later, use **Refresh fields** in the menu.

## Install on your phone

1. On your phone, open the repository's **Releases** page and find
   **Airtable Form (latest build)**, or download `AirtableForm-apk` from the
   latest run of the *Build Airtable Form APK* workflow under **Actions**.
2. Download `AirtableForm.apk` and open it.
3. If Android asks, allow your browser or file manager to *install unknown apps*.

Each push to `AirtableForm/` builds a new APK. Every build is signed with the
same key (`app/debug.keystore`), so a new APK installs over the old one and
keeps your settings.

## Set up

1. Create a personal access token at <https://airtable.com/create/tokens> with
   these scopes:
   - `data.records:write`: create records
   - `data.records:read`: list records for linked-record pickers
   - `schema.bases:read`: read the tables and fields

   Under **Access**, add the base(s) you want to fill in.
2. Open the app, paste the token, tap **Load my bases**, choose the base and
   table, and tap **Save and open form**.

The token is stored only in the app's private storage on the phone and is sent
only to `api.airtable.com`.

## What the form supports

| Airtable field | On the form |
| --- | --- |
| Single line text, long text, rich text, email, URL, phone, barcode | Text box |
| Number, currency, percent (type `25` for 25%), rating | Number box |
| Duration | `h:mm` or `h:mm:ss` |
| Checkbox | Checkbox |
| Single select / multiple select | Drop-down / multi-choice list |
| Date / date and time | Date (and time) picker; hold to clear |
| Link to another record | Searchable list of the linked table's records |
| User (single collaborator) | Collaborator's email |
| Attachment | **Take photo** / **From gallery**, with thumbnails (tap one to remove it) |

Fields Airtable fills in itself (formula, lookup, rollup, count, auto number,
created/modified time) don't appear on the form. Neither do multiple-collaborator
fields, which the app doesn't support yet. The form lists these fields at the
bottom.

### Photos

Photos are straightened, resized to at most 2048 px on the long edge and saved
as JPEGs (usually well under 1 MB), which keeps them under Airtable's 5 MB
upload limit. When you submit, the app creates the record first, then uploads
the photos to it one at a time. Uploading needs no extra token scope beyond
`data.records:write`.

## Offline

If there is no connection when you tap **Submit**, the record and its photos are
saved on the phone. The app sends them the next time you open the app or submit
another record. You can also tap the orange banner, or use **Send saved
records** in the menu. If the connection drops part way, the app remembers what
was already sent and won't upload anything twice. If Airtable rejects a saved record (for example, a field was deleted),
you choose whether to keep it or discard it.

## Build it yourself

```
cd AirtableForm
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

Requires JDK 17 and the Android SDK (platform 35), or just open the folder in
Android Studio.
