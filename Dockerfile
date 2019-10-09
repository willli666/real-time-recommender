FROM openjdk:14-slim

RUN apt -y update
RUN apt -y upgrade
RUN apt install -y procps
RUN apt install -y lsof
RUN apt install -y curl
COPY target/recommender-processor-assembly-1.0.jar /opt/
WORKDIR /opt
EXPOSE 8090
CMD ["java", "-cp", "recommender-processor-assembly-1.0.jar", "WebServer"]