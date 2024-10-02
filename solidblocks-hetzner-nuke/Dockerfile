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

ADD build/distributions/solidblocks-hetzner-nuke.zip solidblocks-hetzner-nuke.zip
RUN unzip solidblocks-hetzner-nuke.zip && \
    rm -rf solidblocks-hetzner-nuke.zip

ENTRYPOINT [ "/solidblocks/solidblocks-hetzner-nuke/bin/solidblocks-hetzner-nuke" ]