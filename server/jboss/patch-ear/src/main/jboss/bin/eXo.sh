BONITA_OPTS="-Dorg.ow2.bonita.environment=../server/default/conf/bonita.environnement.xml"

#EXO_CONFIG_OPTS="-Dorg.exoplatform.container.configuration.debug"
#JPDA_TRANSPORT=dt_socket
#JPDA_ADDRESS=8000
#REMOTE_DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
LOGGING_OPTS="-Djava.util.logging.config.file=../server/default/conf/logging.properties"
JAVA_OPTS="$JAVA_OPTS $BONITA_OPTS $LOGGING_OPTS"

export JAVA_OPTS
exec "$PRGDIR"./run.sh "$@"
