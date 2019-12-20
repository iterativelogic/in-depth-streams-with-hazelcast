# Overview
This

# Table of Contents

All configuration files, including hazelcast client and server config and logging config are in `config`.



# Setup

This reproducer uses docker compose.  You will need to have Docker Desktop installed.

1. Create file in the project root directory called `.env`.  Edit the file to add your Jet license following the
   format shown below.
   
   ```
   JET_LICENSE_KEY=MY-JET-LICENSE
```
   
2. Build the project: `mvn clean install`

# Verify The Setup

1. Review the 