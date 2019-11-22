# Lab Environment Guide

The lab environment we will use is docker-compose.  The main configuration file for docker-compose is `
docker-compose.yml`. To get help at any time with docker-compose, type:


```
docker-compose --help
```

To run a single, ad-hoc container, use the docker-compose "run" command. For example to create a new container with the Jet software and log on to it interactively, use the following commands:

```
docker-compose run jet-server sh
```

In the command above, "jet-server" is the service name, as defined in `docker-compose.yml`.  In the container you just started, verify that java is already installed and on the class path.  Also note that Hazelcast Jet has been installed in the `/opt` directory. Also note that the whole project directory has been mounted under `/opt/project`.   For example, the `/opt/project/hazelcast-config` directory contains configuration files which are visible to all containers.

```bash
/opt/hazelcast-jet # java -version
openjdk version "1.8.0_201"
OpenJDK Runtime Environment (IcedTea 3.11.0) (Alpine 8.201.08-r1)
OpenJDK 64-Bit Server VM (build 25.201-b08, mixed mode)

/opt/hazelcast-jet # ls /opt
hazelcast-jet  project

/opt/hazelcast-jet # ls /opt/project
00_lab_environment_walkthru.md  UI                              docker-compose.yml              hazelcast-client                hazelcast-server
README.MD                       data                            docker-images                   hazelcast-config                pom.xml

/opt/hazelcast-jet # ls /opt/project/hazelcast-config/
hazelcast-client.xml  hazelcast-jet.xml     hazelcast.xml

/opt/hazelcast-jet # exit
```

Note that typing exit will stop the container.  Later we will see how to keep it running.

