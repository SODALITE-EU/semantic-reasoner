# semantic-reasoner

## Description

The repository contains the Maven modules for the Semantic Reasoner component of SODALITE. More specifically:

- `semantic-reasoner`: parent module (pom)  that encapsulates two sub-modules
    1. `reasoner-api`: The reasoner's REST API (war)
    2. `reasoning-engine`: The reasoning infrastructure and application API (jar)

## Installation (Maven)

- cd to `semantic-reasoner` folder
- run: `mvn install`
- Deploy the war file in a web container (e.g. Tomcat)

## Running the reasoner on docker containers
Run 
```docker-compose up```
You can access the graph database [here](http://localhost:7200/)
and send requests to the reasoner http://localhost:8080/reasoner-api/v0.6/<service_name>
