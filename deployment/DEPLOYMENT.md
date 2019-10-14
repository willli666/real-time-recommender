## Step Zero: Prepare K8S

#### 1. are we at the right cluster? 
kubectl config current-context

#### 2. set Google Cloud to the right cluster for local kubectl
gcloud container clusters get-credentials [CLUSTER_NAME]

https://cloud.google.com/kubernetes-engine/docs/how-to/cluster-access-for-kubectl

or simplely go to the cluster website and use the connect button on cluster view to get the pre-filled commands

## Step One: Launch Zookeeper
```
kubectl apply -f zookeeper.json
kubectl apply -f zookeeper-service.json
```

now make sure zookeeper is running and accessible
```
kubectl get pods
kubectl get services
// try this from a pod
// sh to zookeeper's pod
kubectl exec -it zookeeper -c zookeeper sh  
// install nc
yum install -y nmap
// check result
echo ruok | nc <zookeeper Cluster Ip> 2181; echo
```

## Step Two: Launch Storm Service
```
kubectl apply -f storm-nimbus.json
// wait for pod to be ready
kubectl apply -f storm-nimbus-service.json
```

Check to see if Nimbus is running and accessible
```
$ kubectl get services
NAME                          TYPE        CLUSTER-IP    EXTERNAL-IP   PORT(S)             AGE
nimbus                        ClusterIP   10.52.7.8     <none>        6627/TCP            83s
zookeeper                     ClusterIP   10.52.6.228   <none>        2181/TCP            10m
```
SSH into nimbus server and configure ZOOKEEPER_SERVICE_HOST and NIMBUS_SERVICE_HOST
```
kubectl exec -it nimbus -c nimbus sh
/configure.sh 10.52.6.228 10.52.7.8
./bin/storm list
```
## Step Three: Launch Storm Worker
```
kubectl apply -f storm-worker-controller.yaml
```
ssh into zookeeper to check its status
```
kubectl exec -it zookeeper -c zookeeper sh
echo stat | nc <zookeeper cluster ip>  2181; echo
```
There should be one client from the Nimbus service and one per
worker. Ideally, you should get ```stat``` output from ZooKeeper
before and after creating the replication controller.



## Step Four: Deploy Kafka
https://dzone.com/articles/ultimate-guide-to-installing-kafka-docker-on-kuber
```
kubectl apply -f kafka-service.yaml
// get details after creation
kubectl describe svc kafka-service
// check the LoadBalancer Ingress and NodePort, edit them in kafka-broker
```

edit kafka-broker.yaml
```
  - name: KAFKA_ADVERTISED_PORT
    value: "9092"
  - name: KAFKA_ADVERTISED_HOST_NAME
    value: 35.222.168.239
  - name: KAFKA_ZOOKEEPER_CONNECT
    value: 10.52.6.228:2181
```
kubectl apply -f kafka-broker.yaml

test with kafka cat  https://github.com/edenhill/kafkacat
cat README | kafkacat -b 35.222.168.239:9092 -t admintome-test
kafkacat -b 35.222.168.239:9092 -t admintome-test
kafkacat -L -b 35.222.168.239:9092

## Step Five: Deploy Recommender WebServer
0. edit the ip in Config.scale and kafka-0.10.0.1--.properties
1. build the Dockerfile in root
2. push the new built file to google registry
```
docker tag recommender  gcr.io/apollo-230806/recommender:v3
docker push gcr.io/apollo-230806/recommender:v3
docker tag [SOURCE_IMAGE] [HOSTNAME]/[PROJECT-ID]/[IMAGE]:[TAG]
docker push [HOSTNAME]/[PROJECT-ID]/[IMAGE]
gcloud docker -- push gcr.io/apollo-230806/recommender //on gcp vm
```
3. edit the recommender-deployment for image version
4. deploy app and then service
```
kubectl apply -f recommender-deployment.yaml
kubectl apply -f recommender-service.yaml
```

## Step Six: Deploy the Topology in Storm Service
```
kubectl exec -it nimbus -c nimbus sh
cd /real-time-recommender
git pull
sbt 'set test in assembly := {}' assembly
```
submit the topology file
/opt/apache-storm/bin/storm jar /real-time-recommender/target/recommender-processor-assembly-1.0.jar storm.Topology
