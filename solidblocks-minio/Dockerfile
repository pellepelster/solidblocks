FROM alpine:3.15

ENV MINIO_CHECKSUM=b1d88285e80043fbef596b944cee829d21f853ec51438b8345f7e64bfe90562b
ENV MINIO_MC_CHECKSUM=21b409ad36e2c9384fc6ec5d4d320512420bab06a3ed986e15b8924ab32d7326

ENV USER=minio
ENV UID=10000

ENV GROUP=${USER}
ENV GID=10000

ENV DATA_DIR=/storage/data

ENV LANG en_US.utf8

ENV MINIO_HTTPS_PORT=443

RUN addgroup -g $GID $GROUP \
  && adduser $USER \
  -h /$USER \
  -D \
  -s /bin/bash \
  -g "unprivileged application user" \
  -G $GROUP \
  -u $UID

RUN apk upgrade --available
RUN apk add \
    jq \
    bash \
    curl

RUN mkdir -p ${DATA_DIR} && chown -R ${USER}:${USER} ${DATA_DIR} && chmod -R 700 ${DATA_DIR}
RUN chown -R ${USER}:${USER} ${USER}

USER ${USER}

RUN mkdir -p /${USER}/bin

RUN curl -L https://dl.min.io/server/minio/release/linux-amd64/minio -o $USER/bin/minio && \
  echo "${MINIO_CHECKSUM}  $USER/bin/minio" | sha256sum -c && \
  chmod +x $USER/bin/minio

RUN curl -L https://dl.min.io/client/mc/release/linux-amd64/mc -o $USER/bin/mc && \
  echo "${MINIO_MC_CHECKSUM}  $USER/bin/mc" | sha256sum -c && \
  chmod +x $USER/bin/mc

COPY bin/run.sh /${USER}/bin/run.sh
COPY bin/provision.sh /${USER}/bin/provision.sh

EXPOSE ${MINIO_HTTPS_PORT}
EXPOSE 9001

ENTRYPOINT ["/minio/bin/run.sh"]
