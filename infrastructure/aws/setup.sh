#!/usr/bin/env bash
# Creates everything WASLAH needs on AWS. Run it in AWS CloudShell (no local install needed):
#
#   GITHUB_REPO=<your-github-user>/<your-repo> GITHUB_BRANCH=master ALERT_EMAIL=you@example.com \
#     bash infrastructure/aws/setup.sh
#
# Safe to run again: existing resources are reused, nothing is deleted.
# Creates: ECR repositories, S3 release bucket, security groups, IAM roles (EC2 + GitHub OIDC),
#          RDS PostgreSQL, EC2 host with Elastic IP, CloudFront (HTTPS), secrets in Parameter Store,
#          CloudWatch log group and alarms, monthly budget alert.
set -euo pipefail

export AWS_REGION="${AWS_REGION:-us-east-1}"
export AWS_DEFAULT_REGION="$AWS_REGION"
: "${GITHUB_REPO:?Set GITHUB_REPO=owner/repo (your GitHub repository)}"
GITHUB_BRANCH="${GITHUB_BRANCH:-master}"
INSTANCE_TYPE="${INSTANCE_TYPE:-t3.large}"      # 8 GB: 4 Java services + Kafka + Nginx
DB_CLASS="${DB_CLASS:-db.t4g.micro}"
ALERT_EMAIL="${ALERT_EMAIL:-}"
BUDGET_USD="${BUDGET_USD:-30}"
MAIL_USERNAME="${MAIL_USERNAME:-not-configured}"
MAIL_APP_PASSWORD="${MAIL_APP_PASSWORD:-not-configured}"

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
BUCKET="waslah-deploy-$ACCOUNT_ID-$AWS_REGION"
PARAMS=/waslah/prod
TAGS="Key=Project,Value=waslah"
step() { echo; echo "==> $*"; }
none() { [ -z "$1" ] || [ "$1" = "None" ] || [ "$1" = "null" ]; }

# Stores a parameter only if it does not exist yet (generated secrets keep their value on re-runs).
param_once() {  # name value type
  aws ssm get-parameter --name "$PARAMS/$1" >/dev/null 2>&1 \
    || aws ssm put-parameter --name "$PARAMS/$1" --value "$2" --type "$3" >/dev/null
}
param_set() {   # name value type  (always overwritten)
  aws ssm put-parameter --name "$PARAMS/$1" --value "$2" --type "$3" --overwrite >/dev/null
}
param_get() { aws ssm get-parameter --name "$PARAMS/$1" --with-decryption --query Parameter.Value --output text; }

# ------------------------------------------------------------------ 1. image registry
step "ECR repositories"
for repo in identity-service career-service workspace-service notification-service frontend; do
  if ! aws ecr describe-repositories --repository-names "waslah/$repo" >/dev/null 2>&1; then
    aws ecr create-repository --repository-name "waslah/$repo" \
      --image-scanning-configuration scanOnPush=true --tags "$TAGS" >/dev/null
    aws ecr put-lifecycle-policy --repository-name "waslah/$repo" --lifecycle-policy-text \
      '{"rules":[{"rulePriority":1,"description":"keep the last 15 images","selection":{"tagStatus":"any","countType":"imageCountMoreThan","countNumber":15},"action":{"type":"expire"}}]}' >/dev/null
    echo "   created waslah/$repo"
  else
    echo "   exists  waslah/$repo"
  fi
done

# ------------------------------------------------------------------ 2. release bucket (compose files + scripts per version)
step "S3 bucket $BUCKET"
if ! aws s3api head-bucket --bucket "$BUCKET" 2>/dev/null; then
  if [ "$AWS_REGION" = "us-east-1" ]; then
    aws s3api create-bucket --bucket "$BUCKET" >/dev/null
  else
    aws s3api create-bucket --bucket "$BUCKET" --create-bucket-configuration "LocationConstraint=$AWS_REGION" >/dev/null
  fi
  aws s3api put-public-access-block --bucket "$BUCKET" --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
fi

