
#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

VERSION="${1:?Usage: deploy.sh VERSION}"

if [[ ! "$VERSION" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "Invalid deployment version"
  exit 1
fi

REGION="${AWS_REGION:-us-east-1}"
export AWS_DEFAULT_REGION="$REGION"

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
BUCKET="waslah-deploy-${ACCOUNT_ID}-${REGION}"
REGISTRY="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

APP_DIR="/opt/waslah"
RELEASE_DIR="${APP_DIR}/releases/${VERSION}"
ENV_FILE="${APP_DIR}/.env"

mkdir -p "$RELEASE_DIR"

echo "==> Downloading release ${VERSION}"

aws s3 cp \
  "s3://${BUCKET}/releases/${VERSION}/compose.prod.yaml" \
  "${RELEASE_DIR}/compose.prod.yaml"

aws s3 cp \
  "s3://${BUCKET}/releases/${VERSION}/01-schema.pg17.sql" \
  "${RELEASE_DIR}/01-schema.pg17.sql"

echo "==> Loading configuration from SSM"

python3 - "$REGION" "$REGISTRY" "$VERSION" "$ENV_FILE" <<'PY'
import json
import os
import subprocess
import sys

region, registry, version, output = sys.argv[1:]

parameters = {}
next_token = None

while True:
    command = [
        "aws", "ssm", "get-parameters-by-path",
        "--path", "/waslah/prod",
        "--recursive",
        "--with-decryption",
        "--region", region,
        "--no-paginate",
        "--output", "json",
    ]

    if next_token:
        command.extend(["--next-token", next_token])

    result = subprocess.check_output(command, text=True)
    response = json.loads(result)

    for parameter in response.get("Parameters", []):
        name = parameter["Name"].rsplit("/", 1)[-1]
        parameters[name] = parameter["Value"]

    next_token = response.get("NextToken")
    if not next_token:
        break

required = [
    "DB_URL", "DB_HOST", "DB_USERNAME", "DB_PASSWORD",
    "JWT_SECRET", "INTERNAL_TOKEN",
    "APP_BASE_URL", "ADMIN_USERNAME",
    "ADMIN_EMAIL", "ADMIN_PASSWORD",
]

missing = [key for key in required if not parameters.get(key)]
if missing:
    raise SystemExit("Missing SSM parameters: " + ", ".join(missing))

parameters["ECR_REGISTRY"] = registry
parameters["DEPLOY_VERSION"] = version

def quote(value):
    return json.dumps(str(value).replace("$", "$$"))

with open(output, "w", encoding="utf-8") as file:
    for key, value in sorted(parameters.items()):
        if not key.replace("_", "").isalnum():
            raise SystemExit("Invalid environment variable name")
        file.write(f"{key}={quote(value)}\n")

os.chmod(output, 0o600)
PY

echo "==> Checking RDS schema"

python3 - "$ENV_FILE" "$RELEASE_DIR/01-schema.pg17.sql" <<'PY'
import json
import os
import subprocess
import sys

env_path, schema_path = sys.argv[1:]

values = {}

with open(env_path, encoding="utf-8") as file:
    for line in file:
        key, value = line.rstrip("\n").split("=", 1)
        values[key] = json.loads(value).replace("$$", "$")

required = ["DB_HOST", "DB_USERNAME", "DB_PASSWORD"]
for key in required:
    if not values.get(key):
        raise SystemExit(f"Missing {key}")

# The password is passed through stdin to Docker,
# not exposed as a command-line argument.
docker_args = [
    "docker", "run", "--rm", "-i",
    "--network", "host",
    "-e", "PGPASSWORD",
    "-e", "PGSSLMODE=require",
    "postgres:17",
    "psql",
    "-X",
    "-v", "ON_ERROR_STOP=1",
    "-h", values["DB_HOST"],
    "-U", values["DB_USERNAME"],
    "-d", "waslah",
]

env = os.environ.copy()
env["PGPASSWORD"] = values["DB_PASSWORD"]
env["PGSSLMODE"] = "require"

check = subprocess.run(
    docker_args + [
        "-tAc",
        "SELECT count(*) FROM information_schema.tables "
        "WHERE table_schema='public' AND table_type='BASE TABLE';"
    ],
    env=env,
    capture_output=True,
    text=True,
    check=True,
)

table_count = int(check.stdout.strip())

if table_count == 18:
    print("RDS schema already initialized: 18 tables")

elif table_count == 0:
    print("Initializing RDS schema...")

    with open(schema_path, "rb") as schema:
        subprocess.run(
            docker_args + ["-1"],
            stdin=schema,
            env=env,
            check=True,
        )

    print("RDS schema initialized successfully")

else:
    raise SystemExit(
        f"Unexpected RDS schema state: {table_count} tables. "
        "Stopping to avoid modifying existing data."
    )
PY

echo "==> Authenticating to Amazon ECR"

aws ecr get-login-password --region "$REGION" |
  docker login \
    --username AWS \
    --password-stdin "$REGISTRY" >/dev/null

echo "==> Pulling Docker images"

docker compose \
  --env-file "$ENV_FILE" \
  -f "$RELEASE_DIR/compose.prod.yaml" \
  pull

echo "==> Starting WASLAH"

docker compose \
  --env-file "$ENV_FILE" \
  -f "$RELEASE_DIR/compose.prod.yaml" \
  up -d --remove-orphans --wait --wait-timeout 300

echo "==> WASLAH ${VERSION} deployment completed"

docker compose \
  --env-file "$ENV_FILE" \
  -f "$RELEASE_DIR/compose.prod.yaml" \
  ps
