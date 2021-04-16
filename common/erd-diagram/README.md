# SchemaSpy tool
This tool will create ERD diagram of JAQU Database.

## Quick Run
After running
```
make accounts
```
This script should create static website which has ERD diagrams. F.e. you can copy `output/diagrams/summary/relationships.real.large.png` as full ERD diagram to your documentation.

## How does it work
This script uses [SchemaSpy official docker image](https://hub.docker.com/r/schemaspy/schemaspy/). It reads DB schema on realtime and performs some nice statistics. To generate schema for Accounts DB you need to run a job from Makefile:
```
make accounts
```
It will try to read database listening on `5432` of your local machine. This database is running when you start running your local Accounts database on port using `local-db-up` make command of Accounts API project.

Orphan tables are not part of whole relationship diagram. There are visualised separately.

## Configuration
SchemaSpy uses lots of configuration options, so you can adjust how it works. F.e. you can change db connection parameters:
- host and port of Postgres DB
- schema to read
- user and password
You can also adjust the way diagrams are produced, f.e. usually for ERD diagram it's not needed to count rows in each table.
- `-norows`
Full list of these params you can find [at the official configuration glossary](https://schemaspy.readthedocs.io/en/latest/configuration/commandline.html#commandline).
