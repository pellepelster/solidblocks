FROM alpine:3.16.2

COPY sshd_config /sshd_config

RUN apk add openssh
RUN ssh-keygen -t ed25519 -f /etc/ssh/ssh_host_ed25519_key -N ""

ENTRYPOINT ["/usr/sbin/sshd"]
CMD ["-D", "-f", "/sshd_config"]