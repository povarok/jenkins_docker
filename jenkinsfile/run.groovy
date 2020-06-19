pipeline {
    agent{node('master')}
    stages {
        stage('Clean workspace & dowload dist') {
            steps {
                script {
                    cleanWs()
                    withCredentials([
                        usernamePassword(credentialsId: 'srv_sudo',
                        usernameVariable: 'username',
                        passwordVariable: 'password')
                    ]) {
                        try {
                            sh "echo '${password}' | sudo -S docker stop nginx_komarov"
                            sh "echo '${password}' | sudo -S docker container rm nginx_komarov"
                        } catch (Exception e) {
                            print 'container not exist, skip clean'
                        }
                    }
                }
                script {
                    echo 'Update from repository'
                    checkout([$class                           : 'GitSCM',
                              branches                         : [[name: '*/master']],
                              doGenerateSubmoduleConfigurations: false,
                              extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                                   relativeTargetDir: 'auto']],
                              submoduleCfg                     : [],
                              userRemoteConfigs                : [[credentialsId: '	4026c49d-8161-4548-a558-017289883160', url: 'https://github.com/povarok/jenkins_docker.git']]])
                }
            }
        }
        stage ('Build & run docker image'){
            steps{
                script{
                     withCredentials([
                        usernamePassword(credentialsId: 'srv_sudo',
                        usernameVariable: 'username',
                        passwordVariable: 'password')
                    ]) {

                        sh "echo '${password}' | sudo -S docker build ${WORKSPACE}/auto -t docker_image_komarov "
                        sh "echo '${password}' | sudo -S docker run -d  --name nginx_komarov -v ${WORKSPACE}/auto:/stats_folder docker_image_komarov"
                    }
                }
            }
        }
        stage ('Get stats & write to file'){
            steps{
                script{
                    withCredentials([
                        usernamePassword(credentialsId: 'srv_sudo',
                        usernameVariable: 'username',
                        passwordVariable: 'password')
                    ]) {
                        try {
                            sh "truncate -s 0 ${WORKSPACE}/stats.txt"
                        } catch (Exception e) {
                            print 'file exist'
                        }
                        sh "echo '${password}' | sudo -S docker exec -t nginx_komarov df -h >> /stats_folder/stats.txt"
                        sh "echo '${password}' | sudo -S docker exec -t nginx_komarov top -n 1 -b >> /stats_folder/stats.txt"
                    }
                }
            }
        }
        
    }

    
}
