# Overview

The purpose of this project is to accelerate the set up of Hazelcast projects.   It is intended to be used
as a starting point. It consists of a 2 member Hazelcast 3.12 cluster , a Hazelcast client and Hazelcast
Management Center, deployed together using docker-compose.

The Maven groupId and artifactId can be customized, as well as the base
java package name.

To create a project using this template, install [tplate](https://pypi.org/project/tplate) then ...

```
tplate https://github.com/wrmay/hazelcast-starter path/to/my-project-dir
```

The artifact id will be the final part of the second argument you pass ("my-project-dir" in the example).
You will be prompted for the maven group id and the java package name.


# Contents

`docker-images`

Contains a Dockerfile for Hazelcast Management Center 3.12 built on top of Alpine Linux.

`hazelcast-client`

Source code and ".pom" file for a simple Hazelcast client.

`hazelcast-config`

Hazelcast runtime configuration files used by docker-compose deployment.

`hazelcast-server`

Source code and ".pom" file for the Hazelcast server.

`docker-compose.yml`

The main docker-compose configuration file.

# Usage

Prerequisites: Maven and Docker Desktop

Build it: `mvn install`

Start it: `docker-compose up -d`

View log files: `docker-compose logs --follow`

Stop it: `docker-compose down`

Access Management Center: [http://localhost:8080/hazelcast-mancenter](http://localhost:8080/hazelcast-mancenter)
