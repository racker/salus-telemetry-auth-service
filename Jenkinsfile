def label = "maven-${UUID.randomUUID().toString()}"

podTemplate(label: label, containers: [
  containerTemplate(name: 'maven', image: 'maven:3-jdk-8', ttyEnabled: true, command: 'cat')
  ])
{
    node(label) {
        container('maven') {
            ansiColor('xterm') {
                stage('Checkout') {
                    notifyStarted()
                    checkout scm
                    googleStorageDownload bucketUri: 'gs://monplat-jenkins-artifacts/settings.xml', credentialsId: 'monplat-jenkins', localDirectory: './.mvn/'
                }
                stage('Maven install') {
                  sh 'mvn install -Dmaven.test.skip=true -s .mvn/settings.xml'
                }
                stage('Integration Test') {
                  // sh 'mvn integration-test'
                  sh 'TZ=":America/Chicago" date'
                }
                stage('Deploy snapshot') {
                  sh 'mvn deploy -Dmaven.test.skip=true -s .mvn/settings.xml'
                }
                stage('Deploy docker') {
					withCredentials([[$class: 'FileBinding', credentialsId: 'salus-dev-gcr', variable: 'GOOGLE_APPLICATION_CREDENTIALS']]) {
						sh 'gcloud auth activate-service-account --key-file $GOOGLE_APPLICATION_CREDENTIALS'
						sh 'mvn -P docker -Dmaven.deploy.skip=true -DskipLocalDockerBuild=true -Ddocker.image.prefix=gcr.io/salus-220516 -s .mvn/settings.xml deploy'
					}
                }
            }
        }
    }
}

def notifyStarted() {
    slackSend (color: '#FFFF00', message: "STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL}) on branch: ${env.GIT_BRANCH}")
}

def notifySuccessful() {
    slackSend (color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL}) on branch: ${env.GIT_BRANCH}")
}

def notifyFailed() {
    slackSend (color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL}) on branch: ${env.GIT_BRANCH}")
}
