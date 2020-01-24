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