.PHONY: \
	build \
	build-yolo \
	compile \
	run \
	run-development \
	run-active-licences-in-reporting-window-task \
	run-active-licenses-in-reporting-window-task-java \
	package \
	generate-javadoc \
	clean \
	deploy-to-aws \
	create-archetype \
	local-up \
 	local-down \
 	local-redis-up \
 	local-redis-down \
 	integration-test \
 	unit-test \
 	checkstyle \
 	sonar

build:
	./mvnw verify -P jacoco -U

build-yolo:
	./mvnw verify -DskipTests -U

compile:
	./mvnw compile -U
  
run:
	./mvnw spring-boot:run

run-development:
	SPRING_PROFILES_ACTIVE=development ./mvnw spring-boot:run

run-active-licences-in-reporting-window-task:
	SPRING_PROFILES_ACTIVE=development TASKS_ACTIVELICENCESINREPORTINGWINDOW_ENABLED=true SPRING_MAIN_WEBAPPLICATIONTYPE=none ./mvnw spring-boot:run -Dspring-boot.run.arguments=$(startDate),$(endDate),$(csvFile)

run-active-licences-in-reporting-window-task-java:
	java -jar target/national-taxi-register-1.1-SNAPSHOT-spring-boot-executable.jar $(startDate) $(endDate) $(csvFile) --spring.profiles.active=development --tasks.active-licences-in-reporting-window.enabled=true --spring.main.web-application-type=none

package:
	./mvnw package -U

unit-test:
	./mvnw clean test -P jacoco -U

integration-test:
	./mvnw verify -DskipUnitTests -P jacoco -U

checkstyle:
	./mvnw checkstyle:checkstyle

generate-javadoc:
	./mvnw javadoc:javadoc

clean:
	./mvnw clean

dependency-security-check:
	./mvnw org.owasp:dependency-check-maven:check -P security

deploy-to-aws:
	test $(S3_BUCKET_NAME)
	test $(STACK_NAME)

	aws cloudformation package \
	--template-file sam.yaml \
	--output-template-file /tmp/output-sam.yaml \
	--s3-bucket $(S3_BUCKET_NAME)

	aws cloudformation deploy \
	--template-file /tmp/output-sam.yaml \
	--stack-name $(STACK_NAME) \
	--capabilities CAPABILITY_IAM

	aws cloudformation describe-stacks --stack-name $(STACK_NAME)

create-archetype: clean
	@if test ! -s ~/.m2/settings.xml; then \
		echo settings.xml does not exist, creating one; \
		echo "<settings></settings>" > ~/.m2/settings.xml; \
	fi
	./mvnw archetype:create-from-project
	./mvnw -f target/generated-sources/archetype/pom.xml install

sonar:
	./mvnw sonar:sonar

local-up:
	docker-compose -f docker/docker-compose.yml -p postgres_docker up -d

local-down:
	docker-compose -f docker/docker-compose.yml -p postgres_docker down

local-db-up:
	docker-compose -f docker/docker-compose.yml -p postgres_docker up -d postgres

local-db-down:
	docker-compose -f docker/docker-compose.yml -p postgres_docker down

local-redis-up:
	docker-compose -f docker/docker-compose.yml -p redis_docker up -d redis

local-redis-down:
	docker-compose -f docker/docker-compose.yml -p redis_docker down

# Example run: 'make sam-local-run EVENT=src/test/resources/sample_lambda_events/import_10_taxis.json'
sam-local-run:
	SPRING_PROFILES_ACTIVE='sam-local' sam local invoke JaquCazFunction -t sam.yaml -e $$EVENT --docker-network postgres_docker_default

localstack-up:
	SERVICES='s3,sqs,sns,ses' docker-compose -f docker/docker-compose-localstack.yml -p localstack_docker up -d

localstack-down:
	docker-compose -f docker/docker-compose-localstack.yml -p localstack_docker down

local-services-up: local-db-up localstack-up local-redis-up

local-services-down: local-db-down localstack-down local-redis-down

localstack-run:
	SPRING_PROFILES_ACTIVE='localstack,development' AWS_PROFILE='localstack' AWS_REGION='eu-west-2' ./mvnw spring-boot:run

local-integration-up:
	SERVICES='s3,sqs,sns,ses' docker-compose -f docker/docker-compose-integration.yml -p postgres_docker up -d

local-integration-down:
	docker-compose -f docker/docker-compose-integration.yml -p postgres_docker down

docker-stop:
	docker stop `docker ps -a -q`

docker-rm:
	docker rm `docker ps -a -q`

docker-clean: docker-stop docker-rm
