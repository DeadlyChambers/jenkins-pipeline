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
    }
    stages {
        stage('Preparation') { // for display purposes
            steps {
                sshagent(credentials: [gitCredentialId]) {
                    git branch: deployBranch, url: gitUrl
                }
                sh 'ls -la'
                echo 'here do a shallow git to keep size down'
                echo 'if you can cache the repo that could speed up build time'
            }
        }
        stage('Build') {
            steps {
                sh '''
                dotnet build api/system/system.csproj -c Release -r \
                linux-x64 -p:Version="$APP_VER"
                dotnet publish api/system/system.csproj -c Release -r \
                linux-x64 -o ../out --self-contained -p:Version="$APP_VER"
                '''
                // dotnetBuild(project:'api/system/system.csproj', configuration:'Release',
                //     runtime:'linux-x64', propertiesString: "Version=$APP_VER")
                // dotnetPublish(project:'api/system/system.csproj',
                //     configuration:'Release', runtime:'linux-x64', selfContained:true,
                //     outputDirectory: '../out', propertiesString: "Version=$APP_VER")
                sh "zip -r system${APP_VER}.zip . -i out"
                echo 'build, run tests'
                s3Upload(file:"system${APP_VER}.zip",
                bucket: "soinshane-terraform-state")
            }
        }
        stage('Create Release') {
                steps {
                    sh 'which aws'
                    echo 'create the zip, and push to beanstalk'
                    echo "s3_file=s3://bucket/app_version/${env.BUILD_NUMBER}.zip"
                    build job: 'create-release', parameters: [string(name: 'Label', value: "${env.BUILD_NUMBER}")]
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
    }
    post {
        always {
            echo '========always========'
        }
        success {
            cleanWs()
            echo '========pipeline executed successfully ========'
        }
        failure {
            echo '========pipeline execution failed========'
        }
    }
}
