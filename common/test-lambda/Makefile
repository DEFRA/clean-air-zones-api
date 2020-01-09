.PHONY: \
	build \
	build-yolo \
	compile \
	run \
	run-development \
	package \
	generate-javadoc \
	clean \
	deploy-to-aws \
 	unit-test \
 	checkstyle

build:
	./mvnw verify -U

build-yolo:
	./mvnw verify -DskipTests -U

compile:
	./mvnw compile -U
  
run:
	./mvnw spring-boot:run

run-development:
	SPRING_PROFILES_ACTIVE=development ./mvnw spring-boot:run

package:
	./mvnw package -U

unit-test:
	./mvnw clean test -U

checkstyle:
	./mvnw checkstyle:checkstyle

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

# Example run: 'make sam-local-run EVENT=src/test/resources/sample_lambda_events/import_10_taxis.json'
sam-local-run:
	SPRING_PROFILES_ACTIVE='sam-local' sam local invoke TestLambdaFunction -t sam.yaml -e $$EVENT
