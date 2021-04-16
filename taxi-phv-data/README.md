# JAQU-CAZ-National-Taxi-Register
JAQU CAZ National Taxi Register

[![Build Status](http://drone-1587293244.eu-west-2.elb.amazonaws.com/api/badges/InformedSolutions/JAQU-CAZ-National-Taxi-Register/status.svg?ref=refs/heads/develop)](http://drone-1587293244.eu-west-2.elb.amazonaws.com/InformedSolutions/JAQU-CAZ-National-Taxi-Register)

## First steps in National Taxi Register

### Provisioning a development environment

In the root of this repository a vagrantfile can be found. To deploy a local development environment, the following pre-requisites must be present on your host;

1. Vagrant
1. Powershell
1. VirtualBox

Once these above 3 items are installed, simply clone this repository and from the root directory issue the command `vagrant up`. This will then proceed to download a virtual machine image and launch it on your machine. The username and password for the VM is vagrant/vagrant. Once your environment has been provisioned please follow the remaining steps detailed below to run the codebase.

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

### Configuring Nexus access
What is [Nexus](https://www.sonatype.com/nexus-repository-sonatype)?

*Nexus manages components, build artifacts, and release candidates in one central location.* We 
use it as repository for our internal artifacts but also as a proxy for Maven central repo - so as a cache
speeding up our builds.

You need to configure access to JAQU Nexus instance because without it you won't be able to build
and deploy artifacts and projects.

Firstly you need to obtain 3 values:
1. Nexus URL
2. Nexus username
3. Nexus password

You can ask a fellow developer or dedicated DevOps team for these values. Now you need to copy 
`settings.ci.xml.template` from `ci-cd-resources` directory to your local Maven repo dir: `~/.m2/`.
Then backup any existing `~/.m2/settings.xml` file and either copy contents of `settings.ci.xml.template` into
`settings.xml` or rename `settings.ci.xml.template` to `settings.xml`.

Now you need to set Nexus data.
You can either set 3 environment variables:
1. `export JAQU_NEXUS_URL=<nexus url>`
1. `export JAQU_NEXUS_USER=<nexus user>`
1. `export JAQU_NEXUS_PASSWORD=<nexus password>`

or:

Replace `${env.JAQU_NEXUS_URL}`, `${env.JAQU_NEXUS_USER}` and `${env.JAQU_NEXUS_PASSWORD}` strings in
`settings.xml` to the values you got from colleague or DevOps team.

### Vagrant
Optionally you can use Virtual Machine to compile and test project.
A Vagrant development machine definition inclusive of the following software assets can be found at the root of this repository:

1. Ubuntu 18.04 LTS
1. Eclipse for Java Enterprise
1. OpenJDK 8
1. Maven
1. Git
1. Docker CE (for backing tools used for example to emulate AWS lambda functions and DB instances)

As a complimentary note, this Vagrant image targets VirtualBox as its provider. As such, the necessary technical dependencies installed on the host are simply VirtualBox and Vagrant.

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

[Detailed descripton of how to build, run and test NTR service](RUNNING_AND_TESTING.md)
## Features

### Registering licences of taxis/PHVs

The core functionality of the service is to act as centralised and authoritative register that 
details all taxis and PHVs (private hire vehicles) operating in the United Kingdom. A given 
authorised third party can maintain the data using two exposed interfaces: HTTP REST API and 
CSV file based data transport interface.

#### Input data

Input data (irrespective of the used interface) must contain details of all taxis and/or PHVs that 
are currently licensed by the Licensing Authority (or a group of licensing authorities). The 
payload should not contain those taxis and/or PHVs that have been added or changed since the last 
update.

The payload contains a list of attributes that specifies the licences which are to be registered:
* VRM/VRN (Vehicle Registration Mark/Vehicle Registration Number)
* licence start date
* licence end date
* description (a string which specifies whether this is a taxi or PHV licence)
* licensing authority name
* licence plate number
* wheelchair accessible vehicle (an optional attribute of a boolean type indicating whether the 
vehicle is wheelchair accessible) 

#### Uniqueness of a licence

The following attributes are unique per given licence:
* VRM/VRN
* licence start date
* licence end date
* licensing authority name
* license plate number

Some implications:
* the only attributes that can change for a given licence are `description` and `wheelchair accessible vehicle`
* there can be many licences for a given *VRM* in different licensing authorities, i.e. `(vrm, startDate, endDate, plateNumber)` 
data can be the same for different licensing authorities

#### Registration algorithm

Preconditions:
* the given party which wants to modify the register must be authorised to modify the data of the
given licensing authority (`AUTHORISED_UPLOADER_IDS` column of `T_MD_LICENSING_AUTHORITY` table), 
otherwise the registration submission is rejected
* the registration is rejected when for a given licensing authority there is an ongoing 
registration process of a licensing authority authorised to modify the data of the former licensing 
authority ("special case": the given licensing authority cannot simultaneously register licences)

* Before the processing takes place all preconditions are checked
* All input licences are grouped by licensing authority and for each licensing authority:
    * Records which are in the database, but *NOT* in the input data are removed (the equality of records is determined by the equality of their [unique attributes](#uniqueness-of-a-licence))
    * Records which are in the database and in the input data and any of their `description` and `wheelchair accessible vehicle` attributes have changed are updated
    * Records which are not in the database, but in the input data, are inserted into the database 

Some implications:
* the data of a given licensing authority is affected if there is at least one licence in the input data for that licensing authority
* it is impossible to remove all licences for a given licensing authority

#### Validation

Validation rules can be found [here](#validation-rules-for-registering-vehicles).

### Get licence details by VRM

#### Overview

It allows to find *active licences details* of a given vehicle's (identified by its *VRM*) .

The functionality is exposed as an endpoint which returns an object rendered as either *JSON* or *XML* response (this is specified by the client using the HTTP `Accept` header).
The aforementioned object contains two flags, `active` and `wheelchairAccessible` which have the following meaning:
* `active`: this flag is set to 
  * `true` if the given vehicle has any **active** operating licence
  * `false` otherwise
* `wheelchairAccessible`: this flag is set to
  * `true` if the given vehicle has any **active** operating licence which is *wheelchair accessible*
  * `false` if either
    * there is at least one active licence with at least one with *wheelchairAccessible* flag set to `false`, but none to `true` in the database or
    * there is no active licence and there is a licence with *wheelchairAccessible* set to `false`
  * `null` if either
    * there is at least one active licence and for every active one *wheelchairAccessible* flag is `null` in the database or
    * there is no active licence and for every inactive one *wheelchairAccessible* flag is `null` in the database
  
No content with the `404` HTTP status code is returned if the passed *VRM* is not found in the registry.

#### Try it

It is exposed as an endpoint under `/v1/vehicles/{vrm}/licence-info` path (see the value of `uk.gov.caz.taxiregister.controller.LookupController.PATH`).
`src/it/resources/data/sql` directory contains `sql` files which can be used locally to seed the database (those files are used by integration tests).

Sample invocation:
```
$ curl -s \
--header 'X-Correlation-Id: 63eed8a1-3980-4193-8d0b-5cbdd8f67b82' \
--header 'Accept: application/json' \
localhost:8080/v1/vehicles/CB51QMR/licence-info
```

Sample output:
```
{
  "active": true,
  "wheelchairAccessible": false
}
```

### Reporting

#### Getting names of Licensing Authorities that have not uploaded licences in specified number of days

Clean Air Zone - National Taxi Register project puts a requirement on all Licensing Authorities to send
taxi/PHV licences that they manage locally. It is mandatory for each LA to provide complete list
of managed licences either by API Upload call or by CSV file submission **at least once a week**. 

This report allows to list names of Licensing Authorities that have not uploaded licences for
specified number of days. JAQU Admin can then take appropriate actions towards such LAs.

Implementation is provided by Postgres DB function, defined in 
`src/main/resources/db.changelog/rawSql/0004-1.1-create_functions_to_get_licensing_authorities_which_have_not_uploaded_in_last_days.sql` file.

Function is named: `authorities_that_have_not_uploaded_licences_in_last_days(number_of_days integer)`
and takes one parameter called `number_of_days` of type `integer`.

`number_of_days` must be zero or positive number. It specifies maximum number of days the function should go back from today
to look for Licensing Authorities that have not uploaded data. If set to zero it should list LAs that
have not uploaded today. Please take notice that only whole days are taken into account. In other words
hour:minute:second of current time today and time of upload does not matter.

Output or result of function is a Table with one column named `licensing_authority_name` and type `varchar(50)`.
If there are any Licensing Authorities that forgot to upload, their names will be present as rows in this table.

Function puts additional information to caller's console in case when all Licensing Authorities uploaded.
Caller is aware that everything is OK in case it gets empty table in response by getting this message: `Every Licensing Authority uploaded within 7 days`.

To call the function and list all Licensing Authorities that have not uploaded for 7 days
 run `SELECT * FROM authorities_that_have_not_uploaded_licences_in_last_days(7);`

#### Getting names of Licensing Authorities which had active licences for a given VRM on the specified date

For a given *VRM* it is possible to obtain the names of Licensing Authorities of its all active licences
on the specified date.

*WARNING* If the licensing authority for a given active licence was *deleted*, `UNKNOWN` is used as its name in the response body. 
This is because licensing authorities' names are fetched from the 'active' table, not the _audit log_. 

This information is exposed by the following endpoint:
```
/v1/vehicles/{vrm}/licence-info-audit?date=YYYY-MM-DD
```
where `date` is an optional parameter, today's date is used unless specified. 

Sample invocation:
```
$ curl localhost:8080/v1/vehicles/BD51SMR/licence-info-audit\?date\=2019-01-15 \
-H 'Accept: application/json' \
-H 'X-Correlation-ID: 55a40196-f1d2-4e8b-8b2f-4857a023ec5b'
```

Sample output:
```
{
  "licensingAuthoritiesNames": [
    "Leeds"
  ]
}
```

#### Getting VRMs which had at least one active licence on the specified date for a given Licensing Authority

For a given *licensing authority id* it is possible to obtain VRMs of vehicles which had at least one
active licence for a specified date. When the given licensing authority id does not exist in the 
database, an empty list is returned with 200 status code.

This information is exposed by the following endpoint:
```
v1/licensing-authorities/{licensingAuthorityId}/vrm-audit?date=YYYY-MM-DD
```
where `date` is an optional parameter, today's date is used unless specified. 

Sample invocation:
```
$ curl localhost:8080/v1/licensing-authorities/1/vrm-audit\?date\=2019-01-15 \
  -H 'Accept: application/json' \
  -H 'X-Correlation-ID: 55a40196-f1d2-4e8b-8b2f-4857a023ec5b'
```
 
Sample output:
```
{
  "licensingAuthorityId": 1,
  "auditDate": "2019-01-15",
  "vrmsWithActiveLicences": []
}
```

#### Getting events related to active licences during specified reporting window

This reporting query is implemented as a task (see Tasks section below) because it needs to 
traverse complete audit log history which takes a lot of time. To run the query you need to build the 
project and use final jar file: `./target/national-taxi-register-1.1-SNAPSHOT-spring-boot-executable.jar`.
To run reporting execute: `java -jar target/national-taxi-register-1.1-SNAPSHOT-spring-boot-executable.jar 2020-05-01 2020-07-01 report.csv --spring.profiles.active=development --tasks.active-licences-in-reporting-window.enabled=true --spring.main.web-application-type=none`

Reporting takes 2 date parameters: start and end date of desired reporting window.
What you get is a stream of events, sorted by date, that happened to licences during reporting window. 
Additionally you see what was the licence state before reporting window, so even if nothing happened
during the window you will know that licence was active and with what parameters.

## Database management

Liquibase is being used as database migrations tool.
Please check `src/main/resources/db.changelog` directory. It contains file named `db.changelog-master.yaml`
which is automatically picked up by Spring Boot at application startup. This file drives
application of all changesets and migrations.

### Liquibase naming convention
Each changeset should be prefixed with consecutive 4-digit number left padded with zeros.
For example: 0001, 0002, 0003. Then current application version should be put and finally some
short description of change. For example:

`0001-1.0-create_tables_taxi_phv_licensing_authority.yaml`

What we see is application order number, at what application version change was made and finally
a short description of changes. Pretty informative and clean.

If one changeset file contains more than one change, please put consecutive numbers in changeset id:

`0002.1`, `0002.2` and so on.

Raw SQL files must be used from Liquibase Yaml changesets and put into `rawSql` subfolder.
Please use existing files as an example.

## API specification

API specification is available at `{server.host}:{server.port}/v1/swagger-docs` (locally usually at http://localhost:8080/v1/swagger-docs)

## Branching Strategy

### GitFlow

We use [GitFlow](https://nvie.com/posts/a-successful-git-branching-model/) for source control management.

At a high level, GitFlow consists of the following workflow:

-   New features are developed in a `feature` branch
-   `feature` branches are created off the `develop` branch and merged back into it when ready to be released
-   A `release` branch is created from the `develop` branch when it is time to release
-   The `release` branch is used to track prospective deployments - any fixes made prior to a release should target this branch
-   After the release, the `release` branch is merged into both `master` and `develop`, at which point the release is tagged in `master`
-   `hotfix` branches are created from `master` and merged back into `master` and `develop` when finished

### Branch naming

Branch names should follow the following pattern:

```
[branchType]/[ticketReference]/[description]
```
For example:
```
feature/NTR-9/my-small-feature
```
### Branch types

|Branch Type     |Use case                       |
|----------|-------------------------------|
|`feature` |New feature for the user, not a new feature for build script|
|`fix`     |Bug fix for the user, not a fix to a build script|
|`refactor`|Refactoring production code, e.g. renaming a variable|
|`docs`    |Changes to the documentation|
|`style`   |Formatting, missing semi colons, etc.; no production code change|
|`test`    |Adding missing tests, refactoring tests; no production code change|
|`chore`   |Updating build scripts etc.; no production code change|

### Merging into protected branches

The `develop`  and `master` branches are protected branches. Polices are enforced to prevent unstable code escalating into a Production environment. In order to merge into these branches, the following conditions must be satisfied:

- [ ] proposed code changes has been submitted for, and accepted following, peer review
- [ ] CI tests have passed

## Validation rules for registering vehicles

### Common API/CSV validation rules

| Rule description                                                                          | Trigger | Error message                                                                                                                          |
|-------------------------------------------------------------------------------------------|---------|----------------------------------------------------------------------------------------------------------------------------------------| 
| Missing/empty VRM field                                                                   | API     | ```{"vrm":null,"title":"Mandatory field missing","detail":"Missing VRM value","status":400}```                                         |
| Missing/empty VRM field                                                                   | CSV     | Line {}: Missing VRM value                                                                                                             | 
| VRM field too long                                                                        | API     | ```{"vrm":"{VRM}","title":"Value error","detail":"Too long VRM. VRM should should have from 1-7 characters","status":400}```           |
| VRM field too long                                                                        | CSV     | Line {}: Too long VRM. VRM should should have from 1-7 characters.                                                                     | 
| Invalid format of VRM                                                                     | API     | ```{"vrm":"{VRM}","title":"Value error","detail":"Invalid format of VRM","status":400}```                                              |
| Invalid format of VRM                                                                     | CSV     | Line {}: Invalid format of VRM                                                                                                         | 
| Missing licence start date                                                                | API     | ```{"vrm":"AAA999A","title":"Mandatory field missing","detail":"Missing start date","status":400}```                                   |
| Missing licence start date                                                                | CSV     | Line {}: Missing start date                                                                                                            | 
| Missing licence end date                                                                  | API     | ```{"vrm":"AAA999A","title":"Mandatory field missing","detail":"Missing end date","status":400}```                                     |
| Missing licence end date                                                                  | CSV     | Line {}: Missing end date                                                                                                              | 
| Invalid format of licence start date                                                      | API     | ```{"vrm":"AAA999A","title":"Value error","detail":"Invalid start date format","status":400}```                                        |
| Invalid format of licence start date                                                      | CSV     | Line {}: Invalid start date format                                                                                                     | 
| Invalid format of licence end date                                                        | API     | ```{"vrm":"AAA999A","title":"Value error","detail":"Invalid end date format","status":400}```                                          |
| Invalid format of licence end date                                                        | CSV     | Line {}: Invalid end date format                                                                                                       | 
| Invalid licence dates order                                                               | API     | ```{"vrm":"AAA999A","title":"Value error","detail":"Start date must be before end date","status":400}```                               |
| Invalid licence dates order                                                               | CSV     | Line {}: Start date must be before end date                                                                                            | 
| Missing licence type                                                                      | API     | ```{"vrm":"AAA999A","title":"Mandatory field missing","detail":"Missing taxi/PHV value","status":400}```                               |
| Missing licence type                                                                      | CSV     | Line {}: Missing taxi/PHV value                                                                                                        | 
| Licence type too long                                                                     | API     | ```{"vrm":"AAA999A","title":"Value error","detail":"Taxi/PHV value must be less than 100 characters","status":400}```                  |
| Licence type too long                                                                     | CSV     | Line {}:  Taxi/PHV value must be less than 100 characters                                                                              | 
| Missing licence plate number                                                              | API     | ```{"vrm":"AAA999A","title":"Mandatory field missing","detail":"Missing licence plate number","status":400}```                         |
| Missing licence plate number                                                              | CSV     | Line {}: Missing licence plate number                                                                                                  | 
| Invalid format of licence plate number                                                    | API     | ```{"vrm":"AAA999A","title":"Value error","detail":"Invalid format of licence plate number","status":400}```                           |
| Invalid format of licence plate number                                                    | CSV     | Line {}: Invalid format of licence plate number                                                                                        | 
| Missing licensing authority name                                                          | API     | ```{"vrm":"AAA999A","title":"Mandatory field missing","detail":"Missing licensing authority name","status":400}```                     |
| Missing licensing authority name                                                          | CSV     | Line {}: Missing licensing authority name                                                                                              | 
| Invalid format of licensing authority name                                                | API     | ```{"vrm":"AAA999A","title":"Value error","detail":"Invalid licensing authority name","status":400}```                                 |
| Invalid format of licensing authority name                                                | CSV     | Line {}: Invalid licensing authority name                                                                                              |
| License start date too far in past                                                        | CSV     | Line {}: Start date cannot be more than 20 years in the past                                                                           | 
| License start date too far in past                                                        | API     | ```{"vrm":"AAA999A","title":"Value error","detail":"Start date cannot be more than 20 years in the past","status":400}```              | 
| License end date too far in future                                                        | CSV     | Line {}: End date cannot be more than 20 years in the future                                                                           | 
| License end date too far in future                                                        | API     | ```{"vrm":"AAA999A","title":"Value error","detail":"End date cannot be more than 20 years in the future","status":400}```              | 
| Wheelchair accessible boolean is not "true" or "false" (any capitalization)               | CSV     | Line {}: Invalid wheelchair accessible value. Can only be True or False                                                                | 
| Wheelchair accessible boolean is not "true" or "false" (any capitalization)               | API     | ```{"vrm":"AAA999A","title":"Value error","detail":"Invalid wheelchair accessible value. Can only be True or False","status":400}```   | 


### Validation rules applicable only to CSV upload

| Rule description                                                                                               | Trigger | Error message                                                                                                  |
|----------------------------------------------------------------------------------------------------------------|---------|----------------------------------------------------------------------------------------------------------------|  
| NTR microservice cannot connect to S3 bucket/filename. Bucket or filename does not exist or is not accessible. | CSV     | S3 bucket or file not found or not accessible                                                                  | 
| Lack of 'uploader-id' metadata                                                                                 | CSV     | 'uploader-id' not found in file's metadata                                                                     | 
| Invalid format of 'uploader-id'                                                                                | CSV     | Malformed ID of an entity which want to register vehicles by CSV file. Expected a unique identifier (UUID)     | 
| Too large CSV file                                                                                             | CSV     | Uploaded file size exceeded "Max size: 500MB"                                                                  | 
| Invalid fields number in CSV                                                                                   | CSV     | Line {}: Invalid record "Missing data"                                                                         | 
| Maximum line length exceeded                                                                                   | CSV     | Line {}: Line is too long (actual value: {}, allowed value: 110)                                               | 
| Invalid format of a line (e.g. it contains invalid characters)                                                 | CSV     | Line {}: Invalid character or empty row detected                                                               | 
| Potentially included header row                                                                                | CSV     | Line 1: Header information should not be included                                                              | 

### Validation rules applicable only to API upload

| Rule description                                       | Trigger | Error message                                                                                                                                                                                     |
|--------------------------------------------------------|---------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Missing request payload                                | API     | ```{"vrm":"","title":"Validation error","detail":"Required request body is missing(...)","status":400}```                                                                                         |
| Invalid format of 'uploader-id'                        | API     | ```{"vrm":"","title":"Validation error","detail":"Invalid format of uploader-id, expected: UUID","status":400}```                                                                                 |
| Empty JSON payload ('{}')                              | API     | ```{"vrm":"","title":"Validation error","detail":"vehicleDetails cannot be null","status":400}```                                                                                                 |
| Malformed request payload (malformed JSON, e.g. '{,}') | API     | ```{"vrm":"","title":"Validation error","detail":"JSON parse error: Unexpected character ...","status":400}```                                                                                    |
| Missing Content-type header                            | API     | ```{"timestamp":1565101505257,"status":415,"error":"Unsupported Media Type","message":"Content type  not supported","path":"/v1/scheme-management/taxiphvdatabase"}```                            |
| Unsupported Content-type header                        | API     | ```{"timestamp":1565101610979,"status":415,"error":"Unsupported Media Type","message":"Content type {NOT_SUPPORTED_CONTENT_TYPE} not supported","path":"/v1/scheme-management/taxiphvdatabase"}```|
| Wrong HTTP method                                      | API     | ```{"timestamp":1565101671868,"status":405,"error":"Method Not Allowed","message":"Request method {METHOD} not supported","path":"/v1/scheme-management/taxiphvdatabase"}```                      | 
| Missing 'X-Correlation-ID' header                      | API     | ```Missing request header 'X-Correlation-ID' for method parameter of type String```                                                                                                               | 
| Missing 'x-api-key' header                             | API     | ```Missing request header 'x-api-key' for method parameter of type String```                                                                                                                      | 

## Tasks
For more details about what are tasks and how to define and use see [TASKS_HOWTO](./src/main/java/uk/gov/caz/taxiregister/tasks/TASKS_HOWTO.md)
