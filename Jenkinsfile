// Jenkins CI pipeline for the Event Sponsorship Tracker.
// Checks out the code, compiles it with Maven, runs the test suite and packages the JAR.
// Tests use the H2 "test" profile, so no MySQL server is needed on the Jenkins agent.
//
// One-time Jenkins setup (see docs/JENKINS_SETUP.md):
//   Manage Jenkins > Tools: register a JDK named 'JDK21' and a Maven installation named 'Maven3'.

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
    }

    post {
        success {
            echo 'Build, tests and packaging completed successfully.'
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
