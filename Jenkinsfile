// WASLAH CI/CD pipeline (Jenkins).
//
// Every pushed branch:  build + test the 4 services -> build the frontend -> build the 5 Docker images.
// master / main only:   push images to Amazon ECR (1.0.<build>) -> upload release files to S3
//                       -> manual approval -> deploy on EC2 through AWS Systems Manager (no SSH).
// Rollback:             "Build with Parameters" on master, ROLLBACK_VERSION = e.g. 1.0.5
//                       (skips building and redeploys that existing version).
// Any failing step stops the pipeline.

def SERVICES = ['identity_service', 'career_service', 'workspace_service', 'notification_service']
def IMAGES = [
    'identity-service'    : 'services/identity_service',
    'career-service'      : 'services/career_service',
    'workspace-service'   : 'services/workspace_service',
    'notification-service': 'services/notification_service',
    'frontend'            : 'frontend',
]

// Release stages run only once AWS is set up (EC2_INSTANCE_ID and DEPLOY_BUCKET set in Jenkins).
def awsReady() {
    return env.EC2_INSTANCE_ID?.trim() && env.DEPLOY_BUCKET?.trim()
}

pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 90, unit: 'MINUTES')
    }

    parameters {
        string(name: 'ROLLBACK_VERSION', defaultValue: '',
               description: 'Leave empty to build and deploy this commit. Enter an existing version (e.g. 1.0.5) to redeploy it.')
    }

    environment {
        VERSION = "1.0.${env.BUILD_NUMBER}"
        DEPLOY_VERSION = "${params.ROLLBACK_VERSION ?: '1.0.' + env.BUILD_NUMBER}"
    }

    stages {
        stage('Build & test services') {
            when { expression { !params.ROLLBACK_VERSION } }
            steps {
                script {
                    for (service in SERVICES) {
                        dir("services/${service}") {
                            // "test" profile: in-memory H2, no PostgreSQL, Kafka or real secrets needed.
                            sh 'SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp verify'
                        }
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'services/*/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build frontend') {
            when { expression { !params.ROLLBACK_VERSION } }
            steps {
                dir('frontend') {
                    sh 'npm ci && npm run build'
                }
            }
        }

        stage('Build Docker images') {
            when { expression { !params.ROLLBACK_VERSION } }
            steps {
                script {
                    for (name in IMAGES.keySet()) {
                        sh "docker build --pull -t waslah/${name}:${env.VERSION} ${IMAGES[name]}"
                    }
                }
            }
        }

        stage('Push images to Amazon ECR') {
            when {
                allOf {
                    anyOf { branch 'master'; branch 'main' }
                    expression { !params.ROLLBACK_VERSION }
                    expression { awsReady() }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-jenkins',
                        usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        set -eu
                        ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
                        REGISTRY="$ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
                        aws ecr get-login-password | docker login --username AWS --password-stdin "$REGISTRY"
                        SHA=$(git rev-parse --short HEAD)
                        for name in identity-service career-service workspace-service notification-service frontend; do
                          for tag in "$VERSION" "sha-$SHA"; do
                            docker tag "waslah/$name:$VERSION" "$REGISTRY/waslah/$name:$tag"
                            docker push "$REGISTRY/waslah/$name:$tag"
                          done
                        done
                    '''
                }
            }
        }

        stage('Upload release files to S3') {
            when {
                allOf {
                    anyOf { branch 'master'; branch 'main' }
                    expression { !params.ROLLBACK_VERSION }
                    expression { awsReady() }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-jenkins',
                        usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        set -eu
                        : "${DEPLOY_BUCKET:?DEPLOY_BUCKET is not set in Jenkins}"
                        RELEASE="s3://$DEPLOY_BUCKET/releases/$VERSION"
                        aws s3 cp infrastructure/docker/compose.prod.yaml "$RELEASE/compose.prod.yaml"
                        aws s3 cp infrastructure/db/01-schema.pg17.sql   "$RELEASE/01-schema.pg17.sql"
                        aws s3 cp infrastructure/aws/scripts/deploy.sh   "$RELEASE/deploy.sh"
                    '''
                }
            }
        }

        stage('Approve deployment') {
            when {
                allOf {
                    anyOf { branch 'master'; branch 'main' }
                    expression { awsReady() }
                }
            }
            steps {
                script {
                    if (!(env.DEPLOY_VERSION ==~ /[0-9A-Za-z._-]+/)) {
                        error "Invalid version: ${env.DEPLOY_VERSION}"
                    }
                }
                timeout(time: 30, unit: 'MINUTES') {
                    input message: "Deploy WASLAH ${env.DEPLOY_VERSION} to production?", ok: 'Deploy'
                }
            }
        }

        stage('Deploy to AWS (EC2 via SSM)') {
            when {
                allOf {
                    anyOf { branch 'master'; branch 'main' }
                    expression { awsReady() }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-jenkins',
                        usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        set -eu
                        : "${EC2_INSTANCE_ID:?EC2_INSTANCE_ID is not set in Jenkins}"
                        echo "Deploying WASLAH $DEPLOY_VERSION to $EC2_INSTANCE_ID"

                        PARAMS=$(jq -nc --arg b "$DEPLOY_BUCKET" --arg v "$DEPLOY_VERSION" '{commands: [
                          "set -e",
                          "mkdir -p /opt/waslah/scripts",
                          "chown -R ec2-user:ec2-user /opt/waslah",
                          ("aws s3 cp s3://" + $b + "/releases/" + $v + "/deploy.sh /opt/waslah/scripts/deploy.sh"),
                          "chown ec2-user:ec2-user /opt/waslah/scripts/deploy.sh",
                          "chmod 700 /opt/waslah/scripts/deploy.sh",
                          ("sudo -iu ec2-user /opt/waslah/scripts/deploy.sh " + $v)
                        ], executionTimeout: ["1800"]}')

                        CMD_ID=$(aws ssm send-command --instance-ids "$EC2_INSTANCE_ID" \
                          --document-name AWS-RunShellScript --comment "WASLAH deploy $DEPLOY_VERSION" \
                          --parameters "$PARAMS" --query Command.CommandId --output text)

                        STATUS=Pending
                        for _ in $(seq 1 120); do   # up to 20 minutes
                          STATUS=$(aws ssm get-command-invocation --command-id "$CMD_ID" --instance-id "$EC2_INSTANCE_ID" \
                                   --query Status --output text 2>/dev/null || echo Pending)
                          case "$STATUS" in Pending|InProgress|Delayed) sleep 10 ;; *) break ;; esac
                        done

                        aws ssm get-command-invocation --command-id "$CMD_ID" --instance-id "$EC2_INSTANCE_ID" \
                          --query StandardOutputContent --output text
                        echo "Deployment status: $STATUS"
                        if [ "$STATUS" != "Success" ]; then
                          aws ssm get-command-invocation --command-id "$CMD_ID" --instance-id "$EC2_INSTANCE_ID" \
                            --query StandardErrorContent --output text
                          exit 1
                        fi
                    '''
                }
            }
        }
    }

    post {
        success { echo "WASLAH pipeline passed (version ${env.DEPLOY_VERSION})" }
        failure { echo 'WASLAH pipeline FAILED - see the stage marked red' }
        always  { sh 'docker image prune -f > /dev/null || true' }
    }
}