# ------------------------------------------------------------------ 3. network: default VPC, two security groups
step "Security groups"
VPC_ID=$(aws ec2 describe-vpcs --filters Name=is-default,Values=true --query 'Vpcs[0].VpcId' --output text)
if none "$VPC_ID"; then echo "No default VPC. Create one with: aws ec2 create-default-vpc"; exit 1; fi
SUBNETS=$(aws ec2 describe-subnets --filters Name=vpc-id,Values="$VPC_ID" Name=default-for-az,Values=true \
          --query 'Subnets[].SubnetId' --output text)
FIRST_SUBNET=$(echo "$SUBNETS" | awk '{print $1}')
sg_id() { aws ec2 describe-security-groups --filters Name=group-name,Values="$1" Name=vpc-id,Values="$VPC_ID" \
          --query 'SecurityGroups[0].GroupId' --output text; }

WEB_SG=$(sg_id waslah-web)
if none "$WEB_SG"; then
  WEB_SG=$(aws ec2 create-security-group --group-name waslah-web --vpc-id "$VPC_ID" \
           --description "WASLAH host: HTTP from CloudFront only, no SSH" --query GroupId --output text)
  CF_PL=$(aws ec2 describe-managed-prefix-lists --filters Name=prefix-list-name,Values=com.amazonaws.global.cloudfront.origin-facing \
          --query 'PrefixLists[0].PrefixListId' --output text)
  aws ec2 authorize-security-group-ingress --group-id "$WEB_SG" \
    --ip-permissions "IpProtocol=tcp,FromPort=80,ToPort=80,PrefixListIds=[{PrefixListId=$CF_PL,Description=CloudFront}]" >/dev/null
fi
DB_SG=$(sg_id waslah-db)
if none "$DB_SG"; then
  DB_SG=$(aws ec2 create-security-group --group-name waslah-db --vpc-id "$VPC_ID" \
          --description "WASLAH database: PostgreSQL from the WASLAH host only" --query GroupId --output text)
  aws ec2 authorize-security-group-ingress --group-id "$DB_SG" \
    --ip-permissions "IpProtocol=tcp,FromPort=5432,ToPort=5432,UserIdGroupPairs=[{GroupId=$WEB_SG}]" >/dev/null
fi
echo "   web $WEB_SG   db $DB_SG"

# ------------------------------------------------------------------ 4. IAM role for the EC2 host
step "IAM role for the EC2 host"
if ! aws iam get-role --role-name waslah-ec2-role >/dev/null 2>&1; then
  aws iam create-role --role-name waslah-ec2-role --assume-role-policy-document \
    '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ec2.amazonaws.com"},"Action":"sts:AssumeRole"}]}' >/dev/null
  for policy in AmazonSSMManagedInstanceCore AmazonEC2ContainerRegistryReadOnly CloudWatchAgentServerPolicy; do
    aws iam attach-role-policy --role-name waslah-ec2-role --policy-arn "arn:aws:iam::aws:policy/$policy"
  done
