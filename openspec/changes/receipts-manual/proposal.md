# Proposal

## Why

The app shell exists, but there is nothing to keep in it: Receipts is a placeholder and no module stores data. Every later MVP change builds on stored receipts. Capture and AI extraction fill in a receipt for the user to confirm, and Ask queries them. So the receipt model, encrypted on-device storage, and the screens to view and edit receipts come first. Manual entry also keeps the app useful before the on-device model is downloaded, or without it.

## What Changes

- **Receipt domain model** in `:domain`: a receipt has a merchant, purchase date, total (an amount in minor units plus an ISO 4217 currency code), a category from a fixed list, and an optional note. The model also includes the repository interface, the use cases (monitor all, monitor one, save, delete) and input validation.
- **Room, encrypted with SQLCipher**, in `:data:receipts`: the first entity, DAO and database (schema v1, exported), plus a repository implementation and mappers. The database passphrase is random, wrapped by an Android Keystore key, and stored in no-backup storage.
- **Room convention plugin** `pocketreceipts.android.room` in `build-logic`: the Room Gradle plugin, KSP, the Room runtime/compiler and the schema export directory. New version-catalog entries for Room and SQLCipher.
- **Receipts feature** in `:feature:receipts`, replacing the placeholder:
  - a list of receipts, newest first, with an empty state
  - a receipt detail screen
  - one add/edit form for manual entry, with validation
  - delete from the detail screen, after confirmation
- **Navigation**: the receipts feature adds detail and edit destinations that open on top of the Receipts tab. Receipts stays selected while they are shown.
- **Backup rules**: the receipts database and its wrapped key are excluded from cloud backup and device transfer. A restored database could never be opened anyway, because the Keystore key does not leave the device.

## Capabilities

### New Capabilities
- `receipt-management`: what users can do with receipts: browse the list, view one, add one by hand, edit it, and delete it. Also covers the fields and validation rules of a receipt, and that receipts persist across app restarts.

### Modified Capabilities
- `app-navigation`: Receipts is no longer a placeholder. Screens opened within a tab keep that tab selected, and switching tabs leaves them.
- `privacy-guarantees`: receipt data is encrypted at rest and excluded from cloud backup and device-to-device transfer.

## Impact

- **Modules**: `:domain` (model, repository interface, use cases), `:data:receipts` (Room, SQLCipher, Keystore, repository, Hilt bindings), `:feature:receipts` (screens, ViewModels, UI mappers, nav keys), `:app` (wires the new entries, backup rules), `build-logic` (Room convention plugin).
- **Dependencies**: Room (runtime, ktx, compiler, Gradle plugin) and `net.zetetic:sqlcipher-android` (+ `androidx.sqlite`). None is an HTTP client or a tracking SDK, and none adds the INTERNET permission, so the privacy checks still pass.
- **APK size**: SQLCipher ships native libraries (a few MB per ABI). This is accepted for encryption at rest. R8 and ABI splitting come in release-readiness.
- **Out of scope**: line items, user-defined categories, currency conversion, receipt images, search and filtering, and undo after delete. Capture and AI changes add images. Ask adds queries over receipts.
