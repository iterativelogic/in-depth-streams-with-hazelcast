FROM hazelcast/hazelcast-jet:3.2

ENV JET_MC_VERSION 3.2
ENV JET_MC_HOME /opt/hazelcast-jet-management-center

ARG JET_MC_INSTALL_NAME="hazelcast-jet-management-center-${JET_MC_VERSION}"
ARG JET_MC_INSTALL_ZIP="${JET_MC_INSTALL_NAME}.zip"
ARG JET_MC_INSTALL_DIR="${JET_MC_HOME}/${JET_MC_INSTALL_NAME}"
ARG JET_MC_INSTALL_JAR="hazelcast-jet-management-center-${JET_MC_VERSION}.jar"

ENV JET_MC_RUNTIME "${JET_MC_INSTALL_DIR}/${JET_MC_INSTALL_JAR}"
ENV JET_MC_HTTP_PORT=8081

ENV MC_VERSION 3.12.7
ENV MC_HOME /opt/hazelcast-mancenter
ENV MC_DATA /data

ENV MC_HTTP_PORT 8080
ENV MC_HTTPS_PORT 8443
ENV MC_HEALTH_CHECK_PORT 8081
ENV MC_CONTEXT hazelcast-mancenter

ARG MC_INSTALL_NAME="hazelcast-management-center-${MC_VERSION}"
ARG MC_INSTALL_ZIP="${MC_INSTALL_NAME}.zip"
ARG MC_INSTALL_WAR="hazelcast-mancenter-${MC_VERSION}.war"

ENV MC_RUNTIME "${MC_HOME}/${MC_INSTALL_WAR}"

# Install curl to download Jet management center
RUN apk add --no-cache bash curl \
 && rm -rf /var/cache/apk/*

# Prepare Management Center
RUN wget -q -P /tmp https://download.hazelcast.com/hazelcast-jet-management-center/${JET_MC_INSTALL_ZIP} \
  && unzip /tmp/${JET_MC_INSTALL_ZIP} -x ${JET_MC_INSTALL_NAME}/manual/* -d /opt \
  && ln -s  /opt/${JET_MC_INSTALL_NAME} ${JET_MC_HOME} \
  && rm -rf /tmp/${JET_MC_INSTALL_ZIP}

# chmod allows running container as non-root with `docker run --user` option
RUN mkdir -p ${JET_MC_HOME}/lib \
 && chmod a+rwx ${JET_MC_HOME}

 RUN mkdir -p ${MC_HOME} ${MC_DATA} \
  && chmod a+rwx ${MC_HOME} ${MC_DATA}
 WORKDIR ${MC_HOME}

 # Prepare Management Center
 RUN wget -O ${MC_HOME}/${MC_INSTALL_ZIP} \
           http://download.hazelcast.com/management-center/${MC_INSTALL_ZIP} \
  && unzip ${MC_INSTALL_ZIP} \
       -x ${MC_INSTALL_NAME}/docs/* \
  && rm -rf ${MC_INSTALL_ZIP} \
  && mv ${MC_INSTALL_NAME}/* . \
  && rm -rf ${MC_INSTALL_NAME}

EXPOSE ${MC_HTTP_PORT}
EXPOSE ${MC_HTTPS_PORT}
EXPOSE ${MC_HEALTH_CHECK_PORT}
EXPOSE ${JET_MC_HTTP_PORT}
