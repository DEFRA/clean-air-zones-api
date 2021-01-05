# Cognito user migration script

The goal of this script is to set `sub` as the `preferred_username` attribute 
for all users of a given user pool.

## Building & running

You can run the script in two ways: either installing needed dependencies
locally or using a docker container.

### Using docker
* set `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` environment variables.
You can use the following method to read them from stdin:
```
$ read -rsp "Enter AWS_ACCESS_KEY_ID:" AWS_ACCESS_KEY_ID 
$ read -rsp "Enter AWS_SECRET_ACCESS_KEY:" AWS_SECRET_ACCESS_KEY
$ export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
$ export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
```
* run `make build` to build an image with the script
* run the container and provide the user pool identifier, for example:
```
./run.sh eu-west-2_OCwA2zqoX
```
* remove image and container by executing `make cleanup`

### Using local environment
* install `node` and `yarn`
* execute `yarn install` in the script's root directory
* set AWS credentials and region in the environment variables (https://docs.aws.amazon.com/sdk-for-javascript/v2/developer-guide/loading-node-credentials-environment.html)
* run the script by providing the user pool identifier as the only parameter, for example:
```
$ node index.js eu-west-2_OCwA2zqoV
``` 
