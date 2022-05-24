
pipeline {
    agent none
    parameters{
        extendedChoice(
            name: 'ENV',
            defaultValue: "none",
            multiSelectDelimiter: ',',
            type: 'PT_CHECKBOX',
            value: 'none,qa,stage,live'
        )
    }
    options {
        skipDefaultCheckout()
        disableConcurrentBuilds()
    }
    stages {
        stage('Init') {
            when { triggeredBy 'BuildUpstreamCause' }
            steps {
                script {
                    def upstream = currentBuild.rawBuild.getCause(hudson.model.Cause$UpstreamCause)
                    currentBuild.displayName = string(upstream.getShortDescription()+'.'+upstream.getUpstreamBuild())
                    currentBuild.description = upstream.getUpstreamRun().getDisplayName()
                }
            }
        }
        stage('Deploy QA') {
            when {
                expression { params.ENV == 'qa' }
            }
            steps{
                echo "Do Build for ${params.ENV} - ${BROWSER}"
            }
        }
        stage('Deploy Stage') {
            when {
                expression { params.ENV == 'stage' }
            }
            steps {
                echo "Do Test for ${PLATFORM} - ${BROWSER}"
            }
        }
        stage('Deploy Live') {
            when {
                expression { params.ENV == 'live'}
            }
            steps {
                echo "Do Test for ${PLATFORM} - ${BROWSER}"
            }
        }
    }
    post {
        always {
            echo "=====${currentBuild.displayName}=====${currentBuild.description}======"
        }
    }
}

