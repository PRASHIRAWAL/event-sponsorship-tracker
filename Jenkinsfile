// Jenkins CI/CD pipeline for the Event Sponsorship Tracker.
// Checks out the code, compiles it with Maven, runs the test suite and packages the JAR.
// Tests use the H2 "test" profile, so no MySQL server is needed on the Jenkins agent.
// When DEPLOY is ticked, it then builds Docker images and deploys the app behind Nginx.
//
// One-time Jenkins setup (see docs/JENKINS_SETUP.md and docs/DEPLOYMENT.md):
//   Manage Jenkins > Tools: register a JDK named 'JDK21' and a Maven installation named 'Maven3'.
//   Manage Jenkins > Credentials: add a 'Username with password' credential with ID 'eventtracker-db'.

pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven3'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
    }

    parameters {
        string(name: 'APP_PORT', defaultValue: '8081', trim: true,
               description: 'Host port for the Spring Boot container (8040 = Jenkins, 3306 = MySQL)')
        string(name: 'NGINX_PORT', defaultValue: '80', trim: true,
               description: 'Host port for the Nginx reverse proxy')
        booleanParam(name: 'DEPLOY', defaultValue: true,
                     description: 'Build Docker images and deploy after all tests pass (untick to run CI only)')
    }

    environment {
        // Defaults apply on the first run, before Jenkins has registered the parameters.
        APP_PORT        = "${params.APP_PORT ?: '8081'}"
        NGINX_PORT      = "${params.NGINX_PORT ?: '80'}"
        DOCKER_BIN      = 'C:\\Users\\Windows\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin'
        APP_IMAGE       = 'event-sponsorship-tracker'
        DOCKERHUB_IMAGE = 'prashirawal/event-sponsorship-tracker'
        NGINX_IMAGE     = 'eventtracker-nginx'
        APP_CONTAINER   = 'eventtracker-app'
        NGINX_CONTAINER = 'eventtracker-nginx'
        DEPLOY_NETWORK  = 'eventtracker-net'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                runCommand('mvn -B -V clean compile')
            }
        }

        stage('Test') {
            steps {
                runCommand('mvn -B test')
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('E2E Tests') {
            // Selenium tests in headless Chrome (Chrome must be installed on the agent).
            steps {
                runCommand('mvn -B -Pe2e verify')
            }
            post {
                always {
                    junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: false
                    archiveArtifacts artifacts: 'target/e2e-screenshots/*.png', allowEmptyArchive: true
                }
            }
        }

        stage('Package') {
            steps {
                runCommand('mvn -B package -DskipTests')
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Docker Image') {
            when { expression { params.DEPLOY != false } }
            steps {
                withEnv(["PATH+DOCKER=${env.DOCKER_BIN}"]) {
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKERHUB_USER',
                        passwordVariable: 'DOCKERHUB_TOKEN'
                    )]) {
                        script {
                            if (isUnix()) {
                                sh '''
                                    set +x
                                    echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin
                                '''
                            } else {
                                bat '''
                                    @echo off
                                    powershell -NoProfile -NonInteractive -Command "$env:DOCKERHUB_TOKEN | docker login --username $env:DOCKERHUB_USER --password-stdin; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }"
                                '''
                            }

                            runCommand("docker build -t ${env.DOCKERHUB_IMAGE}:${env.BUILD_NUMBER} -t ${env.DOCKERHUB_IMAGE}:latest .")
                            runCommand("docker build -t ${env.DOCKERHUB_IMAGE}-nginx:${env.BUILD_NUMBER} -t ${env.DOCKERHUB_IMAGE}-nginx:latest deploy/nginx")

                            runCommand("docker push ${env.DOCKERHUB_IMAGE}:${env.BUILD_NUMBER}")
                            runCommand("docker push ${env.DOCKERHUB_IMAGE}:latest")
                            runCommand("docker push ${env.DOCKERHUB_IMAGE}-nginx:${env.BUILD_NUMBER}")
                            runCommand("docker push ${env.DOCKERHUB_IMAGE}-nginx:latest")
                            runCommand("docker logout")
                        }
                    }
                }
            }
        }


        stage('Deploy') {
            when { expression { params.DEPLOY != false } }
            steps {
                script {
                    validatePort('APP_PORT', env.APP_PORT)
                    validatePort('NGINX_PORT', env.NGINX_PORT)
                    if (env.APP_PORT == env.NGINX_PORT) {
                        error 'APP_PORT and NGINX_PORT must be different'
                    }
                }
                // The existing eventtracker-mysql container is reached through its published host port;
                // this pipeline never runs any command against the MySQL container.
                withEnv(["PATH+DOCKER=${env.DOCKER_BIN}",
                         'DB_URL=jdbc:mysql://host.docker.internal:3306/eventsponsorship_db']) {
                    withCredentials([
                        usernamePassword(credentialsId: 'eventtracker-db',
                            usernameVariable: 'DB_USERNAME', passwordVariable: 'DB_PASSWORD'),
                        usernamePassword(credentialsId: 'dockerhub-credentials',
                            usernameVariable: 'DOCKERHUB_USER', passwordVariable: 'DOCKERHUB_TOKEN')
                    ]) {
                        script {
                            if (runStatus("docker network inspect ${env.DEPLOY_NETWORK}") != 0) {
                                runCommand("docker network create ${env.DEPLOY_NETWORK}")
                            }

                            // Log in to Docker Hub without printing the token.
                            if (isUnix()) {
                                sh '''
                                    set +x
                                    echo "$DOCKERHUB_TOKEN" | docker login --username "$DOCKERHUB_USER" --password-stdin
                                '''
                            } else {
                                bat '''
                                    @echo off
                                    powershell -NoProfile -NonInteractive -Command "$env:DOCKERHUB_TOKEN | docker login --username $env:DOCKERHUB_USER --password-stdin; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }"
                                '''
                            }

                            runCommand("docker pull ${env.DOCKERHUB_IMAGE}:${env.BUILD_NUMBER}")
                            runCommand("docker pull ${env.DOCKERHUB_IMAGE}-nginx:${env.BUILD_NUMBER}")

                            runCommand("docker logout")

                            // Replace only this project's app and proxy containers.
                            runStatus("docker rm -f ${env.NGINX_CONTAINER}")
                            runStatus("docker rm -f ${env.APP_CONTAINER}")

                            // Pass database credentials through environment variables.
                            runCommand("docker run -d --name ${env.APP_CONTAINER} --network ${env.DEPLOY_NETWORK} " +
                                    "--restart unless-stopped -p ${env.APP_PORT}:8080 " +
                                    "-e DB_URL -e DB_USERNAME -e DB_PASSWORD ${env.DOCKERHUB_IMAGE}:${env.BUILD_NUMBER}")

                            runCommand("docker run -d --name ${env.NGINX_CONTAINER} --network ${env.DEPLOY_NETWORK} " +
                                    "--restart unless-stopped -p ${env.NGINX_PORT}:80 ${env.DOCKERHUB_IMAGE}-nginx:${env.BUILD_NUMBER}")
                        }
                    }
                }
            }
        }

        stage('Verify Deployment') {
            when { expression { params.DEPLOY != false } }
            steps {
                script {
                    // Retries for up to ~90 s while Spring Boot starts and connects to MySQL.
                    def retry = '-fsS --retry 45 --retry-delay 2 --retry-all-errors'
                    runCommand("curl ${retry} -o health-app.json http://localhost:${env.APP_PORT}/actuator/health")
                    runCommand("curl ${retry} -o health-nginx.json http://localhost:${env.NGINX_PORT}/actuator/health")
                    runCommand("curl -fsS -o dashboard-nginx.html http://localhost:${env.NGINX_PORT}/dashboard")
                    // Smoke-test the other main pages and the reporting API through Nginx (curl -f fails on 4xx/5xx)
                    ['events', 'sponsors', 'reports', 'alerts', 'activity', 'api/reports/events'].each { path ->
                        runCommand("curl -fsS -o smoke-page.out http://localhost:${env.NGINX_PORT}/${path}")
                    }
                    ['health-app.json', 'health-nginx.json'].each { file ->
                        def body = readFile(file).trim()
                        echo "${file}: ${body}"
                        if (!body.startsWith('{"status":"UP"')) {
                            error "${file} does not report status UP"
                        }
                    }
                    if (!readFile('dashboard-nginx.html').contains('Dashboard')) {
                        error 'Dashboard page was not served through Nginx'
                    }
                    echo "Deployed build ${env.BUILD_NUMBER}: app http://localhost:${env.APP_PORT}, Nginx http://localhost:${env.NGINX_PORT}"
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'health-*.json', allowEmptyArchive: true
                }
                failure {
                    withEnv(["PATH+DOCKER=${env.DOCKER_BIN}"]) {
                        runStatus("docker logs --tail 100 ${env.APP_CONTAINER}")
                        runStatus("docker logs --tail 50 ${env.NGINX_CONTAINER}")
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully.'
        }
        failure {
            echo 'Pipeline failed. Check the stage logs and test report above.'
        }
        always {
            // Built-in step; no Workspace Cleanup plugin required.
            deleteDir()
        }
    }
}

// Runs a shell command on Linux/macOS agents and a batch command on Windows agents.
def runCommand(String command) {
    if (isUnix()) {
        sh command
    } else {
        bat command
    }
}

// Like runCommand, but returns the exit code instead of failing the build.
def runStatus(String command) {
    return isUnix() ? sh(script: command, returnStatus: true) : bat(script: command, returnStatus: true)
}

// Port parameters are inserted into shell commands, so only plain port numbers are accepted.
def validatePort(String name, String value) {
    if (!(value ==~ /\d{1,5}/) || value.toInteger() < 1 || value.toInteger() > 65535) {
        error "${name} must be a port number between 1 and 65535, got '${value}'"
    }
    if (value in ['8040', '3306']) {
        error "${name} ${value} is reserved (8040 = Jenkins, 3306 = MySQL)"
    }
}
