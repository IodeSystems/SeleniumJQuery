pipeline {
  agent any
  stages {
    stage('Build Parameters') {
      when {
        branch "main"
      }
      steps {
        script {
          properties([parameters([
                  booleanParam(defaultValue: false,
                          description: 'Release',
                          name: 'RELEASE'),
                  stringParam(defaultValue: 'auto',
                          description: 'Version, "auto" for automatic versioning, or specify a version number (e.g. 1.0.0)',
                          name: 'VERSION'),
          ])])
        }
      }
    }

    // When building on the main branch, we want to deploy the project to the
    // repository to expose the artifact to other projects
    stage('Deploy Project') {
      when {
        branch "main"
      }
      steps {
        sh 'bin/mvn clean deploy --no-transfer-progress --update-snapshots'
      }
    }

    // When building on a branch other than main, we intend to build the project
    // but not deploy it to the repository. Since it is not mainline yet.
    stage('Build Project') {
      when {
        not { branch "main" }
      }
      steps {
        sh 'bin/mvn clean package --no-transfer-progress --update-snapshots'
      }
    }

    stage('Release') {
      when {
        branch "main"
        expression {
          return params.RELEASE == true
        }
      }
      steps {
        sh "bin/dev release '$VERSION'"
      }
    }
  }
  post {
    always {
      junit allowEmptyResults: true,
              testResults: 'test-results.xml'
      archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
    }
  }
}
