#!/bin/sh

########## Search module #############

# Source
cd Modules/Search/src/main/java/dk/statsbiblioteket/summa/web/services/
ln -s ../../../../../../../../../../src/dk/statsbiblioteket/summa/web/services/SearchWS.java .

# WEB-INF
cd ../../../../../../webapp/WEB-INF/
ln -s ../../../../../../config/search/web.xml .
ln -s ../../../../../../config/search/server-config.wsdd .

# MEATA-INF
cd ../../resources/META-INF/
ln -s ../../../../../../../build.properties .

# Resources
cd ../
ln -s ../../../../../config/search/configuration.xml .
ln -s ../../../../../config/search/log4j.xml .
ln -s ../../../../../config/search/services.xml .

cd ../../../../

############ Storage module ##############

# Source
cd Storage/src/main/java/dk/statsbiblioteket/summa/web/services/
ln -s ../../../../../../../../../../src/dk/statsbiblioteket/summa/web/services/StorageWS.java .

# WEB-INF
cd ../../../../../../webapp/WEB-INF/
ln -s ../../../../../../config/storage/web.xml .
ln -s ../../../../../../config/storage/server-config.wsdd .

# META-INF
cd ../../resources/META-INF/
ln -s ../../../../../../../build.properties .

# Resources
cd ../
ln -s ../../../../../config/storage/configuration.xml .
ln -s ../../../../../config/storage/log4j.xml .
ln -s ../../../../../config/storage/services.xml .

cd ../../../../

############# Status module ################

# Source
cd Status/src/main/java/dk/statsbiblioteket/
ln -s ../../../../../../../src/dk/statsbiblioteket/gwsc/ .
cd summa/web
ln -s ../../../../../../../../../src/dk/statsbiblioteket/summa/web/RSSChannel.java .
cd services/
ln -s ../../../../../../../../../../src/dk/statsbiblioteket/summa/web/services/Status*.java .

# WEB-INF
cd ../../../../../../webapp/WEB-INF/
ln -s ../../../../../../config/status/web.xml .
ln -s ../../../../../../config/status/server-config.wsdd .

# Resources
cd ../../resources/
ln -s ../../../../../config/status/configuration.xml .
ln -s ../../../../../config/status/log4j.xml .
ln -s ../../../../../config/status/services.xml .

# META-INF
cd META-INF/
ln -s ../../../../../../../build.properties .

# web app
cd ../../webapp/
ln -s ../../../../../status_resources/* .

cd ../../../../

############# Website module ################

# Source
cd Website/src/main/java/dk/statsbiblioteket/
ln -s ../../../../../../../src/dk/statsbiblioteket/gwsc/ .
cd summa/web
ln -s ../../../../../../../../../src/dk/statsbiblioteket/summa/web/RSSChannel.java .
cd services/
ln -s ../../../../../../../../../../src/dk/statsbiblioteket/summa/web/services/Status*.java .

# WEB-INF
cd ../../../../../../webapp/WEB-INF/
ln -s ../../../../../../config/website/web.xml .

# Resources
cd ../../resources/
ln -s ../../../../../config/website/log4j.xml .
ln -s ../../../../../config/website/services.xml .

# META-INF
cd META-INF/
ln -s ../../../../../../../build.properties .

# web app
cd ../../webapp/
ln -s ../../../../../website_resources/* .

cd ../../../../
