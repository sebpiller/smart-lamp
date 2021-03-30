pipeline
{
agent any

environment {
    BRANCH = "${env.BRANCH_NAME}"
}

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
  // MINUTE HOUR DOM MONTH DOW
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
          if( env.BRANCH_NAME == "master" || env.BRANCH_NAME == "main" ) {
              currentBuild.result = 'ABORTED'
              error(env.BRANCH_NAME + ' branch is not meant to be built. It acts as a code reference of the latest production version.')
          }

          def matcherRelease = env.BRANCH_NAME =~ /^release\/(.*)$/
          def matcherFeature = env.BRANCH_NAME =~ /^feature\/(.*)$/
          def matcherPr = env.BRANCH_NAME =~ /^(PR-.*)$/

          def versionOpts = ""
          def mvnOpts = ""

          if(matcherRelease.matches()) {
              // Release branches
              echo "RELEASE BRANCH DETECTED!"
              env.BRANCH_TYPE = "release"
              env.DO_TAG = "true"
              env.BUILD_DOCKER = "true"
              env.RELEASE_VERSION = matcherRelease[0][1]

              // The first build of a release branch do not append a suffix to the version number, only subsequent
              // contain a build number (bX)
              if( env.BUILD_NUMBER == "1") {
                  versionOpts = "-Dbranch=" + env.RELEASE_VERSION + " -Dfeature= -Drevision= -Dmodifier="
              } else {
                  versionOpts = "-Dbranch=" + env.RELEASE_VERSION + " -Dfeature= -Drevision=.b$BUILD_NUMBER -Dmodifier="
              }

              env.DOCKER_TAG = env.RELEASE_VERSION + ".b$BUILD_NUMBER"
          } else if(matcherFeature.matches()) {
              // Feature branches are tagged as snapshot of a particular name, with build number in it.
              echo "FEATURE BRANCH DETECTED!"
              env.BRANCH_TYPE = "feature"
              env.DO_TAG = "true"

              env.FEATURE_NAME = matcherFeature[0][1]
              versionOpts = "-Dbranch=FEAT -Dfeature=." + env.FEATURE_NAME + " -Drevision=.b$BUILD_NUMBER -Dmodifier=-SNAPSHOT"
              mvnOpts = "-Dmaven.site.skip"
          } else if(matcherPr.matches()) {
              // Pull requests branches are NOT deployed, NOT tagged and NO documentation is generated. Only tests are run.
              echo "PULL REQUEST BRANCH DETECTED!"
              env.BRANCH_TYPE = "pr"

              env.PR_NAME = matcherPr[0][1]
              versionOpts = "-Dbranch=PR -Dfeature=." + env.PR_NAME + " -Drevision=.b$BUILD_NUMBER -Dmodifier=-SNAPSHOT"
              mvnOpts = "-Dmaven.site.skip"
          } else if(env.BRANCH_NAME == "develop") {
              echo "DEVELOP BRANCH DETECTED"
              env.BRANCH_TYPE = "develop"
              env.SKIP_IT = "true"

              versionOpts = "-Dbranch=" + env.BRANCH_NAME + " -Drevision=.b$BUILD_NUMBER -Dmodifier=-SNAPSHOT"
              mvnOpts = "-Dmaven.site.skip"
          } else {
              echo "OTHER BRANCH DETECTED"
              env.BRANCH_TYPE = "other"

              versionOpts = "-Dbranch=" + env.BRANCH_NAME + " -Drevision=.b$BUILD_NUMBER -Dmodifier=-SNAPSHOT"
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
          sh '''
              mvn --batch-mode clean ${MAVEN_ARGS}
          '''
       }
     }
   }

  stage('Build')
   {
    steps
     {
      script
       {
          sh '''
             mvn --batch-mode package -DskipUTs -DskipITs ${MAVEN_ARGS}
          '''
       }
     }
   }


  stage('Unit Tests')
   {
    steps
     {
      script
       {
          sh '''
             mvn --batch-mode verify -DskipITs ${MAVEN_ARGS}
          '''
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

 stage('Integration tests')
  {
   steps
    {
     script
      {
        echo env.SKIP_IT
        if ( env.SKIP_IT == "true" ) {
            echo "skipping ITs"
        } else {
             sh '''
                 mvn --batch-mode verify -DskipUTs ${MAVEN_ARGS}
             '''
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
         sh '''
             mvn --batch-mode install -DskipUTs -DskipITs ${MAVEN_ARGS}
         '''
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
         sh '''
             mvn --batch-mode site -X ${MAVEN_ARGS}
         '''
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
         if ( env.DO_TAG == "true" ) {
             echo "Site and tag for this kind of branch: " + env.BRANCH_TYPE
             sh '''
                 mvn --batch-mode source:jar site:jar deploy scm:tag -DskipUTs -DskipITs ${MAVEN_ARGS}
             '''
         } else {
             echo "Skip and doc tag for this kind of branch: " + env.BRANCH_TYPE

             sh '''
                 mvn --batch-mode source:jar site:jar deploy -DskipUTs -DskipITs ${MAVEN_ARGS}
             '''
         }
       }
     }
   }

 }

}