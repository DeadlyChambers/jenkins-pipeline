
pipeline {
    agent none
    options {
        skipDefaultCheckout()
        disableConcurrentBuilds()
    }
    parameters {
        choice(name: 'ENV', default: '', choices: ['', 'qa', 'stage', 'prod'], description: 'Run for environment')
    }
    stages {
        stage('Init') {
            when { triggeredBy 'BuildUpstreamCause' }
        }
        stage('Deploy') {
            when {
                expression { params.ENV == env.deployENV }
            }
            matrix {
                axes {
                    axis {
                        name 'deployENV'
                        values 'qa', 'stage', 'prod'
                    }
                }

                stages {
                    when {
                        ENV: deployEnv
                    }
                    stage('DeployS3') {
                        steps {
                            echo "Do Build for ${PLATFORM} - ${BROWSER}"
                        }
                    }
                    stage('Test') {
                        steps {
                            echo "Do Test for ${PLATFORM} - ${BROWSER}"
                        }
                    }
                }
            }
        }
    }
}
