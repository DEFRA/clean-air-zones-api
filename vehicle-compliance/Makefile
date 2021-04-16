.PHONY: \
	build \
	build-yolo \
	package \
	integration-test \
	unit-test \
	run \
	generate-javadoc \
	clean \
	deploy-to-aws \
	create-archetype

build:
	./mvnw verify -P jacoco

build-yolo:
	./mvnw verify -DskipTests

compile:
	./mvnw compile

package:
	./mvnw package

unit-test:
	./mvnw clean test -P jacoco

scenario-test:
	./mvnw test -Pcucumber-tests

generate-test-data:
	docker exec -u postgres postgres-vcc.docker psql -d vehicle_compliance -c "COPY vehicle TO STDOUT WITH CSV HEADER" > src/it/resources/test_data.csv
	docker exec -u postgres postgres-vcc.docker psql -d vehicle_compliance -c "COPY (SELECT registrationnumber, vehicletype, expectedexempt FROM public.vehicle) TO STDOUT WITH CSV HEADER" > src/it/resources/test_compliance_data.csv

integration-test:
	./mvnw verify -DskipUnitTests -P jacoco

run:
	./mvnw spring-boot:run

run-development:
	SPRING_PROFILES_ACTIVE=development ./mvnw spring-boot:run

generate-javadoc:
	./mvnw javadoc:javadoc

clean:
	./mvnw clean

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

local-remove:
	docker ps -aq | xargs docker rm -f

local-db-up:
	docker-compose -f docker/docker-compose.yml -p postgres_docker up -d postgres-vcc

local-db-down:
	docker-compose -f docker/docker-compose.yml -p postgres_docker down

# Example run: 'make sam-local-run EVENT=src/test/resources/sample_lambda_events/import_10_taxis.json'
sam-local-run:
	SPRING_PROFILES_ACTIVE='sam-local' sam local invoke JaquCazVccFunction -t sam.yaml -e $$EVENT --docker-network postgres_docker_default

localstack-up:
	SERVICES='s3,sqs,sns' docker-compose -f docker/docker-compose-localstack.yml -p localstack_docker up -d

localstack-down:
	docker-compose -f docker/docker-compose-localstack.yml -p localstack_docker down

local-services-up: local-db-up localstack-up

local-services-down: local-db-down localstack-down

localstack-run:
	SPRING_PROFILES_ACTIVE='localstack,development' AWS_PROFILE='localstack' AWS_REGION='eu-west-2' ./mvnw spring-boot:run
	
frontend-up:
	$(aws ecr get-login --no-include-email --region eu-west-2)
	docker pull 018330602464.dkr.ecr.eu-west-2.amazonaws.com/vccs-web:latest	
	docker run --network host -ti -e "GOOGLE_ANALYTICS_ID=purple123" -e "SECRET_KEY_BASE=sckbse123" -e "COMPLIANCE_CHECKER_API_URL=localhost:8080" -p 3000\:3000 018330602464.dkr.ecr.eu-west-2.amazonaws.com/vccs-web

docker-stop:
	docker stop `docker ps -a -q`

docker-rm:
	docker rm `docker ps -a -q`

docker-clean: docker-stop docker-rm

dependency-security-check:
	./mvnw org.owasp:dependency-check-maven:check -P security

local-integration-up:
	SERVICES='s3,sqs,sns' docker-compose -f docker/docker-compose-integration.yml -p postgres_docker up -d

local-integration-down:
	docker-compose -f docker/docker-compose-integration.yml -p postgres_docker down