pipeline {
  agent any
  stages {
    stage('Build Parameters') {
      when {
        branch "main"
      }
      steps {
        script {
          properties([parameters([booleanParam(defaultValue: false,
                  description: 'Deploy',
                  name: 'DEPLOY')])])
        }
      }
    }

    stage('Build Project') {
      when {
        expression {
          return params.DEPLOY != true
        }
      }
      steps {
        sh 'bin/mvn clean install'
      }
    }

    stage('Build and Publish') {
      when {
        branch 'main'
        expression {
          return params.DEPLOY == true
        }
      }
      steps {
        // Ensure that we use the deploy key to deploy, why jenkins has no
        // configuration for this, I have no idea.
        // See https://stackoverflow.com/questions/61148043/add-a-tag-to-a-repository-with-jenkinsfile-pipeline-with-credentials for alternative ideas
        sh 'bin/dev ensure-jenkins-setup'
        sh 'bin/dev release'
      }
    }

    stage('Archive') {
      steps {
        archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
      }
    }
  }
}
