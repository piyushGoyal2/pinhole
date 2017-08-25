#!/bin/bash
#set -x #echo on
function pullApp() {
   echo "Pulling Lastest" $1
   cd ../$1 &&  git pull
}

function buildApp() {
   echo "Building Lastest" $1
   cd ../$1 &&  mvn clean install
}

function usage {
  echo "Usage: bash $0	[OPTIONS]
  
Handy script to run microservices locally.
Options:
  pull               to git pull
  build              to run mvn clean install in every service
  docker-run         to run docker-compose , this builds the docker images before starting the containers
  docker-kill        to kill all running docker containers"
  exit 1
}

if [ $# -eq 0 ]; then
    usage
fi

for arg do
	echo $arg
	if [[ $arg == "pull" ]]; then
		pullApp service-parent
		pullApp service-common
		pullApp user-db-service
		pullApp registration-service
		pullApp notification-service
	fi

	if [[ $arg == "build" ]]; then
		buildApp service-parent
		buildApp service-common
		buildApp user-db-service
		buildApp registration-service
		buildApp notification-service
	fi

	if [[ $arg == "docker-run" ]]; then
		echo "docker-compose up --build -p pinhole"
		docker-compose -p pinhole up --build 
	fi

	if [[ $arg == "docker-kill" ]]; then
		echo "docker rm  -f $(docker ps -a -q)  "
		docker rm  -f $(docker ps -a -q)  
	fi
done
