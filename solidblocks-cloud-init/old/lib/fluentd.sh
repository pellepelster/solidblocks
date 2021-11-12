function fluentd_config() {
    local cloud_name
    cloud_name="$(config '.cloud_name')"
cat <<-EOF
<source>
  @type systemd
  tag systemd
  path /run/log/journal
  matches [
    { "_SYSTEMD_UNIT": "solidblocks-management.service" },
    { "_SYSTEMD_UNIT": "consul-server.service" },
    { "_SYSTEMD_UNIT": "consul-agent.service" }
  ]
  read_from_head false

  <storage>
    @type local
  </storage>

  <entry>
    fields_strip_underscores true
    fields_lowercase true
  </entry>
</source>

<filter *>
  @type record_transformer
  <record>
    hostname "#{Socket.gethostname}"
    cloud_name "${cloud_name}"
  </record>
</filter>

<match **>
  @type logzio_buffered
  endpoint_url https://listener.logz.io:8071?token=QxOtjGifJZftQUmkgoLNdZlYhLBvOoCZ&type=my_type
  output_include_time true
  output_include_tags true
  http_idle_timeout 10
  <buffer>
      @type memory
      flush_thread_count 4
      flush_interval 3s
      chunk_limit_size 16m      # Logz.io bulk limit is decoupled from chunk_limit_size. Set whatever you want.
      queue_limit_length 4096
  </buffer>
</match>

<system>
  root_dir /var/log/fluentd
</system>
EOF
}

function fluentd_systemd_config() {
cat <<-EOF
[Unit]
Description=Fluentd
Documentation=http://www.fluentd.org/
After=network.target

[Service]
Type=forking
ExecStart=/usr/local/bin/fluentd -d /var/run/fluentd.pid -c /etc/fluentd/fluentd.conf
PIDFile=/var/run/fluentd.pid
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
}

function fluentd_bootstrap() {
    check_and_install "ruby"
    check_and_install "ruby-dev"
    check_and_install "build-essential"

    gem install rake --no-ri --no-rdoc
    gem install fluent --no-ri --no-rdoc
    gem install fluent-plugin-logzio --no-ri --no-rdoc
    gem install fluent-plugin-systemd --no-ri --no-rdoc

    mkdir -p /etc/fluentd
    fluentd_config > /etc/fluentd/fluentd.conf
    fluentd_systemd_config > /etc/systemd/system/fluentd.service

    systemctl daemon-reload

    systemctl enable fluentd
    systemctl restart fluentd

}
