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

# REST APIs
## Semantic Reasoner

### Testing communication of reasoner with its dependent components
A sample scenario for checking that reasoner communicates successfully with knowledge base (graph-db) and the bug predictor (tosca-smell)
Send a GET request with no parameter:
http://<hostname>/reasoner-api/v0.6/testReasoner 

- Reasoner communicates successfully with its dependencies, a 200 code response will be returned.
- Reasoner cannot communicate with the defect predictor, a response with bad request status code will be returned,
and a message "error while trying to connect to defect-predictor".
- Reasoner cannot communicate with the graph-db, a response with bad request status code will be returned,
and a message "graphdb host is unknown: <graph-db url>".

 

### Save a resource model:
```
http://<server_ip>:8080/reasoner-api/v0.6/saveRM
```

Send a POST request with two parameters as x-www-form-urlencoded to the above url:

**rmTTL:**
```turtle
RM MODEL HERE IN TURTLE FORMAT
```
**rmURI:** <LEAVE IT EMPTY>

For the sake of this testing, leave rmURI empty.
When rmURI is empty, a new resource model is created. Otherwise, the resource model with rmURI is overriden.
If success, you 'll get an rmURI as response.

**Successfull Response**
```
{
    "rmuri": "https://www.sodalite.eu/ontologies/workspace/1/q03i5hhp8oac5ogftbmgr7ra4v/RM_nom7pmrlja496e5kkb026ub7d8"
}
```
