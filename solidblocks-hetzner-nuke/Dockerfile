FROM alpine:3.18.2

ENV USER=nuker
ENV UID=10000

ENV GROUP=${USER}
ENV GID=10000

RUN  apk update  && \
     apk upgrade && \
     apk add openjdk17-jre-headless

RUN addgroup -g $GID $GROUP \
  && adduser $USER \
  -h /$USER \
  -D \
  -s /bin/bash \
  -g "unprivileged application user" \
  -G $GROUP \
  -u $UID

ENV LANG en_US.utf8

USER ${USER}
WORKDIR ${USER}

ADD nuker/build/distributions/nuker.zip nuker.zip
RUN unzip nuker.zip && \
    rm -rf nuker.zip

ENTRYPOINT [ "/nuker/nuker/bin/nuker" ]