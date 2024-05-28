pipeline {
    
	agent any 

    parameters {
        booleanParam(name: 'SKIP_CONFIRMATION', defaultValue: false, description: 'Skip user confirmation for deployment')
    }

    environment {
        AZURE_CREDENTIALS_ID = 'azurecred'
        registryCredentials = 'docker'
        registry = 'amrish24/containerappui'
        img_name_tags = "${registry}:${BUILD_ID}"
        RESOURCE_GROUP = 'ter-RG'
        CONTAINER_APP_NAME = 'ter-ca-node'
    }
	
    stages {
        stage('CLEAN WS'){
            steps{
                cleanWs()
            }
        }
	stage("CLONE GIT") {
            steps { 
                    // Let's clone the source
                    git branch: 'main', url: 'https://github.com/Amrish24-S/containerapps-ui.git'
            }
        }
        stage('IMAGE BUILD'){
            steps {
                script{
                    dockerImage = docker.build("${img_name_tags}", "-f src/Dockerfile src")
                }
            }
        }
        stage ('PUSH IMAGE TO DOCKERHUB') {
            steps {
                script{
                    docker.withRegistry('', registryCredentials){
                        dockerImage.push("$BUILD_ID")
                        dockerImage.push("latest")
                    }
                }
            }
        }
        stage('User Input for Deployment') {
            when {
                expression { return !params.SKIP_CONFIRMATION }
            }
            steps {
                script {
                    input message: 'Deploy to Azure Container Apps?', ok: 'Deploy'
                }
            }
        }
        stage('Deploy to Azure Container Apps') {
            steps {
                script {
                    withCredentials([azureServicePrincipal(credentialsId: env.AZURE_CREDENTIALS_ID)]) {
                        sh '''
                        az login --service-principal --username $AZURE_CLIENT_ID --password $AZURE_CLIENT_SECRET --tenant $AZURE_TENANT_ID
                        az account set --subscription $AZURE_SUBSCRIPTION_ID

                        az containerapp update \
                            --name $CONTAINER_APP_NAME \
                            --resource-group $RESOURCE_GROUP \
                            --image ${img_name_tags}
                        '''
                    }
                }
            }
        }   
    }
}
