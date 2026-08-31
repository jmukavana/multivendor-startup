# Pipelines

| Workflow | Triggers on | Does |
|---|---|---|
| `backend.yml` | `meridian/**` | JDK 25, `./gradlew build`, module boundary verification (AC-NFR-03), test and module-diagram artefacts, container image build |
| `frontend.yml` | `meridian-frontend/**` | Node 22, `biome check`, `tsc --noEmit`, `next build` |

Path filters mean a frontend change does not run the backend suite, which spends about a minute
starting PostgreSQL under Testcontainers.

## What has been verified

The frontend job's steps were run locally against this tree and pass: `npm ci`, `npm run lint`
(21 files, clean), `npx tsc --noEmit`, and `npm run build`.

The backend job's `build` step is the same command that passes locally.

## What has not been verified

**Nothing has run in CI, because the repository has no git remote.** Until one exists these files
are inert. That is the outstanding half of the Phase 0 exit criterion, which reads "pipeline
deploys to staging automatically".

**`bootBuildImage` is unverified.** It needs a Docker daemon; this machine has podman, and Spring
Boot's builder needs `DOCKER_HOST` pointed at the podman socket to work with it. GitHub-hosted
runners provide Docker, so the step should work there, but it has not been proven.

## Not built yet

**Deployment.** BRD Section 1 names Google Kubernetes Engine. There is no cluster, no registry, no
service account and no staging environment, so there is nothing to deploy to and no credentials to
deploy with. The `image` job builds an image and does not push it.

Adding deployment needs, in order:

1. A container registry and a service account with push rights.
2. A staging GKE cluster.
3. Repository secrets for registry and cluster credentials.
4. Kubernetes manifests — deployment, service, config, secrets, and probes wired to the
   `/actuator/health/liveness` and `/actuator/health/readiness` endpoints already enabled in
   `application.yaml`.
5. A `deploy` job gated on `github.ref == 'refs/heads/main'`.

**Database migrations in the pipeline.** Flyway runs at application start, so a deployment applies
migrations. That is acceptable for a single instance and becomes a problem the first time two
replicas start together. Decide before staging carries data worth keeping.

**Secrets.** No secret management is configured, per the environment. Payment aggregator and eTIMS
credentials arrive in Phase 3 and must not reach the repository.
