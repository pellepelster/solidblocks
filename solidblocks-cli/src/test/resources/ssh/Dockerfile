FROM alpine:3.18.2

RUN apk add openssh
RUN ssh-keygen -t ed25519 -f /etc/ssh/ssh_host_ed25519_key -N ""
RUN ssh-keygen -t rsa -f /etc/ssh/ssh_host_rsa_key -N ""

RUN mkdir /root/.ssh
COPY authorized_keys /root/.ssh/authorized_keys
RUN echo "LogLevel VERBOSE" > /etc/ssh/sshd_config

ENTRYPOINT ["/usr/sbin/sshd"]
CMD ["-D", "-e"]