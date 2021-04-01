pipeline
        {
            agent any

            tools
                    {
                        maven 'Maven'
                        jdk 'OpenJDK11'
                    }

            options
                    {
                        buildDiscarder(logRotator(numToKeepStr: '10'))
                        skipStagesAfterUnstable()
                        disableConcurrentBuilds()
                    }


            triggers
                    {
                        pollSCM('H 6-18/4 * * 1-5')
                    }


            stages
                    {
                        stage('Initialize')
                                {
                                    steps
                                            {
                                                script
                                                        {
                                                            echo "Current branch: " + env.BRANCH_NAME
                                                            env.DO_TAG = "false"
                                                            env.SKIP_IT = "false"
                                                            env.BUILD_DOCKER = "false"
                                                            env.DOCKER_TAG = "latest"

                                                            // Early abort if we run the pipeline on master.
                                                            if (env.BRANCH_NAME == "master" || env.BRANCH_NAME == "main") {
                                                                currentBuild.result = 'ABORTED'
                                                                error(env.BRANCH_NAME + ' branch is not meant to be built. It acts as a code reference of the latest production version.')
                                                            }

                                                            def matcherRelease = env.BRANCH_NAME =~ /^release\/(.*)$/
                                                            def matcherFeature = env.BRANCH_NAME =~ /^feature\/(.*)$/
                                                            def matcherPr = env.BRANCH_NAME =~ /^(PR-.*)$/

                                                            def versionOpts = ""
                                                            def mvnOpts = ""

                                                            if (matcherRelease.matches()) {
                                                                // Release branches
                                                                echo "RELEASE BRANCH DETECTED!"
                                                                env.BRANCH_TYPE = "release"
                                                                env.DO_TAG = "true"
                                                                //env.BUILD_DOCKER = "true"
                                                                env.RELEASE_VERSION = matcherRelease[0][1]

                                                                // The first build of a release branch does not append a suffix to the version number, only subsequent
                                                                // contain a build number (bX)
                                                                versionOpts = "-DmainVersion=" + env.RELEASE_VERSION + " -Dfeature= -Dmodifier= -DbuildNumber="

                                                                if (env.BUILD_NUMBER != "1") {
                                                                    versionOpts = versionOpts + ".b$BUILD_NUMBER"
                                                                }

                                                                env.DOCKER_TAG = env.RELEASE_VERSION + ".b$BUILD_NUMBER"
                                                            } else if (matcherFeature.matches()) {
                                                                // Feature branches are tagged as snapshot of a particular name, with build number in it.
                                                                echo "FEATURE BRANCH DETECTED!"
                                                                env.BRANCH_TYPE = "feature"
                                                                env.DO_TAG = "true"

                                                                env.FEATURE_NAME = matcherFeature[0][1]
                                                                versionOpts = "-DmainVersion=FEAT -Dfeature=." + env.FEATURE_NAME + " -Dmodifier=-SNAPSHOT -DbuildNumber=.b$BUILD_NUMBER"
                                                                mvnOpts = "-Dmaven.site.skip"
                                                            } else if (matcherPr.matches()) {
                                                                // Pull requests branches are NOT deployed, NOT tagged and NO documentation is generated. Only tests are run.
                                                                echo "PULL REQUEST BRANCH DETECTED!"
                                                                env.BRANCH_TYPE = "pr"

                                                                env.PR_NAME = matcherPr[0][1]
                                                                versionOpts = "-DmainVersion=PR -Dfeature=." + env.PR_NAME + " -Dmodifier=-SNAPSHOT -DbuildNumber=.b$BUILD_NUMBER"
                                                                mvnOpts = "-Dmaven.site.skip"
                                                            } else if (env.BRANCH_NAME == "develop") {
                                                                echo "DEVELOP BRANCH DETECTED"
                                                                env.BRANCH_TYPE = "develop"
                                                                env.SKIP_IT = "true"

                                                                versionOpts = "-DmainVersion=" + env.BRANCH_NAME + " -Dfeature= -Dmodifier=-SNAPSHOT -DbuildNumber=.b$BUILD_NUMBER"
                                                                mvnOpts = "-Dmaven.site.skip"
                                                            } else {
                                                                echo "OTHER BRANCH DETECTED"
                                                                env.BRANCH_TYPE = "other"

                                                                versionOpts = "-DmainVersion=" + env.BRANCH_NAME + " -Dfeature= -Dmodifier=-SNAPSHOT -DbuildNumber=.b$BUILD_NUMBER"
                                                                mvnOpts = "-Dmaven.site.skip"
                                                            }

                                                            echo "  > versioning settings: " + versionOpts
                                                            echo "  > maven settings: " + mvnOpts

                                                            env.MAVEN_ARGS = versionOpts + " " + mvnOpts
                                                        }
                                            }
                                }

                        stage('Clean')
                                {
                                    steps
                                            {
                                                script
                                                        {
                                                            sh 'mvn --batch-mode clean ${MAVEN_ARGS}'
                                                        }
                                            }
                                }

                        stage('Build')
                                {
                                    steps
                                            {
                                                script
                                                        {
                                                            sh 'mvn --batch-mode package -DskipUTs -DskipITs ${MAVEN_ARGS}'
                                                        }
                                            }
                                }


                        stage('Unit Tests')
                                {
                                    steps
                                            {
                                                script
                                                        {
                                                            sh 'mvn --batch-mode verify -DskipITs ${MAVEN_ARGS}'
                                                        }
                                            }
                                    post
                                            {
                                                always
                                                        {
                                                            junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                                                        }
                                            }
                                }

                        stage('Integration Tests')
                                {
                                    steps
                                            {
                                                script
                                                        {
                                                            echo env.SKIP_IT
                                                            if (env.SKIP_IT == "true") {
                                                                echo "skipping ITs"
                                                            } else {
                                                                sh 'mvn --batch-mode verify -DskipUTs ${MAVEN_ARGS}'
                                                                junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: true
                                                            }
                                                        }
                                            }
                                }

                        stage('Install')
                                {
                                    steps
                                            {
                                                script
                                                        {
                                                            sh 'mvn --batch-mode install -DskipUTs -DskipITs ${MAVEN_ARGS}'
                                                        }
                                            }
                                }

                        stage('Documentation')
                                {
                                    steps
                                            {
                                                script
                                                        {
                                                            // TODO fix site:stage, fails because of lack of distributionManagement tag in pom.
                                                            sh 'mvn --batch-mode site ${MAVEN_ARGS}'
                                                            publishHTML(target: [reportName: 'Site', reportDir: 'target/site', reportFiles: 'index.html', keepAll: false])
                                                        }
                                            }
                                }


                        stage('Deploy and Tag')
                                {
                                    steps
                                            {
                                                script
                                                        {
                                                            if (env.DO_TAG == "true") {
                                                                sh 'mvn --batch-mode source:jar site:jar deploy scm:tag -DskipUTs -DskipITs ${MAVEN_ARGS}'
                                                            } else {
                                                                sh 'mvn --batch-mode source:jar site:jar deploy -DskipUTs -DskipITs ${MAVEN_ARGS}'
                                                            }
                                                        }
                                            }
                                }


                        stage('Docker Push')
                                {
                                    steps
                                            {
                                                script
                                                        {
                                                            echo env.BUILD_DOCKER
                                                            echo env.DOCKER_TAG

                                                            if (env.BUILD_DOCKER == "true") {
                                                                sh 'docker buildx build --platform linux/arm64,linux/arm/v7 --push -t sebpiller/my-project:latest -t sebpiller/my-project:${DOCKER_TAG} .'
                                                            } else {
                                                                sh 'echo "No docker push for this kind of branch: ${BRANCH_TYPE}"'
                                                            }
                                                        }
                                            }
                                }
                    }
        }