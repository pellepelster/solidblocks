FROM alpine:3.15

ENV GOMPLATE_CHECKSUM=603539aac4e09f98a8ca5b6e5da0c21213221206dc7175a5644255c7a22b936d
ENV GOMPLATE_VERSION=3.10.0

ENV DB_BACKUP_S3=0
ENV DB_BACKUP_LOCAL=0

ENV USER=rds
ENV UID=10000

ENV GROUP=${USER}
ENV GID=10000

ENV DATA_DIR=/storage/data

ENV DB_ADMIN_USERNAME=${USER}

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
    postgresql \
    jq \
    bash \
    openssh-client \
    curl \
    pgbackrest \
    util-linux \
    musl-locales

RUN chown -R ${USER}:${USER} ${USER}

RUN mkdir -p /run/postgresql/ && chown -R ${USER}:${USER} /run/postgresql/

RUN curl -L https://github.com/hairyhenderson/gomplate/releases/download/v${GOMPLATE_VERSION}/gomplate_linux-amd64-slim -o /usr/bin/gomplate && \
  echo "${GOMPLATE_CHECKSUM}  /usr/bin/gomplate" | sha256sum -c && \
  chmod +x /usr/bin/gomplate

USER ${USER}

EXPOSE 5432
WORKDIR ${USER}

COPY --chown=$USER:$USER templates/ templates/
COPY --chown=$USER:$USER bin/ bin/

RUN mkdir log
RUN mkdir socket
RUN mkdir -p pgbackrest/spool

CMD [ "/rds/bin/run.sh" ]