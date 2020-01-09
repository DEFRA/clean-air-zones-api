# Test-Lambda
Test-Lambda - sample raw Java Lambda for various testing purposes.
This project is not part of CAZ Common modules but standalone project. Built it separately.

## First steps in Test-Lambda

### Configuring code style formatter
There are style guides for _Eclipse_ and _Intellij IDEA_ located in `developer-resources`.
It is mandatory to import them and format code to match this configuration. Check Eclipse or IDEA
documentation for details how to set this up and format code that you work on.

### Adding and configuring Lombok
What is [Lombok](https://projectlombok.org/)?

*Project Lombok is a java library that automatically plugs into your editor and build tools, spicing up your java.
Never write another getter or equals method again, with one annotation your class has a fully featured builder, Automate your logging variables, and much more.*

Lombok needs to be installed into Maven build process and into _Eclipse_ and _Intellij IDEA_.
1. Lombok and Maven - this is already configured in _pom.xml_ - nothing more to do.
2. Eclipse - follow up this [official tutorial](https://projectlombok.org/setup/eclipse) to install into Eclipse.
2. IDEA - follow up this [official tutorial](https://projectlombok.org/setup/intellij) to install into IDEA.

For more details about what Lombok can do see this [feature list](https://projectlombok.org/features/all).

### Commit hooks

To minimize the risk of making a _broken_ commit you may want to enable a git pre-commit hook which 
builds the project before a change is committed. Please execute the following in the root project 
directory:
```
$ developer-resources/scripts/git-hooks/install-pre-commit-hook.sh
```
This will create a symlink to `developer-resources/scripts/git-hooks/pre-commit-hook.sh`. If 
the symlink exists or there is another `pre-commit` file in `.git/hooks` directory, the script does 
nothing and appropriate error message is displayed.

If you want to disable the hook please use `--no-verify` option for `git commit`.


## Local Development: building, running and testing

## Building
Build and package Lambda application, run all checks and tests, calculate code coverage:
```
$ make clean build
```
If you want to only do local runs you can skip creating Lambda zip file:
```
$ make compile
```
Build the project without unit and integration tests: 
```
$ make build-yolo
```

## Testing

### Automated tests

#### Run whole harness
Run all automated tests:
```
make build
```

#### Unit tests
Run unit tests: 
```
$ make unit-test
``` 
*NOTE*: the project is only compiled

### Manual testing: Alternative 1: Local run as Spring-Boot app with as many AWS services as possible. Recommended.
This is the recommended way to run and test service locally. It is most convenient to use, fastest to 
spin and suitable for 99% cases. The only missing 1% can be tested using Alternative 2 described below but
it should be very rare.
 
#### Prerequisities
You need to have installed:
1. Java 8 runtime
2. Docker (optional for example for Postgres)
3. make utility (comes as standard on Linux and Mac)

You need to have AWS account with at least programmatic access. And:
1. Make sure you have proper AWS config entry in file in ~/.aws/credentials - there needs to be a profile with your keys.
2. You need to have environment variable set: export AWS_PROFILE=name_of_profile 
3. You need to have environment variable set: export AWS_REGION=eu-west-2 (you can choose whatever region works for you)

#### Building
1. Pull the branch you want from GitHub repo of this project.
2. Run `make clean` to make sure that everything is clean and ready to build.
3. Run `make build` to build and test project locally - it may take some time as there are a lot of tests including integration ones.

#### Run service locally
If you want to run application locally, as regular Spring-Boot application you need to specify
Spring profile `development`. In this profile service will not use any AWS Lambda related parts or
switch them to the ones suitable for such local run. However in this mode service will still
use all other AWS services that are publicly available, for example S3. 
For it to work you need to have correctly configured AWS account and access with `~/.aws/credentials` 
file and AWS profile specified in `AWS_PROFILE` env variable and AWS Region specified in `AWS_REGION`. 
And now run:
```
$ make run-development
```

Now you can call Spring-Boot endpoints as usual using REST client for example curl or Postman.

### Manual testing: Alternative 2: AWS Lambda Integration type run and test
This mode is suitable if we want to test AWS Lambda to Spring-Boot plumbing code. See
`StreamLambdaHandler` class to see how this plumbing looks like.
For any other functionality Alternative 1 is recommended.

In this mode we will run and test our Lambda code in simulated local Lambda runner. 
It can be achieved with support of AWS SAM Local CLI tool. In short, this tool uses Docker to
provide similar runtime to the one AWS uses to run our Lambdas on production.
Moreover, in this mode we will try to use as much real AWS services as possible. For example we
will connect to real S3 buckets and use real SQS and SNS services.

#### Prerequisities
1. Have AWS CLI tool installed. See official [AWS Guide](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html)
2. Have AWS SAM Local CLI tool installed. See official [AWS Guide](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
3. Make sure you have properly configured AWS credentials and profile in `~/.aws/credentials`. You must have your profile added with access key id and secret access key.
4. Make sure you have **AWS_PROFILE** and **AWS_REGION** environment variables set and exported (and matching your profile from credentials file). SAM Local will pass those credentials automatically to Docker with Lambda runtime.

#### Running and testing 
1. (Optional, not supported now) Run `make local-db-up` to spin up local Postgres Docker container. It will automatically set proper network and name.
2. Rebuild and repackage your project: `make build`.  You will have to do it every time you make any changes.
3. Run `make sam-local-run EVENT=path_to_json_file_with_lambda_event` - this uses SAM Local tool to run specified Lambda function locally and passing input JSON event into it.
Beware as first run will download Docker images which can take a while.
4. In case of any changes just repackage as described in point 2 and then rerun as in 3.
5. (Optional, if point 1 in effect) After you're done, drop local Postgres by `make local-db-down` and you are in a clean state.
6. Final note: JSON file with Lambda event is quite complicated and is something that API Gateway sends to Lambda. It must 
contain all HTTP request details, optionally security context as well.


## Features

### AWS Cognito stuff

There are 2 endpoints that allow to test AWS Cognito:
1. POST /cognito/users to create new user in Cognito
2. POST /cognito/users/login to login user using Cognito

Please check dto/NewCognitoUserDto and dto/LoginUserDto for required json payload for these endpoints.

To test Cognito first you need to have proper AWS account and aws-cli with AWS credentials configured locally. See above instructions for more details.
In AWS Console -> Cognito create User Pool and in this pool create new App Client with autogenerated client secret.
In newly created App Client enable checkbox in "Enable username password auth for admin APIs for authentication (ALLOW_ADMIN_USER_PASSWORD_AUTH)".
In "App integration -> App client settings" enable "Client credentials" in "Allowed OAuth flows".

Grab "App client id" and "App client secret" from newly created app client. Grab "Pool id" from "General settings" of newly created User Pool.

#### Deploying Lambda
1. Create S3 bucket with some name.
2. Run `make clean build` for fresh build.
3. Run `make deploy-to-aws S3_BUCKET_NAME=<your_bucket> STACK_NAME=<your_desired_stack_name>` - it will take some time but ultimately it will deploy Lambda and 
create API Gateway resource. In the output you will get final endpoint, for example: "OutputValue": "https://random_xyz.execute-api.eu-west-2.amazonaws.com/Prod/".
This will be your root url.

#### Running locally
1. Run `make clean build` for fresh build.
2. Run `make run-development` to run Spring boot app. Your root url will be `http://localhost:8080`

#### Creating new user
1. You need to have root url, either on API Gateway or localhost. See above paragraphs for details.
2. Run: `curl -i -d '{"userPoolId": "your_pool_id", "userName": "your_username", "email": "your_user@mail.com", "password": "your_secret_password"}' -H "Content-Type: application/json" http(s)://root_url/cognito/users`
but replace `your_pool_id` with grabbed User Pool Id, `your_username`, `your_user@mail.com` and `your_secret_password` with desired values for new user. Replace `root_url` with target URL, either localhost or API Gateway.
3. Check Lambda or console logs for details. Check AWS Cognito User Pool - it should have newly created user.

#### Logging in
1. You need to have root url, either on API Gateway or localhost. See above paragraphs for details.
2. Run: `curl -i -d '{"userPoolId": "your_pool_id", "userName": "your_username", "clientId": "app_client_id", "clientSecret": "app_client_secret", "password": "your_secret_password"}' -H "Content-Type: application/json" http(s)://root_url/cognito/users/login`
but replace `your_pool_id` with grabbed User Pool Id, `your_username` and `your_secret_password` with user credentials. Put app client id and app client secret. Replace `root_url` with target URL, either localhost or API Gateway.
3. Check Lambda or console logs for details.
