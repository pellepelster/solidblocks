FROM alpine:3.15 AS install

ENV CADDY_VERSION="2.4.6"
ENV CADDY_CHECKSUM="690ad64538a39d555294cd09b26bb22ade36abc0e3212342f0ed151de51ec128"

RUN apk update && \
  apk add --no-cache curl unzip

WORKDIR /tmp

RUN curl -L https://github.com/caddyserver/caddy/releases/download/v${CADDY_VERSION}/caddy_${CADDY_VERSION}_linux_amd64.tar.gz -o caddy.tar.gz && \
echo "${CADDY_CHECKSUM}  caddy.tar.gz" | sha256sum -c && \
tar -xvf caddy.tar.gz

FROM alpine:3.15

ENV USER_ID=4000
ENV GROUP_ID=4000

RUN apk update && \
  apk add --no-cache libcap

RUN addgroup -g ${GROUP_ID} caddy && \
    adduser --uid ${USER_ID} -S -G caddy caddy

COPY --from=install /tmp/caddy /solidblocks/bin/caddy

USER caddy

ENTRYPOINT ["/solidblocks/bin/caddy"]
CMD ["run", "-config", "/solidblocks/config/caddy.json", "-watch"]