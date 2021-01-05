build:
	docker build . -t cognito-user-migration:0.1

# the minus sign ignores the errors - we should not stop if there is nothing to remove
cleanup:
	-docker rm `docker ps --filter "status=exited" -q`
	-docker rmi cognito-user-migration:0.1