fi
aws iam put-role-policy --role-name waslah-ec2-role --policy-name waslah-app --policy-document "$(jq -n \
  --arg bucket "arn:aws:s3:::$BUCKET/releases/*" \
  --arg params "arn:aws:ssm:$AWS_REGION:$ACCOUNT_ID:parameter$PARAMS" \
  '{Version:"2012-10-17",Statement:[
     {Effect:"Allow",Action:["s3:GetObject"],Resource:$bucket},
     {Effect:"Allow",Action:["ssm:GetParametersByPath","ssm:GetParameters"],Resource:[$params,($params+"/*")]}]}')"
if ! aws iam get-instance-profile --instance-profile-name waslah-ec2-profile >/dev/null 2>&1; then
  aws iam create-instance-profile --instance-profile-name waslah-ec2-profile >/dev/null
  aws iam add-role-to-instance-profile --instance-profile-name waslah-ec2-profile --role-name waslah-ec2-role
  sleep 15   # new instance profiles take a moment to become usable
fi

# ------------------------------------------------------------------ 5. secrets that do not depend on other resources
step "Secrets in Parameter Store ($PARAMS)"
param_once DB_USERNAME waslah_admin String
param_once DB_PASSWORD "$(openssl rand -hex 20)" SecureString
param_once JWT_SECRET "$(openssl rand -hex 32)" SecureString
param_once INTERNAL_TOKEN "$(openssl rand -hex 32)" SecureString
param_once ADMIN_USERNAME admin String
param_once ADMIN_EMAIL admin@waslah.com String
param_once ADMIN_PASSWORD "$(openssl rand -hex 12)" SecureString
param_once MAIL_USERNAME "$MAIL_USERNAME" String
param_once MAIL_APP_PASSWORD "$MAIL_APP_PASSWORD" SecureString

# ------------------------------------------------------------------ 6. RDS PostgreSQL (not publicly accessible)
step "RDS PostgreSQL"
if ! aws rds describe-db-subnet-groups --db-subnet-group-name waslah-db-subnets >/dev/null 2>&1; then
  # shellcheck disable=SC2086
  aws rds create-db-subnet-group --db-subnet-group-name waslah-db-subnets \
    --db-subnet-group-description "WASLAH database subnets" --subnet-ids $SUBNETS >/dev/null
fi
if ! aws rds describe-db-instances --db-instance-identifier waslah-db >/dev/null 2>&1; then
  PG_VERSION=$(aws rds describe-db-engine-versions --engine postgres \
    --query "DBEngineVersions[?starts_with(EngineVersion, '17.')].EngineVersion" \
    --output text | tr '\t' '\n' | sort -V | tail -1)
  echo "   creating PostgreSQL $PG_VERSION ($DB_CLASS); this takes 5-10 minutes"
  aws rds create-db-instance --db-instance-identifier waslah-db \
    --engine postgres --engine-version "$PG_VERSION" --db-instance-class "$DB_CLASS" \
    --allocated-storage 20 --storage-type gp3 --storage-encrypted \
    --db-name waslah --master-username "$(param_get DB_USERNAME)" --master-user-password "$(param_get DB_PASSWORD)" \
    --vpc-security-group-ids "$DB_SG" --db-subnet-group-name waslah-db-subnets \
    --no-publicly-accessible --no-multi-az --backup-retention-period 1 --no-deletion-protection \
    --tags "$TAGS" >/dev/null
fi

# ------------------------------------------------------------------ 7. EC2 host (Docker, managed through SSM, no SSH)
step "EC2 host ($INSTANCE_TYPE)"
INSTANCE_ID=$(aws ec2 describe-instances \
  --filters Name=tag:Name,Values=waslah-app Name=instance-state-name,Values=pending,running,stopping,stopped \
  --query 'Reservations[0].Instances[0].InstanceId' --output text)
if none "$INSTANCE_ID"; then
  AMI=$(aws ssm get-parameter --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 \
        --query Parameter.Value --output text)
  USER_DATA=$(mktemp)
  cat > "$USER_DATA" <<'EOF'
#!/bin/bash
set -eux
dnf install -y docker
systemctl enable --now docker
usermod -aG docker ec2-user
mkdir -p /usr/local/lib/docker/cli-plugins
curl -fsSL https://github.com/docker/compose/releases/download/v2.32.4/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile swap swap defaults 0 0' >> /etc/fstab
mkdir -p /opt/waslah && chown ec2-user:ec2-user /opt/waslah
EOF
  for attempt in 1 2 3 4 5 6; do
    INSTANCE_ID=$(aws ec2 run-instances --image-id "$AMI" --instance-type "$INSTANCE_TYPE" \
      --iam-instance-profile Name=waslah-ec2-profile --security-group-ids "$WEB_SG" --subnet-id "$FIRST_SUBNET" \
      --metadata-options HttpTokens=required,HttpEndpoint=enabled \
      --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=30,VolumeType=gp3,Encrypted=true}' \
      --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=waslah-app},{Key=Project,Value=waslah}]" \
      --user-data "file://$USER_DATA" --query 'Instances[0].InstanceId' --output text 2>/dev/null) && break
    echo "   instance profile not ready yet, retrying ($attempt)"; sleep 10
  done
  none "$INSTANCE_ID" && { echo "Could not start the EC2 instance"; exit 1; }
fi
aws ec2 wait instance-running --instance-ids "$INSTANCE_ID"

EIP_ALLOC=$(aws ec2 describe-addresses --filters Name=tag:Name,Values=waslah-ip --query 'Addresses[0].AllocationId' --output text)
if none "$EIP_ALLOC"; then
  EIP_ALLOC=$(aws ec2 allocate-address --domain vpc \
    --tag-specifications "ResourceType=elastic-ip,Tags=[{Key=Name,Value=waslah-ip},{Key=Project,Value=waslah}]" \
    --query AllocationId --output text)
fi
aws ec2 associate-address --instance-id "$INSTANCE_ID" --allocation-id "$EIP_ALLOC" --allow-reassociation >/dev/null
sleep 5
PUBLIC_DNS=$(aws ec2 describe-instances --instance-ids "$INSTANCE_ID" --query 'Reservations[0].Instances[0].PublicDnsName' --output text)
echo "   $INSTANCE_ID  $PUBLIC_DNS"

# ------------------------------------------------------------------ 8. IAM role for GitHub Actions (OIDC, no keys)
step "IAM role for GitHub Actions ($GITHUB_REPO, branch $GITHUB_BRANCH)"
OIDC_ARN="arn:aws:iam::$ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
if ! aws iam get-open-id-connect-provider --open-id-connect-provider-arn "$OIDC_ARN" >/dev/null 2>&1; then
  aws iam create-open-id-connect-provider --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1 >/dev/null
fi
GH_TRUST=$(jq -n --arg arn "$OIDC_ARN" --arg repo "$GITHUB_REPO" --arg br "$GITHUB_BRANCH" \
  '{Version:"2012-10-17",Statement:[{Effect:"Allow",Principal:{Federated:$arn},Action:"sts:AssumeRoleWithWebIdentity",
    Condition:{StringEquals:{"token.actions.githubusercontent.com:aud":"sts.amazonaws.com"},
               StringLike:{"token.actions.githubusercontent.com:sub":["repo:\($repo):ref:refs/heads/\($br)","repo:\($repo):environment:production"]}}}]}')
if aws iam get-role --role-name waslah-github-actions >/dev/null 2>&1; then
  aws iam update-assume-role-policy --role-name waslah-github-actions --policy-document "$GH_TRUST"
else
  aws iam create-role --role-name waslah-github-actions --assume-role-policy-document "$GH_TRUST" >/dev/null
fi
aws iam put-role-policy --role-name waslah-github-actions --policy-name waslah-ci-cd --policy-document "$(jq -n \
  --arg repos "arn:aws:ecr:$AWS_REGION:$ACCOUNT_ID:repository/waslah/*" \
  --arg bucket "arn:aws:s3:::$BUCKET/releases/*" \
  --arg instance "arn:aws:ec2:$AWS_REGION:$ACCOUNT_ID:instance/$INSTANCE_ID" \
  --arg doc "arn:aws:ssm:$AWS_REGION::document/AWS-RunShellScript" \
  '{Version:"2012-10-17",Statement:[
     {Effect:"Allow",Action:"ecr:GetAuthorizationToken",Resource:"*"},
     {Effect:"Allow",Action:["ecr:BatchCheckLayerAvailability","ecr:InitiateLayerUpload","ecr:UploadLayerPart",
       "ecr:CompleteLayerUpload","ecr:PutImage","ecr:BatchGetImage","ecr:DescribeImages"],Resource:$repos},
     {Effect:"Allow",Action:["s3:PutObject","s3:GetObject"],Resource:$bucket},
     {Effect:"Allow",Action:"ssm:SendCommand",Resource:[$instance,$doc]},
     {Effect:"Allow",Action:["ssm:GetCommandInvocation","ssm:ListCommandInvocations"],Resource:"*"}]}')"
GH_ROLE_ARN=$(aws iam get-role --role-name waslah-github-actions --query Role.Arn --output text)

# ------------------------------------------------------------------ 9. CloudFront: HTTPS in front of the host
step "CloudFront (HTTPS)"
CACHE_POLICY=$(aws cloudfront list-cache-policies --type custom \
  --query "CachePolicyList.Items[?CachePolicy.CachePolicyConfig.Name=='waslah-no-cache'].CachePolicy.Id | [0]" --output text)
if none "$CACHE_POLICY"; then
  # Nothing is cached (TTL 0); the Authorization header is passed through to the services.
  CACHE_POLICY=$(aws cloudfront create-cache-policy --cache-policy-config \
    '{"Name":"waslah-no-cache","MinTTL":0,"DefaultTTL":0,"MaxTTL":1,"ParametersInCacheKeyAndForwardedToOrigin":{"EnableAcceptEncodingGzip":true,"EnableAcceptEncodingBrotli":true,"HeadersConfig":{"HeaderBehavior":"whitelist","Headers":{"Quantity":1,"Items":["Authorization"]}},"CookiesConfig":{"CookieBehavior":"none"},"QueryStringsConfig":{"QueryStringBehavior":"all"}}}' \
    --query CachePolicy.Id --output text)
fi
DIST_ID=$(aws cloudfront list-distributions --query "DistributionList.Items[?Comment=='WASLAH'].Id | [0]" --output text)
if none "$DIST_ID"; then
  ALL_VIEWER=216adef6-5c7f-47e4-b989-5492eafa07d3   # AWS managed origin request policy "AllViewer"
  DIST_CONFIG=$(jq -n --arg origin "$PUBLIC_DNS" --arg cache "$CACHE_POLICY" --arg req "$ALL_VIEWER" --arg ref "waslah-$(date +%s)" \
    '{CallerReference:$ref,Comment:"WASLAH",Enabled:true,PriceClass:"PriceClass_100",
      Origins:{Quantity:1,Items:[{Id:"waslah-ec2",DomainName:$origin,
        CustomOriginConfig:{HTTPPort:80,HTTPSPort:443,OriginProtocolPolicy:"http-only",OriginReadTimeout:60,OriginKeepaliveTimeout:5,
          OriginSslProtocols:{Quantity:1,Items:["TLSv1.2"]}}}]},
      DefaultCacheBehavior:{TargetOriginId:"waslah-ec2",ViewerProtocolPolicy:"redirect-to-https",Compress:true,
        AllowedMethods:{Quantity:7,Items:["GET","HEAD","OPTIONS","PUT","POST","PATCH","DELETE"],
          CachedMethods:{Quantity:2,Items:["GET","HEAD"]}},
        CachePolicyId:$cache,OriginRequestPolicyId:$req}}')
  DIST_ID=$(aws cloudfront create-distribution --distribution-config "$DIST_CONFIG" --query Distribution.Id --output text)
fi
CF_DOMAIN=$(aws cloudfront get-distribution --id "$DIST_ID" --query Distribution.DomainName --output text)
param_set APP_BASE_URL "https://$CF_DOMAIN" String
echo "   https://$CF_DOMAIN (becomes reachable in about 5-10 minutes)"

# ------------------------------------------------------------------ 10. logs, alarms, budget
step "CloudWatch logs and alarms"
aws logs create-log-group --log-group-name /waslah/app --tags Project=waslah 2>/dev/null || true
aws logs put-retention-policy --log-group-name /waslah/app --retention-in-days 14
if [ -n "$ALERT_EMAIL" ]; then
  TOPIC_ARN=$(aws sns create-topic --name waslah-alerts --query TopicArn --output text)
  if [ "$(aws sns list-subscriptions-by-topic --topic-arn "$TOPIC_ARN" --query "length(Subscriptions[?Endpoint=='$ALERT_EMAIL'])" --output text)" = "0" ]; then
    aws sns subscribe --topic-arn "$TOPIC_ARN" --protocol email --notification-endpoint "$ALERT_EMAIL" >/dev/null
    echo "   check $ALERT_EMAIL and confirm the AWS subscription email"
  fi
  alarm() {  # name namespace metric dimension-name dimension-value statistic comparison threshold periods
    aws cloudwatch put-metric-alarm --alarm-name "$1" --namespace "$2" --metric-name "$3" \
      --dimensions "Name=$4,Value=$5" --statistic "$6" --period 300 --evaluation-periods "$9" \
      --comparison-operator "$7" --threshold "$8" --alarm-actions "$TOPIC_ARN" --treat-missing-data notBreaching
  }
  alarm waslah-host-cpu-high       AWS/EC2 CPUUtilization     InstanceId "$INSTANCE_ID" Average GreaterThanThreshold 80 2
  alarm waslah-host-status-failed  AWS/EC2 StatusCheckFailed  InstanceId "$INSTANCE_ID" Maximum GreaterThanOrEqualToThreshold 1 1
  alarm waslah-db-cpu-high         AWS/RDS CPUUtilization     DBInstanceIdentifier waslah-db Average GreaterThanThreshold 80 2
  alarm waslah-db-storage-low      AWS/RDS FreeStorageSpace   DBInstanceIdentifier waslah-db Minimum LessThanThreshold 2000000000 1
  aws budgets create-budget --account-id "$ACCOUNT_ID" \
    --budget "{\"BudgetName\":\"waslah-monthly\",\"BudgetLimit\":{\"Amount\":\"$BUDGET_USD\",\"Unit\":\"USD\"},\"TimeUnit\":\"MONTHLY\",\"BudgetType\":\"COST\"}" \
    --notifications-with-subscribers "[{\"Notification\":{\"NotificationType\":\"ACTUAL\",\"ComparisonOperator\":\"GREATER_THAN\",\"Threshold\":80,\"ThresholdType\":\"PERCENTAGE\"},\"Subscribers\":[{\"SubscriptionType\":\"EMAIL\",\"Address\":\"$ALERT_EMAIL\"}]}]" \
    2>/dev/null && echo "   budget alert at 80% of $BUDGET_USD USD" || echo "   budget already exists"
else
  echo "   ALERT_EMAIL not set: skipping alarms and budget (set it and run again)"
fi

# ------------------------------------------------------------------ 11. wait for the database, store its address
step "Waiting for RDS to be available"
aws rds wait db-instance-available --db-instance-identifier waslah-db
DB_HOST=$(aws rds describe-db-instances --db-instance-identifier waslah-db --query 'DBInstances[0].Endpoint.Address' --output text)
param_set DB_HOST "$DB_HOST" String
param_set DB_URL "jdbc:postgresql://$DB_HOST:5432/waslah?sslmode=require" String
echo "   $DB_HOST"

# ------------------------------------------------------------------ done
cat <<EOF

=====================================================================
 WASLAH infrastructure is ready.

 1. GitHub -> repo -> Settings -> Secrets and variables -> Actions -> Variables:
      AWS_REGION       = $AWS_REGION
      AWS_ROLE_ARN     = $GH_ROLE_ARN
      EC2_INSTANCE_ID  = $INSTANCE_ID
      DEPLOY_BUCKET    = $BUCKET
 2. GitHub -> Settings -> Environments -> New environment "production" -> Required reviewers -> you.
 3. Push to $GITHUB_BRANCH (or Actions -> CI/CD -> Run workflow). Approve the deploy.

 Site:            https://$CF_DOMAIN
 Admin login:     admin / password with:
   aws ssm get-parameter --name $PARAMS/ADMIN_PASSWORD --with-decryption --query Parameter.Value --output text
 Server shell:    EC2 -> Instances -> waslah-app -> Connect -> Session Manager
 Logs:            CloudWatch -> Log groups -> /waslah/app
 Delete it all:   bash infrastructure/aws/teardown.sh
=====================================================================
EOF
