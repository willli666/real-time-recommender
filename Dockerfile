FROM openjdk:8-stretch

RUN apt-get update -y
RUN apt-get upgrade -y
RUN apt-get install procps lsof curl apt-transport-https git -y

COPY . /opt/real-time-recommender/
RUN  cd /opt/real-time-recommender/ &&\
       sbt 'set test in assembly := {}' assembly &&\
       cd ..

WORKDIR /opt/real-time-recommender/

EXPOSE 8090

CMD ["java", "-cp", "target/recommender-processor-assembly-1.0.jar", "WebServer"]