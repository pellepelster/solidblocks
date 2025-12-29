FROM alpine:3.18.2

COPY sshd_config /sshd_config

RUN apk add openssh
RUN ssh-keygen -t ed25519 -f /etc/ssh/ssh_host_ed25519_key -N ""


RUN mkdir /root/.ssh
COPY test_ed25519.key.pub /root/.ssh/authorized_keys

ENTRYPOINT ["/usr/sbin/sshd"]
CMD ["-D", "-e", "-f", "/sshd_config"]