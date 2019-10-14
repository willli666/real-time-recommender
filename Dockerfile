FROM openjdk:8-stretch

RUN apt-get update -y
RUN apt-get upgrade -y
RUN apt-get install procps lsof curl apt-transport-https git -y

RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt-get update -y
RUN apt-get upgrade -y
RUN apt-get install sbt -y

COPY . /opt/real-time-recommender/
RUN  cd /opt/real-time-recommender/ &&\
       sbt 'set test in assembly := {}' assembly &&\
       cd ..

WORKDIR /opt/real-time-recommender/

EXPOSE 8090

CMD ["java", "-cp", "target/recommender-processor-assembly-1.0.jar", "WebServer"]