FROM adoptopenjdk/openjdk8:alpine
ADD https://download.hazelcast.com/management-center/hazelcast-management-center-3.12.2.tar.gz /tmp
WORKDIR /opt
RUN tar -xzf /tmp/hazelcast-management-center-3.12.2.tar.gz
CMD java -cp /opt/hazelcast-management-center-3.12.2/hazelcast-mancenter-3.12.2.war Launcher