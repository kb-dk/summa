#!/bin/bash

# USAGE:
#
# start-service.sh [path_to_service_xml [config_location]]
#
# This script can start any Summa service given a service.xml
# It will assume that the instance id of the service is the
# name of the directory containing the service.xml 
#

#
# TEMPLATE FOR RUNNING JARS FROM BASH SCRIPTS
#
# Directory Structure:
# The following directory structure will be assumed
#
#    app_root/
#      lib/           # All 3rd party libs/jar
#      config/        # Any properties or other config files
#      service.xml    # Service description used to launch the service
#
# Classpath Construction:
#  - Any .jar in lib/ will be added to the classpath
#  - config/ will be added to the classpath
#
#
# Config Options:
#    MAINJAR          # The jar containing the main classpath. Set to "." for no jar.
#    MAINCLASS        # The jar containing the main classpath
#    LIBDIRS          # Space separated list of paths in where to look for jar files. Defaults to ./lib
#    PRINT_CONFIG     # Optional. Print config to stderr. Set to empty to not print config.
#    JAVA_HOME        # Optional. If there's a global JAVA_HOME it will be used.
#    JVM_OPTS         # Optional arguments to the jvm.
#    SECURITY_POLICY  # Optional. The value of java.security.policy (path to .policy file)
#    ENABLE_JMX       # Optional. If set to "true" the JMX_* paramters will be used.
#
# JMX Options:
#    JMX_PORT         # Port to run JMX on (integer)
#    JMX_SSL          # Wheteher or not to use SSL. (true/false)
#    JMX_ACCESS       # Path to .access file
#    JMX_PASS         # Path to .password file
#

#
# EDIT HERE
#

# Detect the service.xml file
SERVICE_XML=$(pwd)/service.xml
if [ "$1" != "" ]; then
    SERVICE_XML="$1"
fi

# Abort if the service.xml is not found
if [ ! -f $SERVICE_XML ]; then
    echo -e "Service file $SERVICE_XML not found, or is not a regular file"
    echo -e "You may pass the location of the service file as a parameter"
    exit 1
fi

# Update working dir, and update SERVICE_XML relative to DEPLOY
DEPLOY=$(dirname $SERVICE_XML)
pushd $DEPLOY > /dev/null
SERVICE_XML=$(basename $SERVICE_XML)


# Detect CONFIG_LOCATION
CONFIGURATION=configuration.xml
if [ "$2" != "" ]; then
    CONFIGURATION="$2"
fi

# calculate the service instance id from the directory name as per spec
SERVICE_ID=$(basename $(pwd))

MAINJAR=$(grep '<mainJar>' $SERVICE_XML | sed -e 's@</\?mainJar>@@g' -e 's@[ \t]@@g')
MAINCLASS=$(grep '<mainClass>' $SERVICE_XML | sed -e 's@</\?mainClass>@@g' -e 's@[ \t]@@g')
LIBDIRS=lib
PRINT_CONFIG=true
#JAVA_HOME=/usr/lib/jvm/java
JVM_OPTS="-server -Xmx256m -Dsumma.configuration=$CONFIGURATION -Dsumma.control.service.id=$SERVICE_ID -Dsumma.control.service.basepath=$($DEPLOY/..)"
SECURITY_POLICY="$DEPLOY/config/policy"
#ENABLE_JMX=true

#JMX_PORT=8469
#JMX_SSL=false
#JMX_ACCESS="$DEPLOY/config/jmx/jmxremote.access"
#JMX_PASS="$DEPLOY/config/jmx/jmx.password"


#
# DON'T EDIT BEYOND THIS POINT
#

# Helper function to set properties in a properties file
# $1 : property name
# $2 : property value
# $3 : property file
function set_property () {
    if [ -z "$1" ]; then
        echo "ERROR: set_property : no property name given" 1>&2
        exit 1
    fi;
    if [ -z "$2" ]; then
        echo "ERROR: set_property : no property value given" 1>&2
        exit 1
    fi;
    if [ -f "$1" ]; then
        echo "ERROR: set_property : property file: is not a regular file" 1>&2
        exit 1
    fi;

    echo "Setting property $1=$2 in $3"
    sed -i -e "s@\(<entry key=\"$1\">\)\(.*\)\(</entry>\)@\1$2\3@g" $3

    if [ "$?" != "0" ]; then
        echo "Unable to set property. Bailing out." 1>&2
        exit 1
    fi;
}

# Build classpath
if [ "$LIBDIRS" == "" ]; then
    LIBDIRS=$DEPLOY/lib
fi
for libdir in $LIBDIRS
do
    for lib in `ls $libdir/*.jar`
    do
        if [ -z "$CLASSPATH" ]; then
            CLASSPATH=$lib
        else
            CLASSPATH=$CLASSPATH:$lib
        fi
    done
done
CLASSPATH=$CLASSPATH:config/:$MAINJAR

# Check JAVA_HOME
DEFAULT_JAVA_HOME=/usr/lib/jvm/java
if [ "$JAVA_HOME" == "" ]; then
    JAVA_HOME=$DEFAULT_JAVA_HOME
    echo "No JAVA_HOME set. Using default JAVA_HOME=$JAVA_HOME" 1>&2
fi;
if [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo "$JAVA_HOME/bin/java does not exist or is not an executable file." 1>&2
    exit 1
fi

# Check security policy
if [ "$SECURITY_POLICY" != "" ]; then
    SECURITY_POLICY="-Djava.security.policy=$SECURITY_POLICY"
fi;

# Check JMX
if [ "$ENABLE_JMX" == "true" ]; then
    JMX_PORT="-Dcom.sun.management.jmxremote.port=$JMX_PORT"
    JMX_SSL="-Dcom.sun.management.jmxremote.ssl=$JMX_SSL"
    JMX_ACCESS="-Dcom.sun.management.jmxremote.access.file=$JMX_ACCESS"
    JMX_PASS="-Dcom.sun.management.jmxremote.password.file=$JMX_PASS"
    JMX="$JMX_PORT $JMX_SSL $JMX_PASS $JMX_ACCESS";
fi;

COMMAND="$JAVA_HOME/bin/java $JVM_OPTS $SECURITY_POLICY $JMX -cp $CLASSPATH $MAINCLASS"

# Report settings
if [ ! -z $PRINT_CONFIG ]; then
    echo -e "JavaHome:\t$JAVA_HOME" 1>&2
    echo -e "Classpath:\t$CLASSPATH" 1>&2
    echo -e "MainJar:\t$MAINJAR" 1>&2
    echo -e "MainClass:\t$MAINCLASS" 1>&2
    echo -e "Configuration:\t$CONFIGURATION" 1>&2
    echo -e "Service file:\t$SERVICE_XML" 1>&2
    echo -e "Service id:\t$SERVICE_ID" 1>&2
    echo -e "Working dir:\t`pwd`" 1>&2
    echo -e "JMX enabled:\t$ENABLE_JMX" 1>&2
    echo -e "Security:\t$SECURITY_POLICY\n" 1>&2
    echo -e "Command line:\n$COMMAND\n" 1>&2
fi

$COMMAND

popd > /dev/null