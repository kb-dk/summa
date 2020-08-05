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

## Building

Summa is a complex collection of modules. The `Summix`-module merges
these into a single distribution called `summix`.
 
 As several unit-tests can only pass in separate runs, building the
 project can only be done with `mvn -DskipTests package` :-(

The distribution package can be found in `Summix/target/` and is to be used
with the `SummaRise` project. See  

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

