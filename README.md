# semantic-reasoner

## Description

The repository contains the Maven modules for the Semantic Reasoner component of SODALITE. More specifically:

- `semantic-reasoner`: parent module (pom)  that encapsulates two sub-modules
    1. `reasoner-api`: The reasoner's REST API (war)
    2. `reasoning-engine`: The reasoning infrastructure and application API (jar)

## Prerequisites
This module depends on:

- The SODALITE sub-project “defect-prediction”. Thus, first built it.
The information about building defect predictor can be found at
 ` https://github.com/SODALITE-EU/defect-prediction `
- The graph-db which can be downloaded from [here](http://graphdb.ontotext.com/documentation/free/index.html). 
After started, you can access it here http://localhost:7200/. 
 
## Installation (Maven)

- cd to `semantic-reasoner` folder
- run: `mvn install`
- Deploy the war file in a web container (e.g. Tomcat)

## Running the reasoner on a local environment
1) Set an environment variable with name as 'environment'
and value as dev'.
2) Download the [defect predictor](https://github.com/SODALITE-EU/defect-prediction), and build it as a maven project.
Extract the bug-predictor.war to the tomcat webapps folder.
3) Download graph-db from [here](http://graphdb.ontotext.com/). 
After graph-db is up and running, create a TOSCA repository with ruleset = owl-2rl.
Load the ontologies from [semantic-models](https://github.com/SODALITE-EU/semantic-models/tree/master/ontology%20definitions).
Specifically, load the ontologies under ontology-definitions folder:
optimizations.ttl, sodalite-metamodel.ttl, import/DUL.ttl, tosca-builtins.ttl
4) Build the semantic reasoner as a maven project. Run it on a tomcat server. 
The reasoner should be up and running, waiting for receiving API requests.

## Running the reasoner on docker containers
Run 
```docker-compose up```

You can access the graph database [here](http://localhost:7200/)
and send requests to the reasoner http://localhost:8080/reasoner-api/v0.6/<service_name>

  Prerequisite:
 - Docker engine 19.03 or newer

# REST APIs

## Testing communication of reasoner with its dependent components
A sample scenario for checking that reasoner communicates successfully with knowledge base (graph-db) and the bug predictor (tosca-smell)
Send a GET request with no parameter:
```
http://<hostname>/reasoner-api/v0.6/testReasoner 
```
Cases:
- **Successful scenario**

Reasoner communicates successfully with its dependencies, a 200 code response will be returned
with plain/text message:
>"Successfully connected to both defect predictor and graph-db"

- **Wrong url set**

Defect predictor or graph-db url has not been set.
A 400 status response will be returned with plain/text message:
>"no <defect predictor/graphdb> url set"
- **Error in communication**

Reasoner cannot communicate with the defect predictor or graph-db, a response with 400 status code will be returned:
For defect predictor, the error plain/text message is:
>"error while trying to connect to defect-predictor"

For graph-db, the error plain/text message is:
>"graphdb host is unknown: <graphdb url>"

 

## Sample scenarios:
### Save a resource model
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

**Successful Response**
```
{
    "rmuri": "https://www.sodalite.eu/ontologies/workspace/1/q03i5hhp8oac5ogftbmgr7ra4v/RM_nom7pmrlja496e5kkb026ub7d8"
}
```
