# Deployment of the semantic reasoner API and graph-db
This repository contains the TOSCA/Ansible deployment blueprint for the deployment of the graph-db and semantic-resaoner-api on an openstack VM instance. The deployment is done as a step in jenkins CI/CD using SODALITE orchestrator [xOpera](https://github.com/xlab-si/xopera-opera). 

NOTE: SODALITE currently uses the version [xOpera version 0.5.7](https://pypi.org/project/opera/0.5.7/) since xOpera is beeing developed to support [OASIS TOSCA Simple Profile in YAML version 1.3](https://www.oasis-open.org/news/announcements/tosca-simple-profile-in-yaml-v1-3-oasis-standard-published).