# Patient Flow

How a Patient request moves through FHIRBridge, end to end.

## Layers

```
Client
  │  JSON (PatientDTO)
  ▼
PatientController        src/main/java/com/zph/fhirbridge/controller/PatientController.java
  │  delegates to
  ▼
PatientService           src/main/java/com/zph/fhirbridge/service/PatientService.java
  │  builds/reads
  ▼
HAPI FHIR Patient model  org.hl7.fhir.r4.model.Patient
  │  stored in
  ▼
In-memory store          Map<String, Patient> (ConcurrentHashMap)
```

- `PatientController` only translates HTTP ↔ `PatientDTO`. No FHIR logic lives here.
- `PatientService` is the only place that touches the HAPI FHIR `Patient` object and the store.
- `FhirConfig` provides the shared `FhirContext` bean (thread-safe, created once) used for JSON serialization.
- `GlobalExceptionHandler` converts thrown exceptions (e.g. `ResourceNotFoundException`) into JSON error responses.

## DTO ↔ FHIR field mapping

`PatientDTO` (flat, API-facing) maps to `Patient` (nested, FHIR-spec-shaped):

| PatientDTO field | Patient (FHIR) path |
|---|---|
| `familyName`, `givenName` | `name[0].family`, `name[0].given[0]` |
| `gender` | `gender` (`AdministrativeGender` code) |
| `birthDate` | `birthDate` |
| `phone` | `telecom[system=phone, use=home]` |
| `email` | `telecom[system=email]` |
| `addressLine`, `city`, `state`, `postalCode`, `country` | `address[0].*` |
| `id` | `id` (server-generated UUID) |

Conversion is one-directional per call:
- `createPatient` / `updatePatient`: DTO → `Patient`
- `toDTO` (private, used by get/list): `Patient` → DTO
- `toFhirJson`: `Patient` → raw FHIR JSON string (bypasses DTO entirely)

## Endpoints (`/api/patients`)

| Method | Path | Action |
|---|---|---|
| POST | `/api/patients` | Build a new `Patient`, assign a UUID, store it |
| GET | `/api/patients/{id}` | Look up by id, return as `PatientDTO` |
| GET | `/api/patients` | Return all stored patients as `PatientDTO` list |
| PUT | `/api/patients/{id}` | Rebuild the `Patient` from the DTO, keep the original id |
| DELETE | `/api/patients/{id}` | Remove from the store |
| GET | `/api/patients/{id}/fhir` | Return the stored `Patient` as pretty-printed FHIR JSON |

## Error handling

- Unknown `id` on get/update/delete → `ResourceNotFoundException` → HTTP 404 (handled by `GlobalExceptionHandler`).
- Invalid input (e.g. unrecognized gender code) → `IllegalArgumentException` → HTTP 400.
- Anything else → HTTP 500.

## Storage note

Patients live only in the JVM's memory (`ConcurrentHashMap` in `PatientService`). Restarting the app clears all data. This is intentional for the current stage of the project — no database is wired up yet.
