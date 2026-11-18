# Deployment Evidence and Final Submission Checklist

## Required screenshots / links (fill with actual evidence)
- [ ] GitHub repository URL: https://github.com/Noor-Shafeeq-Ashoor/updated-waslah
- [ ] Latest successful GitHub Actions workflow URL and run result: **ADD**
- [ ] AWS EC2 instance screenshot (instance ID/IP may be redacted): **ADD**
- [ ] Amazon ECR repository/images and image tag: **ADD**
- [ ] AWS Systems Manager deployment execution result: **ADD**
- [ ] S3 deployment artifact location (no private tokens): **ADD**
- [ ] PostgreSQL hosting confirmation (EC2 container or RDS, as actually deployed): **ADD**
- [ ] Running frontend URL / screenshot: **ADD**
- [ ] `/actuator/health` or readiness endpoint screenshot per service: **ADD**
- [ ] Docker Compose running containers screenshot: **ADD**
- [ ] Kafka producer/consumer test evidence: **ADD**
- [ ] Swagger/OpenAPI evidence for all four services: **ADD**
- [ ] Final ERD, architecture diagram, user stories and planning committed: **ADD**

## Deployment procedure
1. Set AWS permissions, OIDC trust/role, ECR repositories, deployment bucket and SSM-managed EC2 host.
2. Configure required CI variables/secrets and EC2 runtime environment **outside Git**.
3. Trigger GitHub Actions on `master` or manually; verify tests and frontend build.
4. Verify ECR image tags, S3 deployment artifacts, and SSM execution.
5. On EC2, verify Compose services, healthchecks, logs, network access and notification flow.
6. Capture redacted screenshots and record the actual deployment URLs.

**Do not claim deployment is complete solely because workflow files exist.** Keep the final screenshots free of JWTs, database passwords, mail credentials and private keys.
