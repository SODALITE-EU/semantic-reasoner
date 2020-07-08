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
A sample scenario for checking that reasoner communicates successfully with knowledge base (graph-db) and the bug predictor (tosca-smells)

### Save a resource model:
```
http://<server_ip>:8080/reasoner-api/v0.6/saveRM
```

Send a POST request with two parameters as x-www-form-urlencoded to the above url:

**rmTTL:**
```turtle
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
  exchange:name "type" ;
  exchange:value 'string' ;
.
:Parameter_2
  rdf:type exchange:Parameter ;
  exchange:name "required" ;
  exchange:value 'true' ;
.
:Parameter_3
  rdf:type exchange:Parameter ;
  exchange:name "default" ;
  exchange:value 'tcp' ;	  
.
:Parameter_4
  rdf:type exchange:Parameter ;
  exchange:name "constraints" ;
  exchange:hasParameter :Parameter_5 ;
.
:Parameter_5
  rdf:type exchange:Parameter ;
  exchange:name "valid_values" ;
  exchange:listValue "udp" ;
  exchange:listValue "tcp" ;
  exchange:listValue "icmp" ;
.	
:Property_1
  rdf:type exchange:Property ;
  exchange:name "protocol" ;
  exchange:hasParameter :Parameter_1 ;
  exchange:hasParameter :Parameter_2 ;
  exchange:hasParameter :Parameter_3 ;
  exchange:hasParameter :Parameter_4 ;
.
:DataType_1
  rdf:type exchange:DataType ;
  exchange:name "sodalite.types.OpenStack.SecurityRule" ;
  exchange:derivesFrom "tosca.datatypes.Root" ;
  exchange:properties :Property_1 ; 
  exchange:properties :Property_2 ; 
  exchange:properties :Property_3 ; 
  exchange:properties :Property_4 ; 
.
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
