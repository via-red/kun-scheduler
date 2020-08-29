#!/usr/bin/env sh
dockerize -wait http://${WORKFLOW_HOST}:${WORKFLOW_PORT}/health -wait-retry-interval 3s -timeout 60s
java ${JVM_OPTS} -jar /server/target/kun-metadata-web-1.0.jar
