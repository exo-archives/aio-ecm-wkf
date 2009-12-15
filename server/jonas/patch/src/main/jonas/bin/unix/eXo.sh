#!/bin/sh

# Computes the absolute path of eXo
UNIX_DIR=`dirname "$0"`
JONAS_ROOT=$UNIX_DIR/../..

# Sets some variables
LOG_OPTS="-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog"
EXO_OPTS="-Dexo.webui.reloadable.template=true"
BONITA_HOME = $JONAS_ROOT/bonita
EXO_CONFIG_OPTS="-Dorg.exoplatform.container.configuration.debug"
export JAVA_OPTS="-Xshare:auto -Xms128m -Xmx512m -XX:MaxPermSize=256m $LOG_OPTS $EXO_OPTS $BONITA_HOME $EXO_CONFIG_OPTS"

# Launches the server
cd $UNIX_DIR
exec $UNIX_DIR/jonas "$@"
