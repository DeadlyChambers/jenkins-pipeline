pipeline {
    agent {
        label 'all'
    }
    environment {
        gitCredentialId = 'jenkins-soinshane-global' //defined in credentials area
        gitUrl = 'https://DeadlyChambers85@bitbucket.org/DeadlyChambers85/practice.git'
        deployBranch = 'main'
    }
    stages {
        stage('Preparation') { // for display purposes
            // Get some code from a GitHub repository
            steps {
                withCredentials([usernamePassword(credentialsId: gitCredentialId, p
                asswordVariable: 'password', usernameVariable: 'username')]) {
                    git(
                    url: gitUrl,
                    credentialsId: gitCredentialId,
                    branch: deployBranch
                    )
                    echo 'here do a shallow git to keep size down'
                    echo 'if you can cache the repo that could speed up build time'
                }
            }
        }
        stage('Build') {
            steps {
                sh 'ls -la'
                echo 'dotnet --info'
                echo 'build, run tests'
                sh 'which dotnet'
            }
        }
        stage('Deploy') {
            steps {
                dotnet
                sh 'which aws'
                echo 'create the zip, and push to beanstalk'
                echo "s3_file=s3://bucket/app_version/v${env.BUILD_NUMBER}.zip"
            }
        }
        stage('OutputResults') {
            steps {
                sh 'which curl'
                echo 'push the file to s3, kick off the beanstalk create app version'
                echo 'Then we should have a prepared deploy for all environments'
                echo "archiveArtifacts ${env.BUILD_NUMBER}"
                
            }
        }
        stage('Notifiy Release') {
            steps {
                echo 'create release'
                echo "Running ${env.BUILD_ID} on ${env.JENKINS_URL}"
            }
        }
    }
}
