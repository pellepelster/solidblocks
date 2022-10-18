FROM amazoncorretto:17.0.4-alpine3.16

ARG VERSION

ENV USER=debug
ENV UID=10000

ENV GROUP=${USER}
ENV GID=10000

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
    jq \
    bash \
    curl \
    util-linux

RUN chown -R ${USER}:${USER} ${USER}

USER ${USER}

EXPOSE 8080
WORKDIR ${USER}

COPY build/libs/solidblocks-debug-container-${VERSION}.jar /debug/solidblocks-debug-container.jar

CMD [ "java" , "-jar", "solidblocks-debug-container.jar" ]