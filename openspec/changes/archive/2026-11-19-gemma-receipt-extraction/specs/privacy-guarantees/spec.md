# Spec Delta

## MODIFIED Requirements

### Requirement: Internet permission confined to the model-download module
Only the model-download module SHALL contribute the `android.permission.INTERNET` permission to the merged app manifest. The build SHALL fail if any other source contributes it. A third-party library that the build explicitly lists as an upload backend MAY also declare INTERNET, but only while the merged manifest registers no usage-logging upload backend; such a library then has no way to send anything.

#### Scenario: Permission declared elsewhere
- **WHEN** the app module or any module other than model-download declares the INTERNET permission and the build runs
- **THEN** the build fails and names where the permission was declared

#### Scenario: Model download declares the permission
- **WHEN** the model-download module declares the INTERNET permission and the build runs
- **THEN** the build succeeds and the merged app manifest contains the INTERNET permission

#### Scenario: No model-download permission yet
- **WHEN** the model-download module does not declare the INTERNET permission
- **THEN** any INTERNET permission in the merged app manifest comes only from a listed library whose upload backend is unregistered

#### Scenario: Listed library permission removed by the app
- **WHEN** ML Kit's data-transport upload backend declares INTERNET and the app manifest removes the backend's registration
- **THEN** the build succeeds

#### Scenario: Listed library permission not removed
- **WHEN** ML Kit's data-transport upload backend declares INTERNET and its backend registration is in the merged manifest
- **THEN** the build fails and names the backend
