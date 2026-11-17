# Spec Delta

## MODIFIED Requirements

### Requirement: Internet permission confined to the model-download module
Only the model-download module SHALL contribute the `android.permission.INTERNET` permission to the merged app manifest. The build SHALL fail if any other source contributes it. A third-party library whose INTERNET permission the app removes in its own manifest doesn't contribute it; only libraries the build explicitly lists for this may rely on that removal.

#### Scenario: Permission declared elsewhere
- **WHEN** the app module or any module other than model-download declares the INTERNET permission and the build runs
- **THEN** the build fails and names where the permission was declared

#### Scenario: No model-download permission yet
- **WHEN** the model-download module does not yet declare the INTERNET permission
- **THEN** the merged app manifest contains no INTERNET permission

#### Scenario: Listed library permission removed by the app
- **WHEN** ML Kit's data-transport upload backend declares INTERNET and the app manifest removes the INTERNET permission
- **THEN** the build succeeds, and the merged app manifest contains no INTERNET permission

#### Scenario: Listed library permission not removed
- **WHEN** ML Kit's data-transport upload backend declares INTERNET and the app manifest doesn't remove it
- **THEN** the build fails and names the backend

### Requirement: No analytics or crash-reporting SDKs
The build SHALL fail when any module declares a dependency on a known analytics, advertising or crash-reporting SDK. The build SHALL also fail when the merged app manifest registers a third-party usage-logging upload backend, such as the Google data-transport backend that ML Kit uses to send usage logs, so that such logs stay on the device.

#### Scenario: Analytics SDK added
- **WHEN** a developer adds an analytics, advertising or crash-reporting SDK to any module and runs the build
- **THEN** the build fails with a message naming the module and the SDK

#### Scenario: Usage-logging backend registered
- **WHEN** a library brings in Google's data-transport upload backend and the app manifest doesn't remove its backend registration
- **THEN** the build fails with a message naming the backend

#### Scenario: ML Kit with its usage logging switched off
- **WHEN** the app depends on ML Kit and the app manifest removes the upload backend's registration and INTERNET permission
- **THEN** the build succeeds, the merged app manifest registers no upload backend and has no INTERNET permission, and ML Kit's usage events are dropped on the device
