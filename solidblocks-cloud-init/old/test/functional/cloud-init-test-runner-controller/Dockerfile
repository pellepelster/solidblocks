FROM jrei/systemd-debian:9
MAINTAINER pelle@pelle.io

RUN apt-get update && apt-get install --no-install-recommends -qq -y zlib1g-dev ca-certificates

ADD cloud-init.sh /cloud-init.sh
CMD chmod +x /cloud-init.sh

ADD idle.sh /idle.sh
CMD chmod +x /idle.sh

ENTRYPOINT [ "/idle.sh" ]
