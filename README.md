# RackXLate

RackXLate is an Android utility that acts as a translation layer for barcode scans, primarily for use with enterprise devices using Symbol/Zebra DataWedge.

## High-Level Function

The application intercepts barcode data broadcast by DataWedge, looks up a corresponding value in a local database, and then uses an Accessibility Service to paste the translated result into the currently focused input field. This allows for seamless integration between external location identifiers (e.g., on a rack label) and an internal system's location identifiers.

## Core Features

*   **Accessibility Service Integration**: Pastes translated text into any active input field on the device, removing the need for manual entry.
*   **DataWedge Receiver**: Listens for broadcasts from DataWedge to capture scan data in real-time.
*   **Local Database Lookup**: Translates external scan values to internal IDs using a bundled SQLite database.
*   **Periodic Database Updates**: Uses `WorkManager` to periodically and automatically download database updates from a remote server in the background.
*   **Configurable Settings**: A user-facing settings screen allows for configuration of:
    *   The specific DataWedge Intent to listen for.
    *   A regular expression for validating scanned codes.
    *   The URL for the database update manifest.
    *   The warehouse code for database queries.

## How It Works

1.  **DataWedge Configuration**: DataWedge on the scanning device is configured to broadcast an Intent with the scan data.
2.  **Broadcast Reception**: The `ScanReceiver` in the app listens for this specific Intent.
3.  **Data Validation**: The receiver validates the incoming scan data against a user-defined regular expression.
4.  **Database Query**: If the data is valid, the app queries its local SQLite database to find the corresponding internal `LOCID`.
5.  **Paste Action**: The `RackXLateAccessibilityService` is triggered to paste the result (either the translated ID or the original data if no translation was found) into the currently focused UI element.
6.  **Background Updates**: A `DBUpdateWorker` runs periodically to check a remote manifest file, and if a new database version is available, it downloads, verifies, and installs it.

## Setup & Configuration

1.  **Enable the Service**: The user must enable the RackXLate Accessibility Service in the device's Android Settings.
2.  **Configure DataWedge**: The device's DataWedge profile must be set up to broadcast an Intent with the action that is configured in the app's settings.
3.  **App Settings**: The user must configure the manifest URL in the app's settings to enable database updates.
