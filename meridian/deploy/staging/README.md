# Staging manifests

Kubernetes manifests for the staging environment. Apply in order: `namespace`, `configmap`,
secrets, `deployment`, `service`.

## Status

**Never applied.** There is no cluster. These have been parsed as YAML; they have **not** been
schema-validated, because `kubectl --validate` needs a live API server to fetch the OpenAPI
schema from. Expect to fix things on first apply.

## Decisions worth knowing

**Two replicas.** A node eviction should not be an outage, and running more than one instance in
staging surfaces single-instance assumptions before production does. See the migration note below —
this is the setting that makes it matter.

**No CPU limit, memory limited.** Throttling a JVM mid-GC produces latency spikes worse than the
noisy-neighbour problem a CPU limit prevents. Memory is limited because the image sets
`MaxRAMPercentage`, so the container limit is where heap size is actually configured — one place,
not two.

**Read-only root filesystem** with an `emptyDir` at `/tmp`, non-root user, all capabilities
dropped. The service holds payout details and identity documents (NFR-08, REG-16).

**Startup probe separate from liveness.** First boot includes JVM warmup and Flyway; the startup
probe absorbs it, which lets liveness stay aggressive afterwards without killing pods that are
merely slow to start.

**Readiness reports the datasource**, so a pod that loses its database leaves the service rather
than serving errors.

## Before this is applied

**Flyway runs at application start, and two replicas start together.** Flyway takes a lock, so the
second instance waits rather than corrupting anything — but a long migration becomes a rolling
deployment that stalls, and a failed one leaves an instance crash-looping. Decide before staging
holds data worth keeping: either run migrations as a Kubernetes `Job` before the rollout, or accept
start-time migration and keep them short. This is the same decision at any replica count above one.

**Secrets are not in this repository and must not be.** `meridian-secrets` is referenced by the
deployment but not defined here. It needs the datasource URL, username and password at minimum;
payment aggregator and eTIMS credentials follow in Phase 3. No secret manager is configured yet,
which is itself an open item.

**The image reference is a placeholder.** `meridian:replaced-at-deploy` is substituted by the
deploy job that does not exist yet.

**No ingress.** The service is `ClusterIP`. Exposing it needs an ingress or gateway, a certificate,
and a hostname — none of which exist, and which should not be added before the actuator endpoints
are moved to a separate management port.

**Actuator is on the main port.** `application.yaml` exposes health, info, metrics, prometheus and
modulith on 8080. That is acceptable inside a cluster with no ingress. It is not acceptable once
anything is publicly routable.
