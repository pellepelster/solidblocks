FROM alpine:3.16.2

ENV MINIO_CHECKSUM=114a1b68ce730580640532cb84231596c84a76cf0a6636e38169c636bbcb954d
ENV MINIO_MC_CHECKSUM=f3ad04a3a103a7d3781b66f1f34a1215f2560fbb5a9e49070fe95ef3b11bc820

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
