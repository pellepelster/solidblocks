FROM amazonlinux:2

RUN yum update -y
RUN yum install -y \
    zip \
    jq \
    curl \
    unzip \
    nc \
    zip \
    tar \
    util-linux \
    python \
    java-17-amazon-corretto-devel \
    bzip2
RUN amazon-linux-extras install docker -y