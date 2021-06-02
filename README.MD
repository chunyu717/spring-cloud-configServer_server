# Run k8s Service
```
jerry@ubuntu:~/workspace/java/configServer-server$ kubectl apply -f deploymentWithService.yaml
service/config-server-k8s created
deployment.apps/config-server created
```

# yaml 將 本地端的/home/jerry/configfile 掛到 container 的 /opt 
@deploymentWithService.yaml 
```
      volumes:
      - name: configfile
        hostPath:
          path: /home/jerry/configfile
          type: Directory
```

# client 指定要吃的 properties file :  active=dev, label=release, app=config-client-k8s
```
jerry@ubuntu:~/workspace/java/configServer-client/src/main/resources$ cat application.properties
server.port=8088
  
spring.application.name=config-client-k8s
spring.profiles.active=dev
spring.cloud.config.label=release

management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always
spring.cloud.config.discovery.enabled=true
spring.cloud.config.discovery.service-id=config-server-k8s
  
```
所以是吃 /home/jerry/configfile/release/config-client-k8s-dev.properties
```
$ cat /home/jerry/configfile/release/config-client-k8s-dev.properties
pkslow.age=2000
pkslow.email="jerry@com.tw"
pkslow.webSite="jerry"
```

# Test : 
```
$ kubectl get services
NAME                TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)           AGE
config-client-k8s   NodePort    10.152.183.4     <none>        8088:32087/TCP    3m59s
config-server-k8s   NodePort    10.152.183.226   <none>        8888:32088/TCP    8m30s
  
# Show server config
$ curl http://10.152.183.226:8888/config-client-k8s/dev/release
{
   "name":"config-client-k8s",
   "profiles":[
      "dev"
   ],
   "label":"release",
   "version":null,
   "state":null,
   "propertySources":[
      {
         "name":"file:/opt/release/config-client-k8s-dev.properties",
         "source":{
            "pkslow.age":"2000",
            "pkslow.email":"\"jerry@com.tw\"",
            "pkslow.webSite":"\"jerry\""
         }
      }
   ]
}
  
# Edit Config file 
vim /opt/release/config-client-k8s-dev.properties 
  age":"2000" => age":"100000"
  

# Brocast
$ curl http://10.152.183.226:8888/refresh
{"Basic Info":"Total services in k8s:4","config-client-k8s http://10.1.56.95:8088/actuator/refresh":"OK","config-client-k8s http://10.1.56.96:8088/actuator/refresh":"OK"}
  
# Client Get parameter
$ curl http://10.152.183.4:8088/pkslow
{"webSite":"\"jerry\"","age":"1000","email":"\"jerry@com.tw\""}

```

# 其他參考資料 : 

# Build docker image 
```
$  ~/java/gradle-5.6.3/bin/gradle clean build
~/java/configServer-client$ docker build -t kingbike/config-server-client .
~/java/configServer-server$ docker build -t kingbike/config-server-server .
$ docker images
```

# Run docker test. (put config file in /home/jerry/configfile
```
$ docker run --rm -p 8888:8888 -v /home/jerry/configfile:/opt --name config-server kingbike/config-server-server
$ docker run -p 8088:8088 kingbike/config-server-client
