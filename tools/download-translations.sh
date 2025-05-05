#!/bin/bash

CURRENT_DIR=$(pwd)

cd ..

export CROWDIN_PROJECT_ID=$(grep "crowdin.project_id" local.properties | cut -d'=' -f2)
export CROWDIN_API_TOKEN=$(grep "crowdin.api_token" local.properties | cut -d'=' -f2)

crowdin download translations --token "$CROWDIN_API_TOKEN" --project-id "$CROWDIN_PROJECT_ID"

cd "$CURRENT_DIR"