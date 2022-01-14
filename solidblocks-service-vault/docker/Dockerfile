FROM alpine:3.15 AS install

ENV VAULT_VERSION="1.9.1"
ENV VAULT_CHECKSUM="90fd702db924b55093668a55693d21fd62aa006dcf77d83ba9eaee9383085893"

RUN apk update && \
  apk add --no-cache curl unzip

WORKDIR /tmp

RUN curl https://releases.hashicorp.com/vault/${VAULT_VERSION}/vault_${VAULT_VERSION}_linux_amd64.zip -o vault.zip && \
echo "${VAULT_CHECKSUM}  vault.zip" | sha256sum -c && \
unzip vault.zip

FROM alpine:3.15

ENV USER_ID=4000
ENV GROUP_ID=4000

RUN apk update && \
  apk add --no-cache libcap

RUN addgroup -g ${GROUP_ID} vault && \
    adduser --uid ${USER_ID} -S -G vault vault

COPY --from=install /tmp/vault /solidblocks/bin/vault
RUN setcap cap_ipc_lock=+ep /solidblocks/bin/vault

USER vault

ENTRYPOINT ["/solidblocks/bin/vault"]
CMD ["server", "-config",  "/solidblocks/config/vault.json"]