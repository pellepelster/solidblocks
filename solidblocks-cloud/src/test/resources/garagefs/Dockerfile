FROM ubuntu:latest

RUN apt-get update && apt-get install -y supervisor openssh-server curl ca-certificates

COPY authorized_keys /root/.ssh/authorized_keys
COPY garage.toml /etc/garage.toml
RUN mkdir /run/sshd && chmod 0755 /run/sshd

RUN curl -L https://garagehq.deuxfleurs.fr/_releases/v2.2.0/x86_64-unknown-linux-musl/garage -o /garage && chmod +x /garage

RUN mkdir -p /var/log/supervisor
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
CMD ["/usr/bin/supervisord"]