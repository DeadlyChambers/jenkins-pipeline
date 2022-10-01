/* groovylint-disable NglParseError */
@StepAlias(["source_build", "unit_test", "prepare_tools", "deploy_dotnet"])
void call() { //(Map args = [:],body){

    // Specific Step Variables
    String stepName = ""
    String appName = config?.appName ?: "threes"
    String masterBranch = config?.master_branch ?: "main"
 String outDir = config?.source_build?.outDir ?: "publish"
    String dateFormat = config?.dateFormat ?: "'%Y-%m-%d %H:%M'"
     switch (stepContext.name) {
        case "prepare_tools":
            stepName = "DotNet Prepare"
            break
        case "source_build":
            stepName = "DotNet Build"
            break
        case "unit_test":
            stepName = "DotNet Unit Test"
            break
        case "deploy_dotnet":
            stepName = "DotNet Deploy"
            break
        default:
            error("step name must be \"source_build\", \"prepare_tools\", \"deploy_application\", \"troubleshoot_pipeline\" or \"unit_test\" got \"${stepContext.name}\"")
     }
    stage(stepName) {
        try {
            if (stepName == "DotNet Prepare") {
                    script {
                        //TODO: Remove these bitbucket status calls, it should be handled by webhooks
                        bitbucketStatusNotify(buildState: 'INPROGRESS') 
                        echo "${stepName}"
                        if (env.runStart == null) {
                            env.runStart = sh(returnStdout:true, script: "date +${dateFormat}").trim()
                        }
                        env.appEnviron = "QA"
                        if (env.TAG_NAME) {
                            echo "Prepare for TAG:${env.TAG_NAME}"
                            //Manual process for deploying
                            env.appEnviron = input(id: 'EnvChoice', message: 'Deploy to which environment?',
                           // parameters: [choice(choices: ['QA', 'Staging', 'Live'], description: 'Environment to deploy?', name: 'configChoice')])
                           // env.GIT_CURRENT_CMT_URL = "Commit : https://${gitUrl}/${companyName}/${repoName}/commits/tag/${env.TAG_NAME}\n"
                        } else {
                            // def gitBranch = env.BRANCH_NAME.startsWith("PR-")
                            //     ? env.CHANGE_BRANCH
                            //     : env.BRANCH_NAME
                            //git(url: "${gitUrl}:${companyName}/${repoName}.git", credentialsId: "${jenkinsSshKey}", branch: "${gitBranch}")
                            //env.GIT_CURRENT_CMT_URL = "Commit : https://${gitUrl}/${companyName}/${repoName}/commits/${env.GIT_COMMIT}\n"
                        }
                        env.buildDesc = sh(returnStdout: true, script: "echo \"${appName}\n Branch : ${env.BRANCH_NAME} ${env.BUILD_DISPLAY_NAME}\n Build : ${env.BUILD_URL}\n ${env.GIT_CURRENT_CMT_URL}\"").trim()
                    }
                    sh(script:"""#!/bin/bash
                    set -e +o pipefail
                   # export DOTNET_ROOT="/usr/local/dotnet"
                   # export TOOLS="/home/ubuntu/.tools"
                   # export PATH="\$DOTNET_ROOT:\$DOTNET_ROOT/tools:\$TOOLS:\$PATH"
                   # cp -r \$TOOLS/.config ./
                   # cp -r \$TOOLS/* ./
                    """)
            }
            else if (stepName == "DotNet Unit Test") {
                //Execute dotnet tests and output to coverage directory
                if (env.BRANCH_NAME != "${masterBranch}" && env.TAG_NAME == null) {
                        echo "${stepName}"
                        echo "Config:${env.appEnviron}"
                        //dotnet test p:CollectCoverage=true /p:CoverletOutputFormat=cobertura
                        //or --collect:"XPlat Code Coverage"
                    if(true) {
                        sh(script:"""#!/bin/bash
                                set -e +o pipefail
                                _tmp=\$(pwd)
                                dotnet build
                               
                                cd \$_tmp
                                """)
                        //dotcover vsTestCaseFilter: '*tests*'
                        //export CODACY_PROJECT_TOKEN=${codeacyProjectToken}
                                //./get.sh report -r ${dotnetDir}/${resultsDir}/*.xml
                    } else {
                        sh(script: """#!/bin/bash
                                set -e +o pipefail
                                _tmp=\$(pwd)
                                dotnet build
                                # dotnet dotcover test ${testSnapshotArgs}
                                #dotnet dotcover test ${testReportArgs}
                                #mv dotCover.Output* ${resultsDir}
                                cd \$_tmp
                                #export CODACY_PROJECT_TOKEN=${codeacyProjectToken}
                                #./get.sh report -r ${dotnetDir}/${resultsDir}/*.xml
                                """, label: "Dotnet Test Coverage")
                        //archiveArtifacts artifacts: "${dotnetDir}/${resultsDir}/**/*.*, ", onlyIfSuccessful: true
                    }
                    bitbucketStatusNotify(buildState: 'SUCCESSFUL')
                }
            }
            else if (stepName == "DotNet Build") {
                if (env.BRANCH_NAME == "${masterBranch}" && env.TAG_NAME == null) {
                            environment {
                                JENKINS_URL = "${env.JENKINS_URL}"
                                // GitVersion_SemVer = ""
                                // GitVersion_BranchName = ""
                                // GitVersion_AssemblySemVer = ""
                                // GitVersion_MajorMinorPatch = ""
                                // GitVersion_Sha = ""
                            }
                            script {
                                echo "IN the dotnet build"
                                // sshagent(["${jenkinsSshKey}"]) {
                                //      sh(script: """#!/bin/bash
                                //      set -e +o pipefail
                                //      export GIT_SSH_COMMAND="ssh -oStrictHostKeyChecking=no"
                                //      #git fetch --unshallow
                                //      #git config --unset-all remote.origin.fetch
                                //      #git config --add remote.origin.fetch +refs/heads/master:refs/remotes/origin/master
                                //      #git config --add remote.origin.fetch +refs/heads/master:refs/remotes/origin/feature/*
                                     
                                //      """)
                                // }
                                sh(script:"""#!/bin/bash
                                set -e +o pipefail
                                #dotnet gitversion /output buildserver
                                #dotnet gitversion /showvariable FullSemVer
                                echo \"IN some method\"
                                """)
                                
                                //def props = readProperties file: 'gitversion.properties'
                            //env.GitVersion_SemVer = props.GitVersion_SemVer
                              //  echo "${env.GitVersion_SemVer}"
                                //Versioning in dotnet by just numbers, no pre/suffix
                                //gitversion /showvariable FullSemVer
                                //env.versionTag = sh(returnStdout:true, script: "echo \"${props.GitVersion_SemVer}\" | cut -d '-' -f2").trim()
                                // env.GitVersion_BranchName = props.GitVersion_BranchName
                                // env.GitVersion_AssemblySemVer = props.GitVersion_AssemblySemVer
                                // env.versionPatch = props.GitVersion_AssemblySemFileVer
                                // env.GitVersion_MajorMinorPatch = props.GitVersion_MajorMinorPatch
                                // env.GitVersion_Sha = props.GitVersion_Sha
                                // env.GIT_COMMIT = props.GitVersion_Sha
                                // env.elbVersionLabel = sh(returnStdout: true, script: "echo ${tagPrefix}${env.versionTag}").trim()
                                // env.elbS3Key = sh(returnStdout: true, script: "echo ${appName}/${env.elbVersionLabel}.zip").trim()
                                // env.s3url = sh(returnStdout: true, script: "echo s3://${elbS3Bucket}/${env.elbS3Key}").trim()
                                //echo "${env.elbVersionLabel} and ${env.versionTag} and s3url ${env.s3url} and ${env.GIT_COMMIT}"
                            }
                            sh(script: """#!/bin/bash
                                set -e +o pipefail
                                _tmp=\$(pwd)
                                #cd ${dotnetDir}${projectPath}
                                #rm -rf ${outDir}
                                dotnet publish "threes.csproj" -c debug -o ${outDir} -p:Version="6.1.20}"
                                cd ${outDir}
                                zip "../${env.elbVersionLabel}.zip" -r * .[^.]*
                                cd ..
                                if [[ \$(aws s3 ls ${env.s3url} | wc -l) -eq 0 ]]; then
                                    echo "Uploading to ${env.s3url}"
                                    aws s3 cp ${env.elbVersionLabel}.zip ${env.s3url} --metadata commit-id="${env.GIT_COMMIT}"
                                fi
                            """, label: "DotNet Publish Script")
                    sshagent(["${jenkinsSshKey}"]) {
                                sh(script: """#!/bin/bash
                                    set -e +o pipefail
                                    _email=\$(git log -1 --format='%ae' ${env.GIT_COMMIT})
                                    _username=\$(git log -1 --format='%an' ${env.GIT_COMMIT})
                                    echo "\$_email \$_username"
                                    #git remote set-head origin ${masterBranch}
                                    #git config user.email \$_email
                                    #git config user.name \$_username
                                    #export GIT_SSH_COMMAND="ssh -oStrictHostKeyChecking=no"
                                    #git fetch --tags
                                    #git tag -a ${env.elbVersionLabel} -m '${gitTagMessage}'
                                    #git push origin ${env.elbVersionLabel}
                                    #echo "${env.appEnviron} ${env.elbVersionLabel} ${env.GIT_COMMIT}"
                                """)
                    }
                }
            }
            else if (stepName == "DotNet Deploy") {
                if (env.BRANCH_NAME == "${masterBranch}" || env.TAG_NAME) {
                    echo "Doing the deploy step"
                    sh(script:"""
                        echo 'DELETE: output vars before deploy'
                        printenv
                        """)
                    // if (beanstalkApplication) {
                    //         sh(script: """#!/bin/bash
                    //             set -e +o pipefail
                    //             cd \$_tmp
                    //             echo "Creating AppVersion ${env.elbVersionLabel}"
                    //             _config=\${${env.appEnviron},,}
                    //             _desc="AppVersion:${env.BUILD_URL} ${env.GIT_CURRENT_CMT_URL}"
                    //             _elbAppName=\${${env.elbVersionLabel},,}
                    //             _elbEnv="\${_config}-\${_elbAppName}"
                    //             echo 'aws elasticbeanstalk create-application-version'
                    //             echo '--application-name "\${_elbAppName}" --version-label "${env.elbVersionLabel}" --description "\${_desc:0:200}" --source-bundle S3Bucket="${elbS3Bucket}",S3Key="${env.elbS3Key}"'
                    //             echo '--region "${awsRegion}" --tags Key="ds:created",Value="jenkins" Key="ds:app",Value="\${_elbAppName}" Key="ds:env",Value="\${_config}" --debug;'
                    //             echo "Create beanstalk app version, and deploy"
                    //             echo 'aws elasticbeanstalk update-environment --application-name "\${_elbAppName}" --environment-name "\${_elbEnv}" --version-label "${env.elbVersionLabel}" --debug;'
                    //             echo 'aws elasticbeanstalk wait environment-updated --application-name "\${_elbAppName}" --environment-names "\${_elbEnv}" --version-label "${env.elbVersionLabel}" --debug;'

                    //         """, label: "ElasticBeanstalk Deploy")
                    // }
                    bitbucketStatusNotify(buildState: 'SUCCESSFUL')
                    env.elbVersionLabel="6.1.21"
                    env.appEnviron = "local"
                    buildName "${env.appEnviron}:${env.elbVersionLabel}.${env.BUILD_ID}"
                    buildDescription("App Label : ${env.elbVersionLabel}\nCommit : ${env.GIT_COMMIT}\nEnvironment : ${env.appEnviron}\n")
                    //slackSend color: "good", channel: "${slackChannel}", message: "${env.buildDesc}\n S3Url : https://${elbS3Bucket}.s3.amazonaws.com/${appName}/${env.elbVersionLabel}.zip", timestamp: "${env.runStart}"
                }
            }
       
            else {
           //     slackSend color: "warning", channel: "${slackChannel}", message: "Called Nonexistant Stage : StepName \n ${env.buildDesc}", timestamp: "${env.runStart}"
            }
        } catch (Exception any) {
            bitbucketStatusNotify(buildState: 'FAILED')
        //   slackSend color: "danger", channel: "${slackChannel}", message: "Failed at Stage : ${stepName}\n ${env.buildDesc}", timestamp: "${env.runStart}"
         //   buildDescription(any.getMessage())
            throw any
        }
    }
}