FROM alpine:3.18.2

ARG POSTGRES_VERSION=""
ENV POSTGRES_STOP_TIMEOUT="60"

ENV DB_BACKUP_S3=0
ENV DB_BACKUP_LOCAL=0
ENV DB_BACKUP_GCS=0

ENV USER=rds
ENV UID=10000

ENV GROUP=${USER}
ENV GID=10000

ENV DATA_DIR=/storage/data

RUN addgroup -g $GID $GROUP \
  && adduser $USER \
  -h /$USER \
  -D \
  -s /bin/bash \
  -g "unprivileged application user" \
  -G $GROUP \
  -u $UID

ENV LANG en_US.utf8

RUN apk upgrade --available
RUN apk add \
    postgresql14 \
    postgresql14-contrib \
    postgresql15 \
    postgresql15-contrib \
    jq \
    bash \
    openssh-client \
    curl \
    pgbackrest \
    util-linux \
    musl-locales \
    ripgrep

RUN pg_versions set-default ${POSTGRES_VERSION}

RUN chown -R ${USER}:${USER} ${USER}

RUN mkdir -p /run/postgresql/ && chown -R ${USER}:${USER} /run/postgresql/

COPY install/ /tmp/install/
RUN /tmp/install/install_gomplate.sh && rm -rf /tmp/install/

USER ${USER}

EXPOSE 5432
WORKDIR ${USER}

COPY --chown=$USER:$USER templates/ templates/
COPY --chown=$USER:$USER bin/ bin/

RUN mkdir log
RUN mkdir socket
RUN mkdir -p pgbackrest/spool

ENV PATH=/rds/bin:$PATH

ENTRYPOINT [ "/rds/bin/run.sh" ]