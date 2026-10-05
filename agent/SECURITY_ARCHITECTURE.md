# Security Architecture — Authentication & Authorization

This document decides and records how identity, authentication, and authorization work in
FHIRBridge. It extends the target model already named in
[`layer2-interoperability-layer.md`](layer2-interoperability-layer.md) §2.1
("SMART on FHIR / OAuth2-based") to cover human users, not only system-to-system callers.

## 1. The decision

**OAuth2 (OIDC) for the protocol, JWT for the token format. Not a choice between the two —
they compose.**

- **OAuth2 / OpenID Connect** governs *how* a caller proves who they are and what they're
  allowed to do: login flows, consent, token issuance, token refresh.
- **JWT** is *what* FHIRBridge receives and checks on every request: a signed, short-lived,
  self-contained access token. FHIRBridge never stores a session — it validates the token's
  signature and claims on each call (stateless Resource Server), which matches the
  statelessness requirement already in the layer2 spec (§5, "Statelessness of the mediation
  logic itself").

Rejected alternative: server-side sessions (cookie + session store). It would work, but it
reintroduces state into a layer that is explicitly supposed to stay stateless and multi-tenant,
and it doesn't extend to Layer 1 system callers, which have no human session at all.

## 2. Actors and grants

FHIRBridge has two kinds of caller: human users operating a UI, and Layer 1 systems calling the
API directly. They use different OAuth2 grants but land on the same JWT validation path.

| Actor | What they do | Grant | Token subject |
|---|---|---|---|
| Front desk officer | Registers patients (demographic intake) | Authorization Code + PKCE | Staff user |
| Doctor / Practitioner | Uploads encounters, reads patients under their care | Authorization Code + PKCE | Staff user |
| Patient | Reads their own record only | Authorization Code + PKCE | Patient user, token carries their own `patient_id` |
| Layer 1 system (EMR, lab, claims) | Pushes/pulls FHIR resources, no human present | Client Credentials | System/service identity |

PKCE is mandatory for every interactive flow (web, mobile, SPA) — no implicit grant, no grant
without a code challenge.

## 3. Token shape

Access tokens are JWTs, short-lived (5–15 min), signed by the identity provider (IdP), validated
by FHIRBridge against the IdP's published JWKS (no shared secret, no local credential store).

Minimum claims FHIRBridge relies on:

| Claim | Used for |
|---|---|
| `sub` | Caller identity, recorded on every `AuditEvent` (layer2 §2.5) |
| `roles` (or `realm_access.roles`) | RBAC — maps to `front-desk`, `doctor`, `patient`, `system` |
| `scope` | Coarse interaction grant — see §4 |
| `patient_id` (custom claim, patient tokens only) | Compartment check — see §5 |
| `iss`, `aud`, `exp` | Standard validation (issuer, audience, expiry) |

Refresh tokens exist only for interactive (human) flows and are never issued to system clients
beyond what Client Credentials already provides (short-lived access token, re-request on
expiry — no refresh token needed for M2M).

## 4. Scopes (SMART-style, per resource + interaction)

One flat API key for everything is explicitly ruled out by the layer2 spec (§2.1). Scopes are
per resource type and interaction, following the SMART on FHIR convention:

| Scope | Meaning |
|---|---|
| `user/Patient.write` | Create/update Patient demographics (front desk) |
| `user/Patient.read` | Read any Patient the caller is authorized for (front desk, doctor) |
| `user/Encounter.write` | Create/upload an Encounter (doctor) |
| `patient/*.read` | Read own compartment only (patient role) |
| `system/Patient.read`, `system/Patient.write` | Layer 1 system calls (M2M) |

Scopes gate *which interaction on which resource type* is possible at all. They do not by
themselves say *whose* record — that's the compartment check in §5.

## 5. Authorization model: RBAC + patient compartment

Two checks apply, in order:

1. **RBAC (role → endpoint)** — coarse-grained. Enforced declaratively
   (`@PreAuthorize("hasRole('DOCTOR')")` or equivalent) at the controller/method boundary.

   | Endpoint | Allowed roles |
   |---|---|
   | `POST /api/patients` | `front-desk`, `system` |
   | `GET /api/patients/{id}` | `front-desk`, `doctor`, `patient` (own record only — see below), `system` |
   | `GET /api/patients` | `front-desk`, `doctor` |
   | `PUT /api/patients/{id}` | `front-desk`, `system` |
   | `DELETE /api/patients/{id}` | `front-desk` (admin-level only, in practice) |
   | `POST /api/practitioners/{id}/encounters` (planned) | `doctor`, `system` |

2. **Compartment check (ABAC)** — fine-grained, cannot be expressed as a static role rule.
   A `patient`-role token must only ever resolve to its own record: on every read/write, the
   service layer compares the resource's `Patient.id` against the token's `patient_id` claim
   and rejects (403, with an `AuditEvent(outcome=denied)` per layer2 §2.5) on mismatch. This
   check lives in the service layer, not just the gateway, because "is this MY record" is a
   data-dependent decision no annotation can make before the record is loaded.

Role alone is necessary but not sufficient for the patient actor — this is the one place RBAC
underspecifies the requirement, which is why the compartment check is called out separately
rather than folded into "roles."

## 6. Identity provider

Recommended: **Keycloak**, self-hosted, OIDC-native, free, and the de facto pairing for HAPI
FHIR-based servers in comparable health-exchange stacks. It issues the JWTs described above and
exposes the JWKS endpoint FHIRBridge validates against.

Alternatives (Auth0, Okta, AWS Cognito) work identically from FHIRBridge's side — it only talks
standard OIDC/JWKS, so the IdP is swappable without touching Resource Server code. Keycloak is
the default because it avoids a per-seat SaaS cost while the project is pre-production, and
because it's the IdP already assumed by most national health-exchange reference stacks this
project's architecture docs are modeled on.

## 7. Implementation sketch (Spring Security)

Not yet implemented — this is the shape to build against:

- `spring-boot-starter-oauth2-resource-server` (JWT support) — validates the bearer token on
  every request against the IdP's JWKS, no custom crypto code.
- A `JwtAuthenticationConverter` that maps the `roles` claim onto Spring `GrantedAuthority`
  (`ROLE_FRONT_DESK`, `ROLE_DOCTOR`, `ROLE_PATIENT`, `ROLE_SYSTEM`) so `@PreAuthorize` works
  unchanged.
- Method-level security (`@EnableMethodSecurity`) on controller methods for the RBAC table in
  §5.1.
- A small compartment-check helper in the service layer (not a filter) for §5.2 — it needs the
  loaded resource to compare against, so it cannot run before the data is fetched.
- `SecurityFilterChain` requiring authentication on all `/api/**` routes — no anonymous writes,
  matching layer2 §2.1 verbatim ("No anonymous writes").

## 8. Non-functional alignment

Carried over from layer2 §5, now concretely satisfied by this model:

- **Security**: TLS in transit (enforced at the ingress, not in app code); tokens are bearer
  secrets and are never logged; the IdP holds credentials, FHIRBridge never stores a password.
- **Audit**: `sub` and `roles` from the validated token are exactly what each `AuditEvent`
  needs to answer "who did this" — once the audit trail itself exists (still a gap, see §9).
- **Statelessness**: JWT validation is the only per-request security work; no session store,
  no sticky routing requirement.

## 9. Gap against the current codebase

Honest accounting, in the style of the layer2 gap table:

| Capability | Current state |
|---|---|
| Authentication (any kind) | Not implemented — all endpoints are unauthenticated |
| OAuth2 Resource Server config | Not implemented |
| Identity provider | Not provisioned |
| Role claim → authority mapping | Not implemented |
| RBAC on controllers | Not implemented |
| Patient-compartment (ABAC) check | Not implemented |
| Scope design (`user/*`, `patient/*`, `system/*`) | Designed here, not enforced in code |
| Audit linking (`sub` → `AuditEvent`) | Blocked on audit trail itself (layer2 §2.5, also not implemented) |

This table is the backlog once there is agreement to act on it, same convention as
[`layer2-interoperability-layer.md`](layer2-interoperability-layer.md)'s own gap section.
Nothing in this document is implemented yet — it is the design to build against next.
