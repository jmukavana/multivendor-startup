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

The backend job's `build` step is the same command that passes locally. Note that `./gradlew build`
initially failed at `bootJar` on a duplicate `application-modules.json` contributed by both the main
resources and the AOT output; `build.gradle.kts` now sets a duplicates strategy for that task. The
first CI run would have failed without it.

## What has not been verified

**Nothing has run in CI, because the repository has no git remote.** Until one exists these files
are inert. That is the outstanding half of the Phase 0 exit criterion, which reads "pipeline
deploys to staging automatically".

The container image was built and run locally with podman: 337 MB, starts as the unprivileged
`meridian` user, emits ECS JSON, and reaches the datasource before failing as expected with no
database attached.

## Not built yet

**Deployment.** BRD Section 1 names Google Kubernetes Engine. There is no cluster, no registry, no
service account and no staging environment, so there is nothing to deploy to and no credentials to
deploy with. The `image` job builds an image and does not push it.

Manifests for staging are written and validated in `meridian/deploy/staging/`. What remains is
infrastructure, in order:

1. A container registry and a service account with push rights.
2. A staging GKE cluster.
3. Repository secrets for registry and cluster credentials.
4. A `deploy` job gated on `github.ref == 'refs/heads/main'`.

**Database migrations in the pipeline.** Flyway runs at application start, so a deployment applies
migrations. That is acceptable for a single instance and becomes a problem the first time two
replicas start together. Decide before staging carries data worth keeping.

**Secrets.** No secret management is configured, per the environment. Payment aggregator and eTIMS
credentials arrive in Phase 3 and must not reach the repository.
