FROM alpine:3.18.2

ENV MINIO_CHECKSUM="f467a01ac9cfcd07c7ab944c5ce32282a0685efa9b14c62141d6fba2a42ef676"
ENV MINIO_VERSION="RELEASE.2023-04-07T05-28-58Z"

ENV MINIO_MC_VERSION="RELEASE.2023-04-06T16-51-10Z"
ENV MINIO_MC_CHECKSUM="ec8f80a08224d410743d33ca711288b5888caebd566fb4a7f959a8ea3459f0a6"

ENV USER=minio
ENV UID=10000

ENV GROUP=${USER}
ENV GID=10000

ENV DATA_DIR=/storage/data

ENV LANG=en_US.utf8

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

RUN curl -L https://dl.min.io/server/minio/release/linux-amd64/archive/minio.${MINIO_VERSION} -o $USER/bin/minio && \
  echo "${MINIO_CHECKSUM}  $USER/bin/minio" | sha256sum -c && \
  chmod +x $USER/bin/minio

RUN curl -L https://dl.min.io/client/mc/release/linux-amd64/archive/mc.${MINIO_MC_VERSION} -o $USER/bin/mc && \
  echo "${MINIO_MC_CHECKSUM}  $USER/bin/mc" | sha256sum -c && \
  chmod +x $USER/bin/mc

COPY bin/run.sh /${USER}/bin/run.sh
COPY bin/provision.sh /${USER}/bin/provision.sh

EXPOSE ${MINIO_HTTPS_PORT}
EXPOSE 9001

ENTRYPOINT ["/minio/bin/run.sh"]
