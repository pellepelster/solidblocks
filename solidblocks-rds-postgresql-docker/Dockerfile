FROM debian:bookworm AS base

ENV POSTGRES_KEY_CHECKSUM="0144068502a1eddd2a0280ede10ef607d1ec592ce819940991203941564e8e76"

RUN apt-get update && \
    apt-get -y install curl ca-certificates locales && \
    install -d /usr/share/postgresql-common/pgdg && \
    curl -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc --fail https://www.postgresql.org/media/keys/ACCC4CF8.asc && \
    echo "${POSTGRES_KEY_CHECKSUM}  /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc" | sha256sum -c && \
    . /etc/os-release && \
    echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt $VERSION_CODENAME-pgdg main" > /etc/apt/sources.list.d/pgdg.list

COPY .temp/locale.gen /etc/locale.gen
RUN locale-gen && \
    update-locale LANG=en_US.UTF-8

FROM base AS build_base

RUN apt-get update && \
    apt-get -y install \
        git \
        make \
        gcc

FROM build_base AS builder
ARG POSTGRES_VERSION=""

#############
# pg_remote #
#############
RUN apt-get install -y postgresql-server-dev-${POSTGRES_VERSION}
RUN git clone https://github.com/cybertec-postgresql/pg_remote_exec.git && cd pg_remote_exec && make install

###########
# pgaudit #
###########
RUN apt-get install -y libkrb5-dev && \
    git clone https://github.com/pgaudit/pgaudit.git && \
    cd pgaudit &&  \
    git checkout REL_${POSTGRES_VERSION}_STABLE && \
    make install USE_PGXS=1

FROM build_base AS builder_previous
ARG POSTGRES_PREVIOUS_VERSION=""
ARG PREVIOUS_VERSION_PGAUDIT_BRANCH=""

#############
# pg_remote #
#############
RUN apt-get install -y postgresql-server-dev-${POSTGRES_PREVIOUS_VERSION}
RUN git clone https://github.com/cybertec-postgresql/pg_remote_exec.git && cd pg_remote_exec && make install

###########
# pgaudit    #
###########
RUN apt-get install -y libkrb5-dev && \
    git clone https://github.com/pgaudit/pgaudit.git && \
    cd pgaudit &&  \
    git checkout ${PREVIOUS_VERSION_PGAUDIT_BRANCH} && \
    make install USE_PGXS=1

FROM base

ARG POSTGRES_VERSION=""
ARG POSTGRES_PREVIOUS_VERSION=""
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

RUN addgroup --gid $GID $GROUP \
  && adduser $USER \
  --home /$USER \
  --comment "unprivileged application user" \
  --shell /bin/bash \
  --gid $GID \
  --uid $UID

RUN apt-get update && \
    apt-get -y install \
        postgresql-${POSTGRES_PREVIOUS_VERSION} \
        postgresql-${POSTGRES_VERSION} \
        postgresql-${POSTGRES_VERSION}-cron \
        jq \
        bash \
        coreutils \
        openssh-client \
        curl \
        pgbackrest \
        util-linux \
        ripgrep \
        uuid-runtime \
        netcat-traditional

RUN chown -R ${USER}:${USER} ${USER}
RUN mkdir -p /run/postgresql/ && chown -R ${USER}:${USER} /run/postgresql/

COPY install/ /tmp/install/
RUN /tmp/install/install_gomplate.sh && rm -rf /tmp/install/

COPY --from=builder /usr/lib/postgresql/${POSTGRES_VERSION}/lib/pg_remote_exec.so /usr/lib/postgresql/${POSTGRES_VERSION}/lib/
COPY --from=builder /usr/lib/postgresql/${POSTGRES_VERSION}/lib/bitcode/pg_remote_exec /usr/lib/postgresql/${POSTGRES_VERSION}/lib/bitcode/pg_remote_exec
COPY --from=builder /usr/lib/postgresql/${POSTGRES_VERSION}/lib/bitcode/pg_remote_exec.* /usr/lib/postgresql/${POSTGRES_VERSION}/lib/bitcode/
COPY --from=builder /usr/share/postgresql/${POSTGRES_VERSION}/extension/pg_remote_exec* /usr/share/postgresql/${POSTGRES_VERSION}/extension/
COPY --from=builder /usr/share/doc/postgresql-doc-${POSTGRES_VERSION}/extension/pg_remote_exec* /usr/share/doc/postgresql-doc-${POSTGRES_VERSION}/extension/

COPY --from=builder /usr/lib/postgresql/${POSTGRES_VERSION}/lib/pgaudit.so /usr/lib/postgresql/${POSTGRES_VERSION}/lib/
COPY --from=builder /usr/share/postgresql/${POSTGRES_VERSION}/extension/pgaudit* /usr/share/postgresql/${POSTGRES_VERSION}/extension/
COPY --from=builder /usr/lib/postgresql/${POSTGRES_VERSION}/lib/bitcode/pgaudit* /usr/lib/postgresql/${POSTGRES_VERSION}/lib/bitcode/

COPY --from=builder_previous /usr/lib/postgresql/${POSTGRES_PREVIOUS_VERSION}/lib/pg_remote_exec.so /usr/lib/postgresql/${POSTGRES_PREVIOUS_VERSION}/lib/
COPY --from=builder_previous /usr/lib/postgresql/${POSTGRES_PREVIOUS_VERSION}/lib/bitcode/pg_remote_exec /usr/lib/postgresql/${POSTGRES_PREVIOUS_VERSION}/lib/bitcode/pg_remote_exec
COPY --from=builder_previous /usr/lib/postgresql/${POSTGRES_PREVIOUS_VERSION}/lib/bitcode/pg_remote_exec.* /usr/lib/postgresql/${POSTGRES_PREVIOUS_VERSION}/lib/bitcode/
COPY --from=builder_previous /usr/share/postgresql/${POSTGRES_PREVIOUS_VERSION}/extension/pg_remote_exec* /usr/share/postgresql/${POSTGRES_PREVIOUS_VERSION}/extension/
COPY --from=builder_previous /usr/share/doc/postgresql-doc-${POSTGRES_PREVIOUS_VERSION}/extension/pg_remote_exec* /usr/share/doc/postgresql-doc-${POSTGRES_PREVIOUS_VERSION}/extension/

USER ${USER}

EXPOSE 5432
WORKDIR ${USER}

COPY --chown=$USER:$USER templates/ templates/
COPY --chown=$USER:$USER bin/ bin/

RUN mkdir log
RUN mkdir socket
RUN mkdir run
RUN mkdir -p pgbackrest/spool

ENV PATH=/rds/bin:/usr/lib/postgresql/${POSTGRES_VERSION}/bin:$PATH
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

ENTRYPOINT [ "/rds/bin/run.sh" ]