# privacy-guarantees Specification

## Purpose

Makes the app's privacy promise ("your spending data never leaves the phone") enforceable: the build rejects any configuration that could send data off the device outside the one approved path, the on-device model download.

## Requirements

### Requirement: Network client confined to the model-download module
The build SHALL fail when any module other than the model-download module declares a dependency on an HTTP client library, directly or through another module's API.

#### Scenario: HTTP client added to an unapproved module
- **WHEN** a developer adds an HTTP client dependency to any module other than the model-download module and runs the build
- **THEN** the build fails with a message naming the offending module and dependency

#### Scenario: HTTP client in the model-download module
- **WHEN** the model-download module declares an HTTP client dependency and the build runs
- **THEN** the build succeeds

#### Scenario: Leak through an API dependency
- **WHEN** a module exposes an HTTP client as an API dependency and another module that is not model-download depends on it
- **THEN** the build fails and names the consuming module

### Requirement: Internet permission confined to the model-download module
Only the model-download module SHALL contribute the `android.permission.INTERNET` permission to the merged app manifest. The build SHALL fail if any other source contributes it.

#### Scenario: Permission declared elsewhere
- **WHEN** the app module or any module other than model-download declares the INTERNET permission and the build runs
- **THEN** the build fails and names where the permission was declared

#### Scenario: No model-download permission yet
- **WHEN** the model-download module does not yet declare the INTERNET permission
- **THEN** the merged app manifest contains no INTERNET permission

### Requirement: No analytics or crash-reporting SDKs
The build SHALL fail when any module declares a dependency on a known analytics, advertising or crash-reporting SDK.

#### Scenario: Analytics SDK added
- **WHEN** a developer adds an analytics, advertising or crash-reporting SDK to any module and runs the build
- **THEN** the build fails with a message naming the module and the SDK

### Requirement: Privacy checks run in CI
The privacy checks SHALL run on every continuous-integration build of the main branch, and a failing check SHALL fail that CI run.

#### Scenario: Violation pushed
- **WHEN** a commit that breaks a privacy check is pushed to main
- **THEN** the CI run for that commit fails and reports which privacy check failed
