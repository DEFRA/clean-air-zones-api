## What is a task?

Task is an arbitrary job that can be started from jar file containing complete NTR application.

Task can run for many hours if necessary as it does not run any Lambda code and actually does not
run in Lambda runtime at all. It also won't spin any web server or expose REST endpoints.

You can treat task as a single focused command line application that you can pass parameters into.

In our case the application is jar file.

Although we are not using web or Lambda capabilities we still want to reuse Spring context and beans
especially database and AWS setup.

## Usage

Main usage for tasks are long running reporting queries related to NTR project. You can however
run any other job like importing or batch processing.

## How to use

Tasks are Spring Boot's ApplicationRunner implementations. By definition these components run
automatically before complete REST stack is ready. In our case, we can safely disable web application
capabilities when running tasks, so there won't be any REST endpoints and server waiting for requests.
Task will run until completion and the Spring Boot application will gracefully terminate.
To use a task you can check [application.yml](../../../../../../resources/application.yml)
tasks section to see what tasks are available. They are disabled by default in YAML file and please
do not change false to true - just keep them as disabled (false) in YAML file. To run a task in
development environment run:
`SPRING_PROFILES_ACTIVE=development TASKS_ACTIVELICENCESINREPORTINGWINDOW_ENABLED=true SPRING_MAIN_WEBAPPLICATIONTYPE=none ./mvnw spring-boot:run -Dspring-boot.run.arguments=2019-01-01,2020-10-30,report.csv`

or 

`make run-active-licences-in-reporting-window-task startDate=2019-01-01 endDate=2020-10-30 csvFile=raport.csv` for short.

As you can see we enable selected task at this point and also disable Spring Boot web application. 
You can run multiple tasks at once - they will run sequentially and application will terminate when the 
last of them terminates.

### Running in production

Project builds executable java jar which can be run without Maven. To run it you can initiate following command:
`java -jar target/national-taxi-register-1.1-SNAPSHOT-spring-boot-executable.jar 2019-01-01 2020-10-30 report.csv --spring.profiles.active=development --tasks.active-licences-in-reporting-window.enabled=true --spring.main.web-application-type=none`
This command can be used directly or as Docker entrypoint.

## How to add new task
1. Add your task flag to [application.yml](../../../../../../resources/application.yml) and mark it
as `enabled: false`. This step is not strictly necessary as we will keep `false` in YAML at all times, but it
works nicely as documentation and lists all tasks defined in NTR application.
2. Create a class similar to [ActiveLicencesInReportingWindowStarter](ActiveLicencesInReportingWindowStarter.java) and make it conditional on newly added property.
3. Add newly added flag as conditional to [AnyTaskEnabled](AnyTaskEnabled.java).
4. Create necessary Makefile targets for convenience. Take ActiveLicences as an example.