#!/bin/sh

########## Search module #############

# Source
cd Modules/Search/src/main/java/dk/statsbiblioteket/summa/web/services/
rm SearchWS.java

# WEB-INF
cd ../../../../../../webapp/WEB-INF/
rm web.xml 
rm server-config.wsdd 

# MEATA-INF
cd ../../resources/META-INF/
rm build.properties 

# Resources
cd ../
rm configuration.xml 
rm log4j.xml 
rm services.xml 

cd ../../../../

############ Storage module ##############

# Source
cd Storage/src/main/java/dk/statsbiblioteket/summa/web/services/
rm StorageWS.java

# WEB-INF
cd ../../../../../../webapp/WEB-INF/
rm web.xml
rm server-config.wsdd 

# META-INF
cd ../../resources/META-INF/
rm build.properties 

# Resources
cd ../
rm configuration.xml 
rm log4j.xml 
rm services.xml 

cd ../../../../

############# Status module ################

# Source
cd Status/src/main/java/dk/statsbiblioteket/
rm gwsc
cd summa/web
rm RSSChannel.java
cd services/
rm Status*.java

# WEB-INF
cd ../../../../../../webapp/WEB-INF/
rm web.xml
rm server-config.wsdd 

# Resources
cd ../../resources/
rm configuration.xml
rm log4j.xml 
rm services.xml 

# META-INF
cd META-INF/
rm build.properties 

# web app
cd ../../webapp/
rm images  
rm index.jsp 
rm rss.jsp 
rm xml.jsp

cd ../../../../

############# Website module ################

# Source
cd Website/src/main/java/dk/statsbiblioteket/
rm gwsc
cd summa/web
rm RSSChannel.java 
cd services/
rm Status*.java 

# WEB-INF
cd ../../../../../../webapp/WEB-INF/
rm web.xml 

# Resources
cd ../../resources/
rm log4j.xml
rm services.xml 

# META-INF
cd META-INF/
rm build.properties 

# web app
cd ../../webapp/
rm css 
rm images 
rm index.jsp 
rm js 
rm service 
rm showrecord.jsp 
rm xslt

cd ../../../../
