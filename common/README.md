# JAQU-CAZ Java common

[![Build Status](http://drone-1587293244.eu-west-2.elb.amazonaws.com/api/badges/InformedSolutions/JAQU-CAZ-Java-Common/status.svg?ref=refs/heads/develop)](http://drone-1587293244.eu-west-2.elb.amazonaws.com/InformedSolutions/JAQU-CAZ-Java-Common)

## Version management

To easily update the version in the parent module and all submodules you can use the following 
command executed from the project's root directory:
```
$ scripts/set_version.sh 1.0-SNAPSHOT
``` 

## Deployment

To do deployment of single module please use command below

mvn -pl correlation-id deploy

correlation-id it is name of a module

## Modules

### Logger

Provides a default logging configuration which appends the value of `X-Correlation-Id` http request 
header if present.

#### Usage

In your project add a dependency on the `logger` module, for example:
```
<dependency>
  <groupId>uk.gov.caz</groupId>
  <artifactId>logger</artifactId>
  <version>1.8.0-SNAPSHOT</version>
</dependency>
```

Create `logback-spring.xml` in `src/main/resources` directory and include base configuration:

```
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <include resource="uk/gov/caz/logger/base.xml" />
</configuration>
```

Additionally you can put your own logging configuration there, e.g. change a log level for 
`org.springframework` package:

```
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <include resource="uk/gov/caz/logger/base.xml" />
  <logger name="org.springframework" level="DEBUG"/>
</configuration>
```
