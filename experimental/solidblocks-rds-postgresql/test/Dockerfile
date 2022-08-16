FROM alpine:3.15

ENV GOMPLATE_CHECKSUM=603539aac4e09f98a8ca5b6e5da0c21213221206dc7175a5644255c7a22b936d
ENV GOMPLATE_VERSION=3.10.0

ENV USER=pgbackrest
ENV UID=10000

ENV GROUP=${USER}
ENV GID=10000

ENV LOCAL_STORAGE_DIR=/storage/local
ENV BACKUP_DIR=${LOCAL_STORAGE_DIR}/backup
ENV DATA_DIR=${LOCAL_STORAGE_DIR}/data

ENV LANG en_US.utf8

RUN apk upgrade --available
RUN apk add \
    openssh \
    jq \
    bash \
    shadow \
    curl \
    pgbackrest


RUN curl -L https://github.com/hairyhenderson/gomplate/releases/download/v${GOMPLATE_VERSION}/gomplate_linux-amd64-slim -o /usr/bin/gomplate && \
  echo "${GOMPLATE_CHECKSUM}  /usr/bin/gomplate" | sha256sum -c && \
  chmod +x /usr/bin/gomplate

RUN addgroup -g $GID $GROUP \
  && adduser $USER \
  -h /$USER \
  -D \
  -s /bin/bash \
  -G $GROUP \
  -u $UID
RUN usermod -p '*' $USER

#RUN chown -R ${USER}:${USER} /rds
#RUN apt-get purge -y ${BUILD_PACKAGES} && rm -rf /tmp/* && apt-get clean autoclean && apt-get autoremove --yes && rm -rf /var/lib/{apt,dpkg,cache,log}/

#RUN mkdir -p ${DATA_DIR} && chown -R ${USER}:${USER} ${DATA_DIR} && chmod -R 700 ${DATA_DIR}
RUN mkdir -p ${BACKUP_DIR} && chown -R ${USER}:${USER} ${BACKUP_DIR}

RUN mkdir -p ${USER} && chown -R ${USER}:${USER} ${USER}

RUN mkdir -p /etc/ssh

COPY config/sshd_config /pgbackrest/sshd_config
COPY bin/run.sh /pgbackrest/run.sh
COPY templates/ /pgbackrest/templates/

EXPOSE 22
ENTRYPOINT ["/pgbackrest/run.sh"]
