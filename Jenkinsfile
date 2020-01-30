pipeline {
  options { disableConcurrentBuilds() }
  agent { label 'docker-slave' }
  stages {
    stage ('Pull repo code from github') {
      steps {
        checkout scm
      }
    }
    stage ('Build the code with Maven') {
      steps {
        sh  """ #!/bin/bash
                cd "source code/semantic-reasoner"
                mvn install
            """
        archiveArtifacts artifacts: '**/*.war, **/*.jar', onlyIfSuccessful: true
      }
    }
    stage ('Trigger a build of defect-prediction') {
      when { 
          not { 
              triggeredBy 'UpstreamCause' 
          }
      }
      steps {
        build job: 'defect-prediction/master', wait: false
      }
    }
  }
  post {
    failure {
        slackSend (color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
    }
    fixed {
        slackSend (color: '#6d3be3', message: "FIXED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})") 
    }
  }
}
