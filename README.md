# semantic-reasoner

## Description

The repository contains the Maven modules for the Semantic Reasoner component of SODALITE. More specifically:

- `semantic-reasoner`: parent module (pom)  that encapsulates three sub-modules
    1. `reasoner-api`: The reasoner's REST API (war)
    2. `reasoning-engine`: The reasoning infrastructure and application API (jar)
    3.  `tosca-mapper `: The mapper from tosca to exchange format

## Prerequisites
This module depends on:

- The SODALITE sub-project defect-prediction. Thus, first built it.
The information about building defect predictor can be found at
 ` https://github.com/SODALITE-EU/defect-prediction `
- The graph-db which can be downloaded from [here](http://graphdb.ontotext.com/documentation/free/index.html). 
After started, you can access it here http://localhost:7200/.
```
 - Java 11 or newer
 - Graph db 9.3.0 or newer
 - Docker engine 19.03 or newer 
 ```
 
## Installation (Maven)

- cd to `semantic-reasoner` folder
- run: `mvn install`
- Deploy the war file in a web container (e.g. Tomcat)

## Running the reasoner on a local environment
1) Download the [defect predictor](https://github.com/SODALITE-EU/defect-prediction), and build it as a maven project.
Extract the bug-predictor.war to the tomcat webapps folder.
2) Download graph-db from [here](http://graphdb.ontotext.com/). 
After graph-db is up and running, create a TOSCA repository with ruleset = owl-2rl and disable owl-sameas.
Load the ontologies from [semantic-models](https://github.com/SODALITE-EU/semantic-models/tree/master/ontology%20definitions).
Specifically, load the ontologies under ontology-definitions folder:
 `optimizations.ttl`,  `sodalite-metamodel.ttl `,  `import/DUL.ttl `,  `tosca-builtins.ttl `
4) Build the semantic reasoner as a maven project. Run it on a tomcat server. 
The reasoner should be up and running, waiting for receiving API requests.

## Running the reasoner on docker containers
Run 
```docker-compose up```

You can access the graph database [here](http://localhost:7200/)
and send requests to the reasoner http://localhost:8080/reasoner-api/v0.6/<service_name>
##### For building docker images separately:

> maven install
>docker build -t semantic-reasoner -f docker/web/Dockerfile .
> docker build -t graphdb -f docker/graph-db/Dockerfile .

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

Send a POST request with the following parameters as x-www-form-urlencoded to the above url:

**rmTTL:**
<details>
<summary>Resource model here</summary>

```
# baseURI: https://www.sodalite.eu/ontologies/exchange/rm/
# imports: https://www.sodalite.eu/ontologies/exchange/

@prefix : <https://www.sodalite.eu/ontologies/exchange/rm/> .
@prefix exchange: <https://www.sodalite.eu/ontologies/exchange/> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

:
  rdf:type owl:Ontology ;
  owl:imports exchange: ;
  owl:versionInfo "Created by the SODALITE IDE" ;
.

:RM_1
  rdf:type exchange:RM ;
  exchange:userId "27827d44-0f6c-11ea-8d71-362b9e155667" ;
.
:Parameter_1
  rdf:type exchange:Parameter ;
  exchange:name "property" ;  
  exchange:value 'docker_ip' ; 
.
:Parameter_2
  rdf:type exchange:Parameter ;
  exchange:name "entity" ;  
  exchange:value 'SELF' ; 
.
:Parameter_3
  rdf:type exchange:Parameter ;
  exchange:name "get_property" ;
  exchange:hasParameter :Parameter_1 ;
  exchange:hasParameter :Parameter_2 ;
.	

:Parameter_4
  rdf:type exchange:Parameter ;
  exchange:name "value" ;
  exchange:hasParameter :Parameter_3 ;
.

:Parameter_5
  rdf:type exchange:Parameter ;
  exchange:name "docker_ip" ;
  exchange:hasParameter :Parameter_4 ;
.	


:Parameter_6
  rdf:type exchange:Parameter ;
  exchange:name "path" ;
  exchange:value '/workspace/iac-management/blueprint-samples/blueprints/sodalite-test/modules/vm/playbooks/set_ip.yaml' ;
.

:Parameter_7
  rdf:type exchange:Parameter ;
  exchange:name "content" ;
  exchange:value '- hosts: all\n  gather_facts: no\n  tasks:\n    - name: Set attributes\n      set_stats:\n        data:\n          private_address: "{{ docker_ip }}"\n          public_address: "{{ docker_ip }}"' ;
.


:Parameter_8
  rdf:type exchange:Parameter ;
  exchange:name "primary" ;
  exchange:hasParameter :Parameter_6 ;
  exchange:hasParameter :Parameter_7 ;
.


:Parameter_9
  rdf:type exchange:Parameter ;
  exchange:name "implementation" ;
  exchange:hasParameter :Parameter_8 ;
.

:Parameter_10
  rdf:type exchange:Parameter ;
  exchange:name "create" ;
  exchange:hasParameter :Parameter_5 ;
  exchange:hasParameter :Parameter_9 ;
.

:Parameter_11
  rdf:type exchange:Parameter ;
  exchange:name "type" ;
  exchange:value 'string' ;  
.

:Parameter_12
  rdf:type exchange:Parameter ;
  exchange:name "required" ;
  exchange:value 'false' ;
.
:Property_1
  rdf:type exchange:Property ;
  exchange:name "username" ;
  exchange:hasParameter :Parameter_11 ;
  exchange:hasParameter :Parameter_12 ;
.
:Parameter_13
  rdf:type exchange:Parameter ;
  exchange:name "type" ;
  exchange:value 'string' ;  
.
:Parameter_14
  rdf:type exchange:Parameter ;
  exchange:name "required" ;
  exchange:value 'false' ;
.
:Property_2
  rdf:type exchange:Property ;
  exchange:name "docker_ip" ;
  exchange:hasParameter :Parameter_13 ;
  exchange:hasParameter :Parameter_14 ;
.
:Parameter_15
  rdf:type exchange:Parameter ;
  exchange:name "type" ;
  exchange:value 'tosca.interfaces.node.lifecycle.Standard' ;
.
:Interface_1
  rdf:type exchange:Interface ;
  exchange:name "Standard" ;
  exchange:hasParameter :Parameter_15 ;
  exchange:hasParameter :Parameter_10 ;
.

:NodeType_1
  rdf:type exchange:Type ;
  exchange:name "sodalite.nodes.Compute" ;
  exchange:derivesFrom 'tosca.nodes.Compute' ;  
  exchange:properties :Property_1 ; 
  exchange:properties :Property_2 ; 
  exchange:interfaces :Interface_1 ; 
.  
```

</details>


**rmURI:** <LEAVE IT EMPTY>


For the sake of this testing, leave rmURI empty.
When rmURI is empty, a new resource model is created. Otherwise, the resource model with rmURI is overriden.

**namespace:** openstack

The namespace on which the model will be saved. If no namespace given, the model is saved in the global namespace.

**name**: test.rm

The file name of the model


If success, you 'll get an rmURI as response.

**Successful Response**
```
{
    "rmuri": "https://www.sodalite.eu/ontologies/workspace/1/q03i5hhp8oac5ogftbmgr7ra4v/RM_nom7pmrlja496e5kkb026ub7d8"
}
```
## Sample scenarios:
### Save an aadm model
```
http://<server_ip>:8080/reasoner-api/v0.6/saveAADM
```

Send a POST request with the following parameters as x-www-form-urlencoded to the above url:

**aadmTTL:**
<details>
<summary>Resource model here</summary>

```
# baseURI: https://www.sodalite.eu/ontologies/exchange/sodalite-test/
# imports: https://www.sodalite.eu/ontologies/exchange/

@prefix : <https://www.sodalite.eu/ontologies/exchange/sodalite-test/> .
@prefix exchange: <https://www.sodalite.eu/ontologies/exchange/> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

:
  rdf:type owl:Ontology ;
  owl:imports exchange: ;
  owl:versionInfo "Created by the SODALITE IDE" ;
.

:AADM_1
  rdf:type exchange:AADM ;
  exchange:userId "27827d44-0f6c-11ea-8d71-362b9e155667" ;
.
:Property_1
  rdf:type exchange:Property ;
  exchange:name "username" ;
  exchange:value "1" ;
.
:Property_2
  rdf:type exchange:Property ;
  exchange:name "docker_ip" ;
  exchange:value "1" ;
.

:Template_1
  rdf:type exchange:Template ;
  exchange:name "sodalite-vm" ;
  exchange:type 'openstack/sodalite.nodes.Compute' ;  
  
  exchange:properties :Property_1 ;
  exchange:properties :Property_2 ;
.  
```

</details>


**aadmURI:** <LEAVE IT EMPTY>


For the sake of this testing, leave rmURI empty.
When rmURI is empty, a new resource model is created. Otherwise, the resource model with rmURI is overriden.

**namespace:** test

The namespace on which the model will be saved. If no namespace given, the model is saved in the global namespace.

**name**: test.aadm

The file name of the model


If success, you 'll get an aadmURI as response.

**Successful Response**
```
{
    "aadmuri": "https://www.sodalite.eu/ontologies/workspace/1/q03i5hhp8oac5ogftbmgr7ra4v/AADM_nom7pmrlja496e5kkb026ub7d8"
}
```


 