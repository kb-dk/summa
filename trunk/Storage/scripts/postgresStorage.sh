# Start script for lucene storage
#
# Environment Variable Prequisites
#
#   DEPLOY
#
#   RESUME          Start resuming previous run (if something went wrong)
#
#   RUNTIME         When to make first run  (YYYY-MM-DD HH:mm:SS:ss)
#
#   PERIOD          Period in days between updates.
#
#   JAVA_HOME       Must point at your JRE or JDK instalation (requires JAVA1.5.0).
#
#   JAVA_OPTS       (Optional) Java runtime options used when
#
#   SECURITY_POLICY Has to point to a policy file, enabeling rmi socket communication.
#
# -----------------------------------------------------------------------------

#DEPLOY
DEPLOY=`pwd`

#RMIServer
RMISERVER=atria

#RMIPort
#RMIPort=8500
RMIPort=19000

#RMIServiceName
RMIServiceName=summa_io_postgres

#ServicePort
#ServicePort=8530
ServicePort=19500

#JAVA_HOME
JAVA_HOME=/usr/java/jdk1.5.0_07

#JAVA_OPTS
JAVA_OPTS="-server -Xmx512m -Xms512m -Dcom.sun.management.jmxremote"

#JMX
JMX_PORT=-Dcom.sun.management.jmxremote.port=22000
JMX_SSL=-Dcom.sun.management.jmxremote.ssl=false
JMX_ACCESS=-Dcom.sun.management.jmxremote.access.file=$DEPLOY/config/jmx/jmxremote.access
JMX_PASS=-Dcom.sun.management.jmxremote.password.file=$DEPLOY/config/jmx/jmx.password

#SECURITY_POLICY
SECURITY_POLICY=$DEPLOY/config/.storage.policy

chmod 600 config/jmx/*

#echo $SECURITY_POLICY

#CODE_BASE
CODE_BASE=http://developer.statsbiblioteket.dk/remoteStubs/LuceneStorageStub.jar

$JAVA_HOME/bin/rmiregistry $RMIPort &

$JAVA_HOME/bin/java -jar $JAVA_OPTS -Dsun.rmi.transport.tcp.connectionPool=true -Djava.rmi.server.codebase=$CODE_BASE -Djava.security.policy=$SECURITY_POLICY $JMX_PORT $JMX_SSL $JMX_ACCESS $JMX_PASS PostgresStorage.jar $RMISERVER $RMIPort $RMIServiceName $ServicePort &
