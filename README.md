# Summa README

Summa is an open source search engine developed by the
State and University Library of Denmark. It is released
under the Apache License, Version 2.0.

This project is developed for internal use at State and University Library.
While it is certainly possible to use it elsewhere, it is not very
polished and documentation is lacking.

Homepage: http://wiki.statsbiblioteket.dk/summa
Git: https://github.com/statsbiblioteket/summa

## Technical

Summa is old software. It has been tested under Oracle and OpenJDK 1.8. 


## Running maven
Insert the following XML into the Maven2 settings file (under linux this i found
in ~/.m2/settings.xml:
```
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
```

If you don't want to or can't use the repository from Statsbiblioteket non
official jars are supplied in the checkout and can be installed into you local
maven repository.

Run the following command to create symbolic links that enables to create war
files under the web module.
$ `cd Web; ./mkLinks.sh; cd ../;`

and run `mvn compile` from the projects root directory.

## Building Summix

Run
$ `mvn package -DskipTests`

Summix should now be available in Summix/target/

Get the checksum by running
`md5sum Summix/target/summix-*-SNAPSHOT.zip`

## Bumping versionn

Change the version in the root `pom.xml`, then run `./newversion`.

For some unknown reason `Web/Modules/SummaWeb/pom.xml` is synchronized so that must be done by hand.
