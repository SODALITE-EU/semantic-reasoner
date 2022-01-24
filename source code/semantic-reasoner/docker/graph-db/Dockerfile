FROM adoptopenjdk/openjdk11:alpine

# Build time arguments
ARG version=9.7.0
ARG edition=free

ENV GRAPHDB_PARENT_DIR=/opt/graphdb
ENV GRAPHDB_HOME=${GRAPHDB_PARENT_DIR}/home

ENV GRAPHDB_INSTALL_DIR=${GRAPHDB_PARENT_DIR}/dist

ADD http://160.40.52.200:8084/graph-db/graphdb-free-${version}-dist.zip /tmp

RUN apk add --no-cache bash util-linux procps net-tools busybox-extras wget less && \
    mkdir -p ${GRAPHDB_PARENT_DIR} && \
    cd ${GRAPHDB_PARENT_DIR} && \
    unzip /tmp/graphdb-free-${version}-dist.zip && \
    rm /tmp/graphdb-free-${version}-dist.zip && \
    mv graphdb-${edition}-${version} dist && \
    mkdir -p ${GRAPHDB_HOME}

ENV PATH=${GRAPHDB_INSTALL_DIR}/bin:$PATH

# Added 10485760 threshold because of Insufficient heap memory error
#CMD ["-Dgraphdb.home=/opt/graphdb/home -Ddefault.min.distinct.threshold=10485760 -Djava.security.egd=file:/dev/./urandom -Xmx10G"]
CMD ["-Dgraphdb.home=/opt/graphdb/home -Ddefault.min.distinct.threshold=10485760 -Djava.security.egd=file:/dev/./urandom"]
ENTRYPOINT ["/opt/graphdb/dist/bin/graphdb"]

EXPOSE 7200
