#!/usr/bin/env groovy
pipeline {
    tools {
        maven "mvn"
        jdk "jdk21"
    }

    agent {
        label "java"
    }

    // Make sure we have GIT_COMMITTER_NAME and GIT_COMMITTER_EMAIL set due to machine weirdness.
    environment {
        GIT_COMMITTER_NAME = "jenkins"
        GIT_COMMITTER_EMAIL = "jenkins@jenkins.io"
    }

    post {
        // always {
        //     junit '*/target/surefire-reports/*.xml'
        // }
        success {
            archive "*/target/**/*"
        }
        unstable {
            archive "*/target/**/*"
        }
    }

    stages {
        stage("build") {
            steps {
                script {
                    infra.runMaven(['clean', 'validate', 'javadoc:aggregate', '-Dmaven.test.failure.ignore=true'], 21)
                }
            }
        }
    }
}
