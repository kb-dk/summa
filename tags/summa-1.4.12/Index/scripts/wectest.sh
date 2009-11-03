DEFAULT_JAVA_HOME="/usr/lib/jvm/java"

if [ "$JAVA_HOME" == "" ]; then
JAVA_HOME=$DEFAULT_JAVA_HOME
echo "No JAVA_HOME set. Using default: $JAVA_HOME"
fi;

HEAP_SIZE=-Xmx256m

SECURITY="-Djava.security.policy=config/.index.policy"

JMX_PORT=-Dcom.sun.management.jmxremote.port=25000
JMX_SSL=-Dcom.sun.management.jmxremote.ssl=false
JMX_ACCESS=-Dcom.sun.management.jmxremote.access.file=config/jmx/jmxremote.access
JMX_PASS=-Dcom.sun.management.jmxremote.password.file=config/jmx/jmx.password

# Enable JMX for the Employer
if [ "$1" == "employer" ]; then
JMX="$JMX_PORT $JMX_SSL $JMX_PASS $JMX_ACCESS";
fi;


$JAVA_HOME/bin/java -jar -server $JMX $HEAP_SIZE $SECURITY wec-test.jar $*
