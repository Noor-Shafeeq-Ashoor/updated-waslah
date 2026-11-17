# CI/CD pipeline

GitHub Actions, defined in `.github/workflows/ci-cd.yml`.

```
git push / pull request
  └─ 1. Build & test     one job per service (identity, career, workspace, notification), in parallel
  └─ 2. Build frontend   npm ci + npm run build
        └─ 3. Images      5 Docker images, tagged with the version; pushed to Amazon ECR from main
              └─ 4. Deploy   EC2 pulls that version and restarts (manual approval, then smoke test)
```

| Stage | What runs | Fails the pipeline when |
| --- | --- | --- |
| Build & test | `mvn verify` per service with the `test` profile (H2 in memory, no PostgreSQL, Kafka or secrets) | a service does not compile or a test fails |
| Frontend | `npm ci`, `npm run build` (TypeScript check + Vite build) | a TypeScript or build error |
| Images | `docker build` for the 4 services and the frontend (validates every Dockerfile); push to ECR from the default branch | a Dockerfile step fails, or the push fails |
| Deploy | AWS Systems Manager runs `/opt/waslah/scripts/deploy.sh <version>` on the EC2 host, which pulls the images, restarts the stack, waits for health checks and runs the smoke test | the deploy script or smoke test fails |

Each stage only starts if the previous one passed. Test reports are attached to every run (Artifacts: `test-reports-<service>`).

## When each stage runs

| Event | Build & test | Frontend | Images | Push | Deploy |
| --- | --- | --- | --- | --- | --- |
| Pull request | yes | yes | build only | no | no |
| Push / merge to the default branch (`main` or `master`) | yes | yes | yes | yes (once AWS is set up) | yes, after approval |
| Manual run with a version | no | no | no | no | that version (redeploy / rollback) |

## Versions

Every run gets one version for all images: `1.0.<run number>` (e.g. `1.0.27`), plus `sha-<commit>` so any image can be traced to its commit. `latest` is never used, so a deploy always names an exact version.

## Switching on push and deploy (done in the AWS step)

The pipeline signs in to AWS with a short-lived role (OpenID Connect); no AWS access keys are stored in GitHub.

1. AWS: an IAM OIDC identity provider for `token.actions.githubusercontent.com`, and a role that only this repository's default branch and `production` environment can assume, allowed to push to ECR and to run SSM commands on the EC2 host.
2. AWS: ECR repositories `waslah/identity-service`, `waslah/career-service`, `waslah/workspace-service`, `waslah/notification-service`, `waslah/frontend`.
3. GitHub → Settings → Secrets and variables → Actions → **Variables**:

| Variable | Example | Turns on |
| --- | --- | --- |
| `AWS_REGION` | `me-south-1` | push + deploy |
| `AWS_ROLE_ARN` | `arn:aws:iam::<account>:role/waslah-github-actions` | push to ECR |
| `EC2_INSTANCE_ID` | `i-0abc123...` | deploy |

Until these are set, the pipeline still builds, tests and builds every image; it just does not push or deploy.

## Protecting the default branch

- **Manual approval before deploy:** GitHub → Settings → Environments → `production` → Required reviewers → add yourself.
- **Failed checks block merging:** GitHub → Settings → Branches → rule for your default branch → Require status checks to pass → select the `Build & test (...)`, `Build frontend` and `Image (...)` checks.

## Rollback

Actions → CI/CD → Run workflow → enter the previous version (for example `1.0.26`). Only the deploy stage runs, with images that are already in ECR, so rollback takes as long as a restart.

## Evidence for the submission

- A green run with all stages (Actions → the run → summary graph).
- A red run where a failing test stopped the pipeline before images were built.
- ECR showing images tagged `1.0.x` and `sha-...`.
- The approval prompt on the `production` environment.
