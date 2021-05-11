pipeline {
  options { 
	  disableConcurrentBuilds() 
	  buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '30', daysToKeepStr: '', numToKeepStr: '')
  }
  agent { label 'docker-slave' }
  environment {
       // OPENSTACK SETTINGS
       ssh_key_name = "jenkins-opera"
       image_name = "centos7"
       vm_name = "semantic-web"
       network_name = "orchestrator-network"
       security_groups = "default,sodalite-remote-access,sodalite-uc,sodalite-graphdb"
       flavor_name = "m1.xlarge"
       // DOCKER SETTINGS
       docker_network = "sodalite"
       dockerhub_user = " "
       dockerhub_pass = " "
       docker_registry_cert_country_name = "SI"
       docker_registry_cert_organization_name = "XLAB"
       docker_public_registry_url = "registry.hub.docker.com"
       docker_registry_cert_email_address = "dragan.radolovic@xlab.si"
       //KB DEPLOYMENT SETTINGS
       KB_USERNAME = credentials('kb-username')
       KB_PASSWORD = credentials('kb-password')
       //KEYCLOAK SETTINGS
       KEYCLOAK_URL = credentials('keycloak-url')
       KEYCLOAK_CLIENT_ID = credentials('keycloak-client-id')
       KEYCLOAK_CLIENT_SECRET = credentials('keycloak-client-secret')
       // OPENSTACK DEPLOYMENT FALLBACK SETTINGS
       OS_PROJECT_DOMAIN_NAME = "Default"
       OS_USER_DOMAIN_NAME = "Default"
       OS_PROJECT_NAME = "orchestrator"
       OS_TENANT_NAME = "orchestrator"
       OS_USERNAME = credentials('os-username')
       OS_PASSWORD = credentials('os-password')
       OS_AUTH_URL = credentials('os-auth-url')
       OS_INTERFACE = "public"
       OS_IDENTITY_API_VERSION = "3"
       OS_REGION_NAME = "RegionOne"
       OS_AUTH_PLUGIN = "password"

       // ROOT X.509 CERTIFICATES
       ca_crt_file = credentials('xopera-ca-crt')
       ca_key_file = credentials('xopera-ca-key')

       // CI-CD vars
       // When triggered from git tag, $BRANCH_NAME is actually tag_name
       TAG_SEM_VER_COMPLIANT = """${sh(
                returnStdout: true,
                script: './validate_tag.sh SemVar $BRANCH_NAME'
             )}"""

       TAG_MAJOR_RELEASE = """${sh(
              returnStdout: true,
              script: './validate_tag.sh MajRel $BRANCH_NAME'
             )}"""

      TAG_PRODUCTION = """${sh(
              returnStdout: true,
              script: './validate_tag.sh production $BRANCH_NAME'
             )}"""
   }
  stages {
    stage ('Pull repo code from github') {
      steps {
        checkout scm
      }
    }
    stage('Inspect GIT TAG'){
            steps {
                sh """ #!/bin/bash
                echo 'TAG: $BRANCH_NAME'
                echo 'Tag is compliant with SemVar 2.0.0: $TAG_SEM_VER_COMPLIANT'
                echo 'Tag is Major release: $TAG_MAJOR_RELEASE'
                echo 'Tag is production: $TAG_PRODUCTION'
                """
            }

    }
    stage ('Build the code with Maven') {
      steps {
        sh  """ #!/bin/bash
                cd "source code/semantic-reasoner"
                mvn clean install -Ddefault.min.distinct.threshold=104857600
            """
        //archiveArtifacts artifacts: '**/*.war, **/*.jar', onlyIfSuccessful: true
      }
    }
    stage('SonarQube analysis'){
        environment {
          scannerHome = tool 'SonarQubeScanner'
        }
        steps {
            withSonarQubeEnv('SonarCloud') {
                sh  """ #!/bin/bash
                        cd "source code/semantic-reasoner"
                        ${scannerHome}/bin/sonar-scanner
                    """
            }
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
   stage('Build docker images') {
            when {  // Only on production tags
               // branch "master" 
			         allOf {
                    expression{tag "*"}
                    expression{
                        TAG_PRODUCTION == 'true'
                    }
                } 
            }
            steps {
                sh "cd source\\ code/semantic-reasoner; docker build -t semantic_web -f  ./docker/web/Dockerfile ."
                sh "cd source\\ code/semantic-reasoner; docker build -t graph_db -f  ./docker/graph-db/Dockerfile ."
            }
   }
   stage('Push Reasoner to DockerHub') {
           when {  // Only on production tags
                allOf {
                    expression{tag "*"}
                    expression{
                        TAG_PRODUCTION == 'true' || tag "graphdb-*"
                    }
                }
			//branch "master"  
            }
            steps {
                withDockerRegistry(credentialsId: 'jenkins-sodalite.docker_token', url: '') {
                    sh  """#!/bin/bash                       
                            docker tag semantic_web sodaliteh2020/semantic_web:${BRANCH_NAME}
                            docker tag semantic_web sodaliteh2020/semantic_web
                            docker push sodaliteh2020/semantic_web:${BRANCH_NAME}
                            docker push sodaliteh2020/semantic_web:latest
                        """
                }
            }
   }
   stage('Push graphdb to DockerHub') {
            when {
               tag "graphdb-*"
            }
            steps {
                withDockerRegistry(credentialsId: 'jenkins-sodalite.docker_token', url: '') {
                    sh  """#!/bin/bash                       
                            docker tag graph_db sodaliteh2020/graph_db:${BRANCH_NAME}
                            docker tag graph_db sodaliteh2020/graph_db
                            docker push sodaliteh2020/graph_db:${BRANCH_NAME}
                            docker push sodaliteh2020/graph_db:latest
                        """
                }
            }
   }
   stage('Install dependencies') {
             when {  // Only on production tags
                allOf {
                    expression{tag "*"}
                    expression{
                        TAG_PRODUCTION == 'true'
                    }
                }
			//branch "master" 
            }
            steps {
                sh """#!/bin/bash
                    rm -rf venv
                    python3 -m venv venv
                    . venv/bin/activate
                    python3 -m pip install --upgrade pip
                    python3 -m pip install opera[openstack]==0.6.4 docker
                    ansible-galaxy install -r openstack-blueprint/requirements.yml --force
                    rm -r -f openstack-blueprint/modules/
                    git clone -b 3.4.1 https://github.com/SODALITE-EU/iac-modules.git openstack-blueprint/modules/
                    cp ${ca_crt_file} openstack-blueprint/modules/docker/artifacts/ca.crt
                    cp ${ca_crt_file} openstack-blueprint/modules/misc/tls/artifacts/ca.crt
                    cp ${ca_key_file} openstack-blueprint/modules/docker/artifacts/ca.key
                    cp ${ca_key_file} openstack-blueprint/modules/misc/tls/artifacts/ca.key
                   """
            }
   }
   stage('Deploy to openstack') {
            when {  // Only on production tags
                allOf {
                    expression{tag "*"}
                    expression{
                        TAG_PRODUCTION == 'true'
                    }
                }
	        //  branch "master"  
            }
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'xOpera_ssh_key', keyFileVariable: 'xOpera_ssh_key_file', usernameVariable: 'xOpera_ssh_username')]) {
                    sh """#!/bin/bash
                        # create input.yaml file from template
                        envsubst < openstack-blueprint/input.yaml.tmpl > openstack-blueprint/input.yaml
                        . venv/bin/activate
                        cd openstack-blueprint
                        rm -rf .opera
                        opera deploy service.yaml -i input.yaml
                       """                  
                }
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
