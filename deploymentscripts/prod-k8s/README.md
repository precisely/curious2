# Precisely-k8s

## PRODUCTION
### Prerequisites

1. Google cloud account with billing enabled.

2. Basic knowledge of Kubernetes Pods, ReplicationControllers, Services, Networking.
3. **kubectl** (Kubernetes command line tool to interact with Kubernetes cluster): See
 [download and setup kubectl](https://kubernetes.io/docs/tasks/kubectl/install/).

 Once you are done with the installation, open the terminal and type in `kubectl version` to verify the kubectl installation.

 ```
╭─causecode@causecode-Inspiron-5548 /home/repos/curious
╰─$ kubectl version
Client Version: version.Info{Major:"1", Minor:"5", GitVersion:"v1.5.3", GitCommit:"029c3a408176b55c30846f0faedf56aae5992e9b", GitTreeState:"clean", BuildDate:"2017-02-15T06:40:50Z", GoVersion:"go1.7.4", Compiler:"gc", Platform:"linux/amd64"}
Server Version: version.Info{Major:"1", Minor:"6", GitVersion:"v1.6.0", GitCommit:"fff5156092b56e6bd60fff75aad4dc9de6b6ef37", GitTreeState:"dirty", BuildDate:"2017-04-07T20:43:50Z", GoVersion:"go1.7.1", Compiler:"gc", Platform:"linux/amd64"}
 ```

**Reference**: Offcial docs @ [kubernetes.io](http://kubernetes.io/)

### List of docker images used and their versions -

#### List of docker images used and their versions

**1\.** Mysql: mysql:5.5

**2\.** Jetty: jetty:9.4.4-jre8-alpine

**3\.** ElasticSearch: elasticsearch:1.7-alpine

**4\.** Analytics Server: Custom image built from clojure:lein-2.7.1-alpine

**Note**: Custom image has been added to google container registry. If you want to recreate this image with modifications in the analytics code then run the Dockerfile.
To push the image to registry follow - https://cloud.google.com/container-registry/docs/pushing-and-pulling
To create image pull secret follow - http://stackoverflow.com/questions/36283660/creating-image-pull-secret-for-google-container-registry-that-doesnt-expire/36286707

### Deploying curious on Google cloud -

1. Create a namespace for Curious by running `kubectl create -f curious-namespace.yaml`

2. Now create the nfs server by running nfs-svc.yaml file and then nfs-rc.yaml file.
As soon as the service for nfs server is created, a cluster ip is assigned to this service. This IP should be assigned to the persistent volume using this server(See curious-pv-nfs.yaml).
Create the persistent volume and the claim to use this volume.

3. Now, create the services using command `kubectl create -f resource-svc.yaml`. In this way create service for
nginx, tomcat, solr, mysql and letsencrypt.

4. Once the service for nginx has been created then a IP will be assigned to this service. This IP changes everytime
the service is recreated. To bind this IP permanently to the service, go to the google cloud console **networking**
tab and choose external IPs section. Now find the IP which has been assigned to the nginx service and mark it static.
To bind this IP to the service modify the `nginx-svc.yaml` file and add the IP to the loadBalancerIP property.

5. Now we need to create our nginx config file using the command -
`kubectl create configmap <config_name> --from-file=<config_file> --namespace=<your_namespace`

In our case here we use the command -
`kubectl create configmap nginx-config --from-file=nginx.conf --namespace=prod-curious`

6. In the next step we generate a letsencrypt certificate. The steps to create and save certificate has been provided
 in the next section.

7. We are using Google Cloud SQL to manage our database. The instructions on how the instance was created are given [here](https://cloud.google.com/sql/docs/mysql/quickstart)
To import data on the cloud SQL instance follow the steps [here](https://cloud.google.com/sql/docs/postgres/import-export/importing)
To see how to connect to Google Cloud SQL from your compute engine instance follow the steps in the [doc](https://cloud.google.com/sql/docs/mysql/connect-container-engine)

8. After the Cloud SQL is ready and data has been imported we can create the rest of the pods i.e jetty. Jetty pod runs two containers, one for jetty and other for mysql.
The mysql pod serves as proxy server to connect to the Google Cloud sql instance.

Note - Before creating the jetty pod you need to copy the war file and LocalConfig.groovy files to the nfs volume so that when jetty pod is created these files can be found and war can be exploded.

To copy a file from your host machine to a pod use the command -
`kubectl cp <path/to/file> <namespace>/<nfs-pod-name>:<path/to/destination> -c <pod-name-on-rc-file>`

In our case here we use command -
`kubectl cp /path/to/root.war prod-curious/<nfs-pod-name>:/exports/jetty/webapps/ -c nfs-server`
Note - To get the pod name use command `kubectl get pods --namespace=prod-curious`

Here is the list of files and their respective directories which will be needed -

- root.war file in /exports/jetty/webapps/
- cu-gcs-dev.json file in /exports/jetty/localconfig/
- Localconfig.groovy file in /exports/jetty/localconfig/


### SSL Configuration for Curious

For generating SSL certificate for Curious, we will be obtaining certificate from letsencrypt certification authority.

For more information visit: [https://github.com/ployst/docker-letsencrypt](https://github.com/ployst/docker-letsencrypt)

**Note**: Currently first-time deployment requires us to start nginx Pod without any SSL configuration.

1. Comment out SSL block and other SSL configuration from `nginx.conf`. (Configuration file `nginx-conf-without-ssl.conf` is provided)
You can simply copy the contents from this file to `nginx.conf` for this step.

2. Also, comment out `letsencrypt-cert` volume and volumeMounts configurations.

 ```
 - name: letsencrypt-cert
      secret:
         secretName: cert-wearecurio.us
 ```
 and
 ```
 volumeMounts:
    - name: letsencrypt-cert
       mountPath: /etc/nginx/nginx-ssl
 ```
3. Create `ReplicationController` and `Service` for `letsencrypt`.

4. Add `upstream` in `nginx.conf` for `letsencrypt` nginx server.

 ```
 # upstream created to handle challenge response from letsencrypt server.
 upstream letsencrypt {
 	server letsencrypt-svc.prod-curious:80;
 }
 ```
 and proxy the incoming request on `/.well-known/acme-challenge` to `letsencrypt` Service.

 ```
server {
                listen 80;
                server_name wearecurio.us;

                // Add this block
                location /.well-known/acme-challenge {
                        proxy_set_header  Host $http_host;
                        proxy_set_header X-Real-IP $remote_addr;
                        proxy_set_header X-Forwarded-Host $host;
                        proxy_set_header X-Forwarded-Server $host;
                        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

                        proxy_pass http://letsencrypt/.well-known/acme-challenge;
                }
}
```

5. Now, run the following command.
 ```
kubectl exec -it --namespace=prod-curious <LETSENCRYPT_POD_NAME> -c letsencrypt -- /bin/bash -c 'EMAIL=test@causecode
.com
DOMAINS=wearecurio.us ./fetch_certs.sh'
 ```
It will fetch the certificate from the letsencrypt server.

6. After fetching the certificate we'll be storing the certificate in the `Secret` resource.
 To save the certificates, run the following command
 ```
kubectl exec -it --namespace=prod-curious <LETSENCRYPT_POD_NAME> -c letsencrypt -- /bin/bash -c 'EMAIL=test@causecode
.com
DOMAINS=wearecurio.us SECRET_NAME=cert-wearecurio.us NAMESPACE=prod-curious ./save_certs.sh'
 ```

 Certificates will be stored inside secret named `cert-wearecurio.us`

6. Now, Add the SSL configuration back, update the nginx `Deployment` with `letsencrypt-cert` volume and volumeMounts configuration. Restart the nginx Pod.

7. letsencrypt certificates expire after 90 days. A cron job running in the background is responsible for refreshing the certificates before they are expired.