# Summa README

Summa is an open source search engine developed by the
State and University Library of Denmark. It is released
under the Apache License, Version 2.0.

This project is developed for internal use at State and University Library.
While it is certainly possible to use it elsewhere, it is not very
polished and documentation is lacking.

This project has End Of Life around 2022-24, depending on circumstances,
and only high-priority bug fixes and features will be applied.

Git: https://github.com/statsbiblioteket/summa

## Requirements

* Java 1.8 (tested using Oracle JVM and OpenJDK)

## Building

Summa is a complex collection of modules. The `Summix`-module merges
these into a single distribution called `summix`.
 
 As several unit-tests can only pass in separate runs, building the
 project can only be done with `mvn -DskipTests package` :-(

The distribution package can be found in `Summix/target/` and is to be used
with the `SummaRise` project.

### Callback JARS

Summa uses XSLT callbacks for some data processing. In order to edit
XSLTs using jEdit, these callbacks must be generated and enabled in
jEdit.

Run `./callbackJARS` to generate the XSLT callback JARS. The result is
placed in the current folder.

## Developer setup

Running maven:
Insert the following XML into the Maven2 settings file (under linux this i found
in ~/.m2/settings.xml:
<settings>
  <mirrors>
    <mirror>
      <!--This sends everything else to /public -->
      <id>nexus</id>
      <mirrorOf>*</mirrorOf>
      <url>http://sbforge.statsbiblioteket.dk/nexus/content/groups/public</url>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>nexus</id>
      <!--Enable snapshots for the built in central repo to direct -->
      <!--all requests to nexus via the mirror -->
      <repositories>
        <repository>
          <id>central</id>
          <url>http://central</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>
      </repositories>
     <pluginRepositories>
        <pluginRepository>
          <id>central</id>
          <url>http://central</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
  <activeProfiles>
    <!--make the profile active all the time -->
    <activeProfile>nexus</activeProfile>
  </activeProfiles>
</settings>

If you don't want to or can't use the repository from Statsbiblioteket non
official jars are supplied in the checkout and can be installed into you local
maven repository.

## A note on storage

Summa is capable of using different database backends. It has build-in support for
creating and using H2, a database that uses local files under the user account.
H2 is used for unit-tests and works well for smaller production systems.

Scaling into multi-million records or relations, H2 has been tricky to work with.
For larger installations, PostgreSQL has been used successfully for years at kb.dk.

### Setting up PostgreSQL for Summa

1. Find a guide such as [How to install PostgreSQL on Ubuntu 20.04](https://www.digitalocean.com/community/tutorials/how-to-install-postgresql-on-ubuntu-20-04-quickstart)
and follow that.
2. Create a user and a database named `summa_doms` or similar descriptive name
```sql
$ sudo -u postgres psql
CREATE ROLE summa_doms with PASSWORD 'summa_doms' login;
CREATE DATABASE summa_doms;
GRANT ALL ON DATABASE summa_doms TO summa_doms;
exit
```
3. Create the relevant tables in the database
```shell script    
sudo -u postgres psql -v ON_ERROR_STOP=1 -1 -f ./Core/src/main/resources/storage/database/h2/create_summa_database.ddl summa_doms
```
