# JAQU-CAZ-Tariff-Service
JAQU CAZ Tariff Service

## Data validation rules

| Field                        | Validation                                                   | 
|------------------------------|--------------------------------------------------------------|
| tariff type                  | can be only 'A' 'B' 'C' or 'D'                               | 
| hgv entrant fee              | should be more or equal 0                                    | 
| hgv entrant fee              | Acceptable formats: „12”, „12.0”, „12.00”, „12.5”, „12.50”   | 
| taxi entrant fee             | should be more or equal 0                                    | 
| taxi entrant fee             | Acceptable formats: „12”, „12.0”, „12.00”, „12.5”, „12.50”   | 
| phv entrant fee              | should be more or equal 0                                    | 
| phv entrant fee              | Acceptable formats: „12”, „12.0”, „12.00”, „12.5”, „12.50”   | 
| bus entrant fee              | should be more or equal 0                                    | 
| bus entrant fee              | Acceptable formats: „12”, „12.0”, „12.00”, „12.5”, „12.50”   | 
| minibus entrant fee          | should be more or equal 0                                    | 
| minibus entrant fee          | Acceptable formats: „12”, „12.0”, „12.00”, „12.5”, „12.50”   | 
| car entrant fee              | should be more or equal 0                                    | 
| car entrant fee              | Acceptable formats: „12”, „12.0”, „12.00”, „12.5”, „12.50”   | 
| motorcycle ent fee           | should be more or equal 0                                    | 
| motorcycle ent fee           | Acceptable formats: „12”, „12.0”, „12.00”, „12.5”, „12.50”   | 
| coach entrant fee            | should be more or equal 0                                    | 
| coach entrant fee            | Acceptable formats: „12”, „12.0”, „12.00”, „12.5”, „12.50”   | 
| large van entrant fee        | should be more or equal 0                                    | 
| large van entrant fee        | Acceptable formats: „12”, „12.0”, „12.00”, „12.5”, „12.50”   | 
| small van entrant fee        | should be more or equal 0                                    | 
| small van entrant fee        | Acceptable formats: „12”, „12.0”, „12.00”, „12.5”, „12.50”   | 
| moped entrant fee            | should be more or equal 0                                    | 
| moped entrant fee            | Acceptable formats: „12”, „12.0”, „12.00”, „12.5”, „12.50”   | 
| caz name                     | must be string                                               | 
| caz name                     | can not be longer than 50 characters                         | 
| caz name                     | can not be empty                                             | 
| clear air zone               | must be uuid                                                 | 
| clear air zone               | can not be empty                                             | 
| main info url                | must be string                                               | 
| main info url                | can not be empty                                             | 
| pricing url                  | must be string                                               | 
| pricing url                  | can not be empty                                             | 
| operation hours url          | must be string                                               | 
| operation hours url          | can not be empty                                             | 
| exemption url                | must be string                                               | 
| exemption url                | can not be empty                                             | 
| pay caz url                  | must be string                                               | 
| pay caz url                  | can not be empty                                             | 
| become compliant url         | must be string                                               | 
| become compliant url         | can not be empty                                             | 
| financial assistance url     | must be string                                               | 
| financial assistance url     | can not be empty                                             | 
| emissions url                | must be string                                               | 
| emissions url                | can not be empty                                             | 
| boundary url                 | must be string                                               | 
| boundary url                 | can not be empty                                             | 

## First steps in Tariff Service

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

[Detailed descripton of how to build, run and test Tariff service](RUNNING_AND_TESTING.md)

## API specification

API specification is available at `{server.host}:{server.port}/v2/api-docs` (locally usually at http://localhost:8080/v2/api-docs)

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