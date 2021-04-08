# Deployment of the semantic reasoner API and graph-db
This repository contains the TOSCA/Ansible deployment blueprint for the deployment of the graph-db and semantic-resaoner-api on an openstack VM instance. The deployment is done as a step in jenkins CI/CD using SODALITE orchestrator [xOpera](https://github.com/xlab-si/xopera-opera). 

NOTE: SODALITE currently uses the version [xOpera version 0.6.4](https://pypi.org/project/opera/0.6.4/) since xOpera is being developed to support [OASIS TOSCA Simple Profile in YAML version 1.3](https://www.oasis-open.org/news/announcements/tosca-simple-profile-in-yaml-v1-3-oasis-standard-published).

## Manual install and deploy

```shell script
python3 -m venv venv
. venv/bin/activate
python3 -m pip install --upgrade pip
python3 -m pip install opera[openstack]==0.6.4 docker
ansible-galaxy install -r openstack-blueprint/requirements.yml --force
git clone -b 3.4.1 https://github.com/SODALITE-EU/iac-modules.git openstack-blueprint/modules/
envsubst < openstack-blueprint/input.yaml.tmpl > openstack-blueprint/input.yaml
cd openstack-blueprint
opera deploy service.yaml -i input.yaml               
```