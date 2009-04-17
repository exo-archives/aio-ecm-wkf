BONITA_OPTS="-Dorg.ow2.bonita.environment=../server/default/conf/bonita.environnement.xml"

#EXO_CONFIG_OPTS="-Dorg.exoplatform.container.configuration.debug"
#JPDA_TRANSPORT=dt_socket
#JPDA_ADDRESS=8000
#REMOTE_DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"

JAVA_OPTS="$JAVA_OPTS $BONITA_OPTS"

export JAVA_OPTS
exec "$PRGDIR"./run.sh "$@"
