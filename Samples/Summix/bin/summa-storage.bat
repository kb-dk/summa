@echo off
rem $Id:$
rem
rem A poor Windows BAT-equivalent to summa-storage.sh
rem

set BATLOCATION=%~dp0

rem Directory Structure:
rem The following directory structure will be assumed
rem
rem    app_root/
rem      bin/           rem All executable scripts go here, ie derivatives of this template
rem      lib/           rem All 3rd party libs/jar
rem      config/        rem Any properties or other config files
rem      MAINJAR        rem jar file containing the main class 
rem
rem Classpath Construction:
rem  - Any .jar in lib/ will be added to the classpath
rem  - config/ will be added to the classpath

set MAINCLASS=dk.statsbiblioteket.summa.storage.tools.StorageRunner

rem echo %MAINCLASS% 
rem echo %1%

set JVM_OPTS=-Dsumma.configuration=%1%

rem
rem DON'T EDIT BEYOND THIS POINT
rem

set NEWCLASSPATH=%CLASSPATH%;%BATLOCATION%../config/;%BATLOCATION%../lib/*;%MAINJAR%
rem echo %NEWCLASSPATH%

rem COMMAND="$JAVA_HOME/bin/java $JVM_OPTS $SECURITY_POLICY $JMX -cp $CLASSPATH $MAINCLASS"

@echo on
java %JVM_OPTS% -cp %NEWCLASSPATH% %MAINCLASS%

