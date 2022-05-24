pipeline {
    options {
        skipStagesAfterUnstable()
    }
    agent {
        label 'all'
    }
    environment {
        gitCredentialId = 'DeadlyChambers' //defined for sshkey
        gitUrl = 'git@bitbucket.org:DeadlyChambers85/practice.git'
        //'https://DeadlyChambers85@bitbucket.org/DeadlyChambers85/practice.git'
        deployBranch = 'main'
        APP_VER = "6.0.${BUILD_NUMBER}"
        APP_LABEL = ""
    }
    stages {
        
        stage('Preparation') { // for display purposes
            steps {
                
                sshagent (credentials: ['bb-ssh']) {
                    
                    sh("git clone ${env.gitUrl}")
                    script{
                        APP_VER = sh(returnStdout: true, script:'git tag | sort -V | tail -1 | cut -d "v" -f2').trim()
                    }
                }
                script{
                    env.APP_LABEL="system${APP_VER}-6.0.${env.BUILD_NUMBER}"
                }
                sh 'ls -la'
                echo 'here do a shallow git to keep size down - ${env.APP_LABEL}'
                echo 'if you can cache the repo that could speed up build time'
            }
        }
        stage('Build') {
            steps {
                sh '''
                dotnet build api/system/system.csproj -c Release -r \
                linux-x64 -p:Version="6.0.${env.BUILD_NUMBER}"
                dotnet publish api/system/system.csproj -c Release -r \
                linux-x64 -o ../out --self-contained -p:Version="6.0.${env.BUILD_NUMBER}"
                zip -r ${env.APP_LABEL}.zip . -i out
                echo "build, run tests"
                '''
                //s3Upload(file:"system${env.APP_VER}.zip",
                //bucket: "soinshane-terraform-state")
            }
        }
        stage('Create Release') {
                steps {
                    sh 'which aws'
                    echo 'create the zip, and push to beanstalk'
                    echo "s3_file=s3://bucket/app_version/${env.APP_LABEL}.zip"
                    build job: 'dotnet-deploy-pipeline', parameters: [string(name: 'Label', value: "${env.APP_LABEL}"), string(name: 'ENV', value: "")]
                }
        }

        stage('OutputResults') {
            steps {
                sh 'which curl'
                echo 'push the file to s3, kick off the beanstalk create app version'
                echo 'Then we should have a prepared deploy for all environments'
                echo "archiveArtifacts ${env.APP_LABEL}"
            }
        }
    }
    post {
        always {
            echo '========always========'
        }
        success {
            //cleanWs()
            echo '========pipeline executed successfully ========'
        }
        failure {
            echo '========pipeline execution failed========'
        }
    }
}
