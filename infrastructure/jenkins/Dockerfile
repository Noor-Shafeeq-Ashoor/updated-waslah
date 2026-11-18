# Jenkins for WASLAH CI/CD, with the tools the Jenkinsfile needs:
# Java 17 (Maven wrapper), Node 20 (frontend build), Docker CLI (builds images on the host's Docker),
# AWS CLI v2 (ECR, S3, SSM) and jq. Plugins and the pipeline job are configured from code (JCasC).
FROM jenkins/jenkins:lts-jdk17

USER root
RUN sed -i 's|http://|https://|g' /etc/apt/sources.list.d/debian.sources \
    && apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl gnupg jq unzip \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
       > /etc/apt/sources.list.d/docker.list \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends docker-ce-cli docker-buildx-plugin nodejs \
    && curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-$(uname -m).zip" -o /tmp/awscli.zip \
    && unzip -q /tmp/awscli.zip -d /tmp && /tmp/aws/install && rm -rf /tmp/aws /tmp/awscli.zip \
    && rm -rf /var/lib/apt/lists/*

USER jenkins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
COPY casc/jenkins.yaml /usr/share/jenkins/casc/jenkins.yaml

ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false" \
    CASC_JENKINS_CONFIG=/usr/share/jenkins/casc/jenkins.yaml
