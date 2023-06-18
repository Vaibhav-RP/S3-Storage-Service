pipeline {
    agent any
    tools {
        gradle 'gradle7.6.1'
    }
    stages {
        stage('Build Gradle') {
            steps {
                checkout scmGit(branches: [[name: '*/master']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/Vaibhav-RP/S3-Storage-Service']])
                sh 'gradle clean build'
            }
        }
        stage('Build Docker image') {
            steps {
                script {
                    sh ' docker build -t vaibhavrp/awsstorageimage .'
                }
            }
        }
        stage('Push image to Hub') {
            steps {
                withCredentials([string(credentialsId: 'dockerhub', variable: 'dockerhubpwd')]) {
                    sh "docker login -u vaibhavrp -p ${dockerhubpwd}"
                }
                sh 'docker push vaibhavrp/awsstorageimage'
            }
        }
    }
    
}