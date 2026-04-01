pipeline {
    agent any

    tools {
        maven 'Maven 3.8.x' // Assuming you configure this in Jenkins global tools
        jdk 'Java 17'
    }

    environment {
        SONAR_URL = 'http://localhost:9000'
        SONAR_CREDENTIALS_ID = 'sonar-token'
        DOCKER_REGISTRY = 'registry.hub.docker.com'
        DOCKER_CREDENTIALS_ID = 'docker-hub-credentials'
        APP_VERSION = "1.0.0-b${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Unit Test') {
            steps {
                echo "Building all microservices..."
                // Assuming parent POM exists, otherwise loop through directories
                sh 'mvn clean package -DskipTests=false'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQubeServer') { // Configured in Jenkins
                    sh 'mvn sonar:sonar'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    // Requires SonarQube Scanner plugin webhook configured
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    def services = ['ServiceRegistry', 'config_server_smart_sure', 'ApiGatewaySmartSure', 'AuthService', 'PolicyService', 'claimService', 'paymentService', 'adminService']
                    for (service in services) {
                        sh "docker build -t mydockerhubusername/${service.toLowerCase()}:${APP_VERSION} ./${service}"
                    }
                }
            }
        }

        stage('Push Docker Images') {
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIALS_ID, passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                    sh 'echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin'
                    script {
                        def services = ['ServiceRegistry', 'config_server_smart_sure', 'ApiGatewaySmartSure', 'AuthService', 'PolicyService', 'claimService', 'paymentService', 'adminService']
                        for (service in services) {
                            sh "docker push mydockerhubusername/${service.toLowerCase()}:${APP_VERSION}"
                            sh "docker tag mydockerhubusername/${service.toLowerCase()}:${APP_VERSION} mydockerhubusername/${service.toLowerCase()}:latest"
                            sh "docker push mydockerhubusername/${service.toLowerCase()}:latest"
                        }
                    }
                }
            }
        }

        stage('Deploy to Staging (Docker Compose)') {
            steps {
                echo "Deploying to Staging Environment via Docker Compose..."
                sh 'docker-compose -f docker-compose.services.yml up -d'
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished. Archiving test results...'
            junit '**/target/surefire-reports/TEST-*.xml'
        }
        success {
            echo 'Pipeline succeeded!'
            // slackSend channel: '#deployments', message: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})"
        }
        failure {
            echo 'Pipeline failed!'
            // slackSend color: 'danger', channel: '#deployments', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})"
        }
    }
}
