FROM alpine:3.20.2

ENV USER=solidblocks
ENV UID=10000

ENV GROUP=${USER}
ENV GID=10000

RUN  apk update  && \
     apk upgrade && \
     apk add openjdk21-jre-headless

RUN addgroup -g $GID $GROUP \
  && adduser $USER \
  -h /$USER \
  -D \
  -s /bin/bash \
  -g "unprivileged application user" \
  -G $GROUP \
  -u $UID

ENV LANG=en_US.utf8

USER ${USER}
WORKDIR ${USER}

ADD build/distributions/solidblocks-terraform.zip solidblocks-terraform.zip
RUN unzip solidblocks-terraform.zip && \
    rm -rf solidblocks-terraform.zip

ENTRYPOINT [ "/solidblocks/solidblocks-terraform/bin/solidblocks-terraform" ]