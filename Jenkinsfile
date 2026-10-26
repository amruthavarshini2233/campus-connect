pipeline {
    agent any

    triggers {
        githubPush()
        pollSCM('* * * * *')
    }

    environment {
        JAVA_HOME    = 'C:\\Program Files\\Java\\jdk-21.0.11'
        PATH         = "C:\\Program Files\\Java\\jdk-21.0.11\\bin;C:\\Users\\amrutha\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin;${env.PATH}"
        APP_NAME     = 'campusconnect'
        DOCKER_IMAGE = 'campusconnect:latest'
    }

    stages {
        stage('Checkout SCM') {
            steps {
                echo 'Checking out source repository...'
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                echo 'Compiling Java classes with Maven Wrapper...'
                script {
                    if (isUnix()) {
                        sh 'chmod +x mvnw && ./mvnw clean compile'
                    } else {
                        bat '.\\mvnw.cmd clean compile'
                    }
                }
            }
        }

        stage('Run Tests') {
            steps {
                echo 'Running unit and web controller tests...'
                script {
                    if (isUnix()) {
                        sh './mvnw test'
                    } else {
                        bat '.\\mvnw.cmd test'
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package JAR') {
            steps {
                echo 'Packaging application executable JAR...'
                script {
                    if (isUnix()) {
                        sh './mvnw package -DskipTests'
                    } else {
                        bat '.\\mvnw.cmd package -DskipTests'
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo "Building Docker container image [${DOCKER_IMAGE}]..."
                script {
                    if (isUnix()) {
                        sh "docker build -t ${DOCKER_IMAGE} ."
                    } else {
                        bat "docker build -t ${DOCKER_IMAGE} ."
                    }
                }
            }
        }
    }

    post {
        success {
            echo "CI/CD Pipeline finished successfully! Image '${DOCKER_IMAGE}' built."
        }
        failure {
            echo "Pipeline build failed. Please inspect build console output."
        }
    }
}
