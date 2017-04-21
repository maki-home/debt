#!/bin/sh

set -e
source /docker-lib.sh
start_docker
# This loading is not nesessary but enalbes caching by Concourse
docker load -i redis/image
docker tag "$(cat redis/image-id)" "$(cat redis/repository):$(cat redis/tag)"
docker images

docker-compose -f repo/docker-compose.yml up -d
sh utils/scripts/wait-until-port-available.sh localhost 6379
sh utils/scripts/add-repos-in-pom-xml.sh repo && \
sh utils/scripts/mvn.sh test repo
docker-compose -f repo/docker-compose.yml down