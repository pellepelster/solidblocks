FROM debian:12

ENV DEBIAN_FRONTEND="noninteractive"

RUN apt-get update
RUN apt-get \
        -o Dpkg::Options::="--force-confnew" \
        --force-yes \
        -fuy \
        dist-upgrade

RUN apt-get install --no-install-recommends -qq -y \
    apt-utils \
    ca-certificates \
    zip \
    docker.io \
    jq \
    netcat-openbsd \
    curl \
    unzip \
    xz-utils \
    python3 \
    python3-venv \
    uuid-runtime \
    default-jdk \
    bzip2
