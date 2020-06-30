def sudo_pass = 'rtf-ci-456'
pipeline {
    agent{node('master')}
    stages {
        stage('Clean workspace & dowload dist') {
            steps {
                script {
                    cleanWs()
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
                     sh "echo '${sudo_pass}' | sudo -S docker build ${WORKSPACE}/auto -t docker_image_komarov"
                     sh "echo '${sudo_pass}' | sudo -S docker run --rm -d  --name nginx_komarov -v /home/adminci/komarov_docker_mnt:/stats_folder docker_image_komarov"
                }
            }
        }
        stage ('Get stats & write to file'){
            steps{
                script{
                    try {
                        sh "truncate -s 0 ${WORKSPACE}/stats.txt"
                    } catch (Exception e) {
                        print 'file exist'
                    }
                    sh "echo '${sudo_pass}' | sudo -S docker exec -t nginx_komarov  bash -c 'df -h > /stats_folder/stats.txt'"
                    sh "echo '${sudo_pass}' | sudo -S docker exec -t nginx_komarov bash -c 'top -n 1 -b >> /stats_folder/stats.txt'"
                }
            }
        }
        stage ('Stop container & remove image'){
                    steps{
                        script{
                            sh "echo '${sudo_pass}' | sudo -S docker stop nginx_komarov"
                        }
                    }
                }
    }
}
