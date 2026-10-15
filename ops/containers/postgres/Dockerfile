FROM postgres:18.6-bookworm@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c
COPY ops/containers/postgres/entrypoint.sh /opt/spoolin/entrypoint.sh
COPY ops/containers/postgres/init.sh /docker-entrypoint-initdb.d/10-owner-bootstrap.sh
COPY ops/containers/postgres/pg_hba.conf /etc/postgresql/pg_hba.conf
COPY ops/postgres/bootstrap.sql /opt/spoolin/bootstrap.sql
COPY ops/containers/postgres/membership.sh /opt/spoolin/membership.sh
COPY ops/postgres/grants.sql /opt/spoolin/grants.sql
ENTRYPOINT ["/opt/spoolin/entrypoint.sh"]
CMD ["postgres", "-c", "ssl=on", "-c", "ssl_cert_file=/run/spoolin/server.crt", "-c", "ssl_key_file=/run/spoolin/server.key", "-c", "hba_file=/etc/postgresql/pg_hba.conf", "-c", "max_connections=50", "-c", "shared_buffers=256MB", "-c", "work_mem=4MB"]
