# Overview

The In-Depth Stream Processing Course is a 3 day hands-on experience intended to get Java developers ready to use the Hazelcast platform to build solutions that process and react to streaming data in real time.   The lab emphasized how IMDG and Jet features can be used together to deliver insights and actions on fast moving data.

The lab is built around a hypothetical truck rental company, UMove. All trucks in the UMove fleet are equipped with GPS and engine monitoring devices which periodically transmit location and other information over cell networks. The student will be designing solutions that help UMove turn the continuous stream of data produced by their fleet into meaningful insights, and to act on those insights in near real time.

All of the documentation for the labs is in the [doc](docs) directory.

This repository contains a "master" branch, which contains the "starters" for all of the labs and a "solutions" branch with solutions.  It is generally possible to apply the solution to a single lab to your working copy with a command similar to the following: `git revert starter_lab03`.

# Prerequisites

To do these labs you will need a laptop with the following:

- docker-compose installed
- Maven 
- A Java IDE (IntelliJ is strongly recommended)
- An internet connection (for downloading dependencies and docker images).

# Outline and Status

1. Environment Guide: DONE

   Familiarizes the student with the docker-compose environment.

2. Configuring and Starting: DONE

   Covers starting Jet with the built-in scripts, plain java code with declarative or programmatic configuration, and Spring Boot.  Also covers how to configure the underlying IMDG

3. Ingest: DONE

   The student must write a pipeline to ingest GPS pings from 2 sources, transform them into HazelcastJSON instances and sink them to an IMAP.  Requires writing a custom source that supports snapshots and the use of "mapUsingContext".

4. Operating: DONE

   In this lab the student will make the shift to packaging  Jet pipelines in a jar and deploying to an existing cluster. The student will also configure and test fault tolerance, and scale  a pipeline with stateful processors up and down.  Hot restart will be configured and exercised.  The student will learn how to update an existing pipeline by adding engine diagnostic codes to the ingest pipeline.

5. Automated Crash Detection and Response: DONE

   A new pipeline will be introduce.  It will monitor all pings for certain engine codes that denote a serious crash.  The pipeline will consist of an IMap source and a simple filter on engine codes.  When a crash is detected, the IMDG aggregation API will be used to rapidly locate the closest city so that help can be dispatched.

6. Maintenance Alerts: DONE

   A new pipeline will be developed to support the delivery of make/model specific maintenance alerts to the fleet's operators.  The hash-join operation will be introduced.

7. Fleet Monitoring: DONE

   The vehicle monitor pipeline will be extended to continuously calculate speed and miles driven.  This will involve a custom aggregator.  An entry processor will be used on the speed category map sink to avoid duplicate updates.  Now UMove will be able to see who is speeding in real time!

8. Ad Hoc Queries: DONE

   This lab explores Ad-Hoc Queries

9. Mileage Limits: NOT STARTED

   Vehicles in a certain category have daily mileage limits.  The student will use what they have learned to implement this feature.  

10. Geofencing: NOT STARTED

    The student will use bounded (batch) streams to produce a report of vehicles that need maintenance.