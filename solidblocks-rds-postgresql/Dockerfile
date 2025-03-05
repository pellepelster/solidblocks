ARG ALPINE_VERSION="3.21.3"
ARG ALPINE_PREVIOUS_VERSION="3.21.3"

FROM alpine:${ALPINE_VERSION} AS builder
ARG POSTGRES_VERSION=""

RUN apk upgrade --available
RUN apk add \
    git \
    make \
    gcc \
    libc-dev \
    postgresql-dev

RUN git clone https://github.com/cybertec-postgresql/pg_remote_exec.git && cd pg_remote_exec && make install

FROM alpine:${ALPINE_PREVIOUS_VERSION} AS builder_previous
ARG POSTGRES_PREVIOUS_VERSION=""

RUN apk upgrade --available
RUN apk add \
    git \
    make \
    gcc \
    libc-dev \
    postgresql-dev

RUN git clone https://github.com/cybertec-postgresql/pg_remote_exec.git && cd pg_remote_exec && make install

FROM alpine:${ALPINE_VERSION}

ARG POSTGRES_VERSION=""
ARG POSTGRES_PREVIOUS_VERSION=""
ARG POSTGRES_PREVIOUS_VERSION_PATH=""
ENV POSTGRES_STOP_TIMEOUT="60"

ENV DB_BACKUP_S3=0
ENV DB_BACKUP_LOCAL=0
ENV DB_BACKUP_GCS=0

ENV DB_BACKUP_FULL_SCHEDULE=""
ENV DB_BACKUP_DIFF_SCHEDULE=""
ENV DB_BACKUP_INCR_SCHEDULE=""

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

ENV LANG=en_US.utf8

RUN apk upgrade --available
RUN apk add \
    postgresql${POSTGRES_PREVIOUS_VERSION} \
    postgresql${POSTGRES_PREVIOUS_VERSION}-contrib \
    postgresql${POSTGRES_VERSION} \
    postgresql${POSTGRES_VERSION}-contrib \
    postgresql-pg_cron \
    jq \
    bash \
    coreutils \
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

COPY --from=builder /usr/lib/postgresql${POSTGRES_VERSION}/pg_remote_exec.so /usr/lib/postgresql${POSTGRES_VERSION}/
COPY --from=builder /usr/lib/postgresql${POSTGRES_VERSION}/bitcode/pg_remote_exec /usr/lib/postgresql${POSTGRES_VERSION}/bitcode/pg_remote_exec
COPY --from=builder /usr/lib/postgresql${POSTGRES_VERSION}/bitcode/pg_remote_exec.* /usr/lib/postgresql${POSTGRES_VERSION}/bitcode/
COPY --from=builder /usr/share/postgresql${POSTGRES_VERSION}/extension/pg_remote_exec* /usr/share/postgresql${POSTGRES_VERSION}/extension/
COPY --from=builder /usr/share/doc/postgresql${POSTGRES_VERSION}/extension/pg_remote_exec* /usr/share/doc/postgresql${POSTGRES_VERSION}/extension/

COPY --from=builder_previous /usr/lib/postgresql${POSTGRES_PREVIOUS_VERSION_PATH}/pg_remote_exec.so /usr/lib/postgresql${POSTGRES_PREVIOUS_VERSION_PATH}/
COPY --from=builder_previous /usr/lib/postgresql${POSTGRES_PREVIOUS_VERSION_PATH}/bitcode/pg_remote_exec /usr/lib/postgresql${POSTGRES_PREVIOUS_VERSION_PATH}/bitcode/pg_remote_exec
COPY --from=builder_previous /usr/lib/postgresql${POSTGRES_PREVIOUS_VERSION_PATH}/bitcode/pg_remote_exec.* /usr/lib/postgresql${POSTGRES_PREVIOUS_VERSION_PATH}/bitcode/
COPY --from=builder_previous /usr/share/postgresql${POSTGRES_PREVIOUS_VERSION_PATH}/extension/pg_remote_exec* /usr/share/postgresql${POSTGRES_PREVIOUS_VERSION_PATH}/extension/
COPY --from=builder_previous /usr/share/doc/postgresql${POSTGRES_PREVIOUS_VERSION_PATH}/extension/pg_remote_exec* /usr/share/doc/postgresql${POSTGRES_PREVIOUS_VERSION_PATH}/extension/

USER ${USER}

EXPOSE 5432
WORKDIR ${USER}

COPY --chown=$USER:$USER templates/ templates/
COPY --chown=$USER:$USER bin/ bin/

RUN mkdir log
RUN mkdir socket
RUN mkdir run
RUN mkdir -p pgbackrest/spool

ENV PATH=/rds/bin:$PATH

ENTRYPOINT [ "/rds/bin/run.sh" ]