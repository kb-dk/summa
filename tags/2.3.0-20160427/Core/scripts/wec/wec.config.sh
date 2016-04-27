#!/bin/sh

#
# The following files must be remotely available:
#
#     $DEPLOYER/wecindexer.tgz
#     $DEPLOYER/id_rsa.pub
#

#
# Any script sourcing this file is obligated to set the
# WEC_ROOT variable to the root of the wec installation.
#


# Deployment info
export DEPLOYER=http://pc134.sb.statsbiblioteket.dk/~mikkel
export TARBALL=wecindexer.tgz
export TARBALL_URL=$DEPLOYER/$TARBALL
export RSA_KEY_URL=$DEPLOYER/id_rsa.pub.atria

SECURITY="-Djava.security.policy=$WEC_ROOT/config/.index.policy"
JMX_PORT=-Dcom.sun.management.jmxremote.port=8469
JMX_SSL=-Dcom.sun.management.jmxremote.ssl=false
JMX_ACCESS=-Dcom.sun.management.jmxremote.access.file=$WEC_ROOT/config/jmx/jmxremote.access
JMX_PASS=-Dcom.sun.management.jmxremote.password.file=$WEC_ROOT/config/jmx/jmx.password
JMX="$JMX_PORT $JMX_SSL $JMX_PASS $JMX_ACCESS";

export EMPLOYER_ARGS="-server -Xmx512m $SECURITY $JMX"
export CONSUMER_ARGS="-server -Xmx2048m $SECURITY"
export WORKER_ARGS="-client -Xmx256m -Dworker.role=sleeper"          # Sleeper Agent - low priority
#export WORKER_ARGS="-server -Xmx512m -Dworker.role=dedicated"       # Dedicated Worker - max priority and ram indexing
#export DEDICATED=1                                                    # Set dedicated to anything to enable dedicated worker

# Slave info
export SLAVES="hadoop@mirfak hadoop@wezen hadoop@dubhe hadoop@eon hadoop@thulcandra hadoop@liambs hadoop@dastardly hadoop@ganymede hadoop@jhlj-linux hadoop@pc031 hadoop@pc134 hadoop@pc167 hadoop@pc913 hadoop@pc934 hadoop@pc975 hadoop@pc976 hadoop@pc977 hadoop@pc980 hadoop@pc981 hadoop@pc982 hadoop@pc983 hadoop@pc984 hadoop@pc985 hadoop@pc987 hadoop@pc990 hadoop@pc992"



