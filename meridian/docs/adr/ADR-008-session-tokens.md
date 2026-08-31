# ADR-008 — Opaque, revocable session tokens

- **Status:** Accepted
- **Date:** 31 August 2026
- **Phase:** 1
- **Satisfies:** BR-IAM-02, BR-IAM-03, BR-IAM-06, BR-VND-10, BR-SEC-07

## Context

The frontend holds an httpOnly `meridian_session` cookie and sends its value as
`Authorization: Bearer <token>`. It treats the value as opaque, so the choice of token format is
entirely the backend's.

`spring-boot-starter-security-oauth2-resource-server` is on the classpath, which implies
self-issued JWTs. That is the conventional choice and it is the wrong one here.

The deciding requirement is revocation. BR-VND-10 requires admin suspension of a vendor to
delist their products **immediately**. BR-SEC-07 requires every automated restriction to be
reversible and visible. BR-STL-08 blocks settlement when payout details changed inside a
cooling-off window. Each assumes the platform can withdraw access at a known instant.

A JWT cannot be withdrawn. A suspended vendor keeps a working token until it expires — fifteen
minutes of continued access on a fraud suspension, during which they can still accept orders.
The usual repair is a revocation list checked on every request, which is a database lookup per
request: exactly the cost a JWT was adopted to avoid, plus a second source of truth.

## Decision

Opaque session tokens stored in the database.

- 32 bytes from `SecureRandom`, base64url-encoded.
- Stored as a SHA-256 hash, never in plaintext. The token is high-entropy, so a fast hash is
  correct here — it is not a password, and it must be found by hash on every request. Using a
  slow KDF would add per-request latency for no gain against an offline attacker who cannot
  brute-force 256 bits regardless.
- Revocation is setting `revoked_at`. It takes effect on the next request, with no window.
- One-time codes are the opposite case and are hashed with BCrypt: six digits is a million
  possibilities, so the hash must be slow, and it is paired with an attempt limit and a short
  expiry.

`spring-boot-starter-security-oauth2-resource-server` stays on the classpath for validating
tokens issued by an external identity provider, should one be introduced. It is not used to
issue our own.

## Consequences

**We get** revocation that is immediate and total, sessions that are enumerable per account so
"sign out everywhere" and admin-forced logout are straightforward, and no signing keys to
distribute, rotate or leak.

**We accept** a database read per authenticated request. It is a primary-key lookup on an indexed
hash, it is a strong candidate for the Caffeine cache already configured — bounded by a short TTL
so revocation stays prompt — and NFR-04's ten thousand concurrent buyers is well within what this
costs.

**We accept** that the token table grows and needs pruning. Expired rows are evidence for
BR-IAM-06's authentication log until they are archived, so pruning is a retention decision
under 15.3, not a cleanup job to be added thoughtlessly.

**We must** treat the token as a credential in transit and at rest: never logged, never in a URL,
never in an error message.

## Alternatives considered

**Self-issued JWT with a short TTL.** Rejected: it trades a revocation guarantee the requirements
need for a database read they can afford.

**JWT plus a revocation list.** Rejected: the per-request lookup returns, and correctness now
depends on two stores agreeing.
