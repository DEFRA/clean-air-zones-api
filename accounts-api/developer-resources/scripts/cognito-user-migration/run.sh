#!/usr/bin/env bash

[[ -z "$1" ]] && { echo "Error: Please provide the user pool id as the only argument!"; exit 1; }
[[ -z "$AWS_ACCESS_KEY_ID" ]] && { echo "Error: Please set AWS_ACCESS_KEY_ID env variable"; exit 1; }
[[ -z "$AWS_SECRET_ACCESS_KEY" ]] && { echo "Error: Please set AWS_SECRET_ACCESS_KEY env variable"; exit 1; }

USER_POOL_ID=$1

docker run \
--rm \
-e "AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID" \
-e "AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY" \
-e "AWS_REGION=eu-west-2" \
cognito-user-migration:0.1 node index.js "$USER_POOL_ID"