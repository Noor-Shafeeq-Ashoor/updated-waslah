#!/usr/bin/env bash
# Deletes everything setup.sh created, to stop all charges. Run in AWS CloudShell:
#   bash infrastructure/aws/teardown.sh
# The database is deleted WITHOUT a final snapshot. Export it first if you need the data.
set -uo pipefail   # no -e: keep going if one item is already gone

export AWS_REGION="${AWS_REGION:-us-east-1}"
export AWS_DEFAULT_REGION="$AWS_REGION"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
BUCKET="waslah-deploy-$ACCOUNT_ID-$AWS_REGION"
step() { echo; echo "==> $*"; }
none() { [ -z "$1" ] || [ "$1" = "None" ] || [ "$1" = "null" ]; }

read -r -p "Delete ALL WASLAH resources in $AWS_REGION (account $ACCOUNT_ID)? Type 'delete': " answer
[ "$answer" = "delete" ] || { echo "Cancelled."; exit 0; }

step "CloudFront (disable, wait, delete: about 5-15 minutes)"
DIST_ID=$(aws cloudfront list-distributions --query "DistributionList.Items[?Comment=='WASLAH'].Id | [0]" --output text)
if ! none "$DIST_ID"; then
  ETAG=$(aws cloudfront get-distribution-config --id "$DIST_ID" --query ETag --output text)
  aws cloudfront get-distribution-config --id "$DIST_ID" --query DistributionConfig --output json \
    | jq '.Enabled=false' > /tmp/waslah-dist.json
  aws cloudfront update-distribution --id "$DIST_ID" --if-match "$ETAG" --distribution-config file:///tmp/waslah-dist.json >/dev/null
  aws cloudfront wait distribution-deployed --id "$DIST_ID"
  ETAG=$(aws cloudfront get-distribution-config --id "$DIST_ID" --query ETag --output text)
  aws cloudfront delete-distribution --id "$DIST_ID" --if-match "$ETAG"
fi
CACHE_POLICY=$(aws cloudfront list-cache-policies --type custom \
  --query "CachePolicyList.Items[?CachePolicy.CachePolicyConfig.Name=='waslah-no-cache'].CachePolicy.Id | [0]" --output text)
if ! none "$CACHE_POLICY"; then
  ETAG=$(aws cloudfront get-cache-policy --id "$CACHE_POLICY" --query ETag --output text)
  aws cloudfront delete-cache-policy --id "$CACHE_POLICY" --if-match "$ETAG"
fi

step "EC2 host and Elastic IP"
INSTANCE_ID=$(aws ec2 describe-instances --filters Name=tag:Name,Values=waslah-app \
  Name=instance-state-name,Values=pending,running,stopping,stopped --query 'Reservations[0].Instances[0].InstanceId' --output text)
if ! none "$INSTANCE_ID"; then
  aws ec2 terminate-instances --instance-ids "$INSTANCE_ID" >/dev/null
  aws ec2 wait instance-terminated --instance-ids "$INSTANCE_ID"
fi
EIP_ALLOC=$(aws ec2 describe-addresses --filters Name=tag:Name,Values=waslah-ip --query 'Addresses[0].AllocationId' --output text)
none "$EIP_ALLOC" || aws ec2 release-address --allocation-id "$EIP_ALLOC"

step "RDS (no final snapshot)"
if aws rds describe-db-instances --db-instance-identifier waslah-db >/dev/null 2>&1; then
  aws rds delete-db-instance --db-instance-identifier waslah-db --skip-final-snapshot --delete-automated-backups >/dev/null
  aws rds wait db-instance-deleted --db-instance-identifier waslah-db
fi
aws rds delete-db-subnet-group --db-subnet-group-name waslah-db-subnets 2>/dev/null

step "Security groups"
VPC_ID=$(aws ec2 describe-vpcs --filters Name=is-default,Values=true --query 'Vpcs[0].VpcId' --output text)
for name in waslah-db waslah-web; do
  SG=$(aws ec2 describe-security-groups --filters Name=group-name,Values=$name Name=vpc-id,Values="$VPC_ID" \
       --query 'SecurityGroups[0].GroupId' --output text)
  none "$SG" || aws ec2 delete-security-group --group-id "$SG"
done

step "ECR repositories (with all images)"
for repo in identity-service career-service workspace-service notification-service frontend; do
  aws ecr delete-repository --repository-name "waslah/$repo" --force >/dev/null 2>&1
done

step "S3 release bucket"
aws s3 rb "s3://$BUCKET" --force 2>/dev/null

step "IAM roles"
aws iam remove-role-from-instance-profile --instance-profile-name waslah-ec2-profile --role-name waslah-ec2-role 2>/dev/null
aws iam delete-instance-profile --instance-profile-name waslah-ec2-profile 2>/dev/null
for policy in AmazonSSMManagedInstanceCore AmazonEC2ContainerRegistryReadOnly CloudWatchAgentServerPolicy; do
  aws iam detach-role-policy --role-name waslah-ec2-role --policy-arn "arn:aws:iam::aws:policy/$policy" 2>/dev/null
done
aws iam delete-role-policy --role-name waslah-ec2-role --policy-name waslah-app 2>/dev/null
aws iam delete-role --role-name waslah-ec2-role 2>/dev/null
aws iam delete-role-policy --role-name waslah-github-actions --policy-name waslah-ci-cd 2>/dev/null
aws iam delete-role --role-name waslah-github-actions 2>/dev/null
echo "   (the GitHub OIDC provider is kept; it is free and may be used by other projects)"

step "Parameters, logs, alarms, budget"
NAMES=$(aws ssm get-parameters-by-path --path /waslah/prod --query 'Parameters[].Name' --output text)
# shellcheck disable=SC2086
none "$NAMES" || aws ssm delete-parameters --names $NAMES >/dev/null
aws logs delete-log-group --log-group-name /waslah/app 2>/dev/null
aws cloudwatch delete-alarms --alarm-names waslah-host-cpu-high waslah-host-status-failed waslah-db-cpu-high waslah-db-storage-low 2>/dev/null
TOPIC_ARN="arn:aws:sns:$AWS_REGION:$ACCOUNT_ID:waslah-alerts"
aws sns delete-topic --topic-arn "$TOPIC_ARN" 2>/dev/null
aws budgets delete-budget --account-id "$ACCOUNT_ID" --budget-name waslah-monthly 2>/dev/null

echo; echo "Done. WASLAH resources in $AWS_REGION are deleted."
