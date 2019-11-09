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
nimbus                        ClusterIP   10.52.14.175     <none>        6627/TCP            83s
zookeeper                     ClusterIP   10.52.6.228   <none>        2181/TCP            10m
```
SSH into nimbus server and configure ZOOKEEPER_SERVICE_HOST and NIMBUS_SERVICE_HOST
```
kubectl exec -it nimbus -c nimbus sh
/configure.sh 10.52.6.228 10.52.14.175
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
kafkacat -b 34.69.208.152:9092 -t admintome-test
kafkacat -L -b 34.69.208.152:9092

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

## DEBUG cluster
1. delete kafka topic

kubectl exec -it kafka-broker0-749594ccf5-n8654  -c kafka bash
// then delete the topic, it will be recreated automatically soon

/opt/kafka/bin/kafka-topics.sh --zookeeper 10.52.6.228:2181 --delete --topic recommender-clickstream

2. clear the cassandar database
export CQLSH_NO_BUNDLED=true

cqlsh 
DESCRIBE keyspaces;
USE product_recommender;
DESCRIBE tables;

TRUNCATE trendingitemcounts;
TRUNCATE users;
TRUNCATE userviews;
TRUNCATE itemcounts;
TRUNCATE similaritiesindex;
TRUNCATE stats;
TRUNCATE bestsellers;
TRUNCATE bestsellersindex;
TRUNCATE paircounts;
TRUNCATE similarities;

select * from trendingitemcounts;
select * from users;
select * from userviews;
select * from itemcounts;
select * from similaritiesindex;
select * from stats;
select * from bestsellers;
select * from bestsellersindex;
select * from paircounts;
select * from similarities;

3. check messages under a topic

kafkacat -b localhost:9092 -t recommender-clickstream


## Install everything on a VM
docker build -t localTag .
docker tag imageId  tjlian616/imagename:imagetag
docker push tjlian616/imagename:imagetag


docker for zookeeper
debian
apt-get install zookeeper

ENTRYPOINT ["/usr/share/zookeeper/bin/zkServer.sh"]
CMD ["start-foreground"]

Install Java ( for kafka)
- sudo apt update
- Sudo apt install default-jre
- Sudo apt install default-jdk
- sudo update-alternatives --config javac
- sudo nano /etc/environment to set JAVA_HOME=“”
https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-on-debian-9#installing-the-default-jrejdk



Install Kafka
https://www.digitalocean.com/community/tutorials/how-to-install-apache-kafka-on-debian-9
- Create a new user kafka (kafka)
- Download kafka tar to user’s directory
- Create  /etc/systemd/system/zookeeper.service and  /etc/systemd/system/kafka.service
- sudo systemctl start kafka (now listening on 9092)
- sudo systemctl enable kafka (reboot restart enabled)
- 

Install Cassandra
http://cassandra.apache.org/doc/latest/getting_started/installing.html
https://stackoverflow.com/questions/38616858/cqlsh-connection-error-ref-does-not-take-keyword-arguments
sudo apt-get install cassandra
sudo service cassandra status
sudo nodetool status
https://stackoverflow.com/questions/16783725/error-while-connecting-to-cassandra-using-java-driver-for-apache-cassandra-1-0-f
https://stackoverflow.com/questions/38616858/cqlsh-connection-error-ref-does-not-take-keyword-arguments


Install Storm
- Create user storm in the same way as of Kafka tutorial
- Download and modify storm configuration as this tutorial https://www.tutorialspoint.com/apache_storm/apache_storm_installation.htm
- Create unit service file in the same way of Kafka tutorial and also look at this https://github.com/salt-formulas/salt-formula-storm/tree/master/storm/files/systemd
- Also learn from here and test the installation with it  https://knowm.org/how-to-install-a-distributed-apache-storm-cluster/

Install Docker
https://linuxize.com/post/how-to-install-and-use-docker-on-debian-9/

Install  sbt
https://www.scala-sbt.org/release/docs/sbt-by-example.html


Read More about how to docker everything
https://endocode.com/blog/2015/05/06/building-a-stream-processing-pipeline-with-kafka-storm-and-cassandra-part-3-using-coreos/

Error on java libraries
https://stackoverflow.com/questions/21894128/how-to-install-sigar-on-ubuntu-based-linux

// submit topology
/home/storm/storm/bin/storm jar /home/willapollobox_gmail_com/real-time-recommender/target/recommender-processor-assembly-1.0.jar storm.Topology

// show all topology
/home/kafka/kafka/bin/kafka-topics.sh --zookeeper localhost:2181 --list

//create a topic
/kafka-topics.sh --zookeeper localhost:2181 --create --topic test1 --partitions 1 --replication-factor 1

// delete
/home/kafka/kafka/bin/kafka-topics.sh --zookeeper localhost:2181 --delete --topic recommender-clickstream


// read current stream from kafka
kafkacat -b localhost:9092 -t recommender-clickstream

// increase storm java memory
https://stackoverflow.com/questions/50443737/increasing-assigned-memory-for-a-topology-in-storm
https://www.freecodecamp.org/news/apache-storm-is-awesome-this-is-why-you-should-be-using-it-d7c37519a427/
https://storm.apache.org/releases/2.0.0/Understanding-the-parallelism-of-a-Storm-topology.html


## Reset a VM setup

how to reset a recommender VM?

// stop storm
sudo systemctl stop storm-ui &&
sudo systemctl stop storm-supervisor &&
sudo systemctl stop  storm-nimbus

// stop webServer
ps aux | grep WebServer
sudo kill +9 xxx

// delete topic
kafka/bin/kafka-topics.sh --zookeeper localhost:2181 --list
kafka/bin/kafka-topics.sh --zookeeper localhost:2181 --delete --topic recommender-clickstream

// stop kafka
sudo systemctl stop kafka

// delete tables in cassandar
export CQLSH_NO_BUNDLED=true
cqlsh //then  see DEPLOYMENT.md 

//git pull real-time-recommender
nano src/main/scala/config/Config.scala
nano src/main/resources/kafka-0.10.0.1-producer-defaults.properties
sbt assembly
sbt 'set test in assembly := {}' assembly

// start kafka
sudo systemctl start kafka

// start webserver
java -cp ~/real-time-recommender/target/recommender-processor-assembly-1.0.jar WebServer
// send one fake event to trigger kafka topic create
curl -X POST \
  localhost:8090/learn \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "some_user",
     "itemId": "some_item",
     "action": "click",
     "timestamp": 1482744482113,
     "price": 1
}'

// start storm
sudo systemctl start storm-nimbus && 
sudo systemctl start storm-supervisor && 
sudo systemctl start storm-ui
// submit storm topology
storm/bin/storm jar  real-time-recommender/target/recommender-processor-assembly-1.0.jar storm.Topology

// use juypter to send events


kafka/bin/zookeeper-shell.sh  localhost:2181
ls /brokers/ids
ls /brokers/topics
rmr /storm
rmr /recommender-clickstream
rmr /brokers/topics


## view the current offset of storm topology
//https://github.com/nathanmarz/storm-contrib/tree/master/storm-kafka

1. get into zookeeper
kafka/bin/zookeeper-shell.sh  localhost:2181
2. list topology details
get /recommender-clickstream/kafka-recommender-clickstream/partition_0

also you can use KafkaTool (app on mac) and its zookeeper viewer tool