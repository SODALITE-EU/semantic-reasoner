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
  /* stage('Build docker images') {
            steps {
                sh "cd source\\ code/semantic-reasoner; docker build -t semantic_web -f  ./docker/web/Dockerfile ."
                sh "cd source\\ code/semantic-reasoner; docker build -t graph_db -f  ./docker/graph-db/Dockerfile ."
            }
   }
   stage('Push Dockerfile to DockerHub') {
            when {
               branch "master"
            }
            steps {
                withDockerRegistry(credentialsId: 'jenkins-sodalite.docker_token', url: '') {
                    sh  """#!/bin/bash                       
                            docker tag semantic_web sodaliteh2020/semantic_web:${BUILD_NUMBER}
                            docker tag semantic_web sodaliteh2020/semantic_web
                            docker push sodaliteh2020/semantic_web:${BUILD_NUMBER}
                            docker push sodaliteh2020/semantic_web
                            docker tag graph_db sodaliteh2020/graph_db:${BUILD_NUMBER}
                            docker tag graph_db sodaliteh2020/graph_db
                            docker push sodaliteh2020/graph_db:${BUILD_NUMBER}
                            docker push sodaliteh2020/graph_db
                        """
                }
            }
   }*/
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
