#!/bin/sh
echo y | fly -t home sp -p home-debt -c pipeline.yml -l ../../credentials.yml
