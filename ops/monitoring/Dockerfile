FROM prom/prometheus:v3.15.0@sha256:efd719c99d83b060d9daefdcf00360461adf279f45ef5391f8d111892118753e AS prometheus
USER 0
RUN chown 10001:10001 /prometheus
USER 10001:10001

FROM prom/alertmanager:v0.34.1@sha256:e9733bafb1bdef9b00e25a21f8f99dc26a22224bf16641ad754d1649f4c3357a AS alertmanager
USER 0
RUN chown 10001:10001 /alertmanager
USER 10001:10001

FROM grafana/grafana:13.2.3@sha256:b28bae15e219c998fb0e0424ed724930cc61b1f61fb404d47c862f9a23f9e572 AS grafana
USER 0
RUN chown -R 10001:10001 /var/lib/grafana
USER 10001:10001

FROM busybox:1.37.0-musl@sha256:5cec3fc171c87218698e85a52af7087de727372aae264a787b8112901a5b0092 AS monitoring-tools
RUN mkdir -p /loki && chown 10001:10001 /loki

FROM grafana/loki:3.7.0@sha256:c316b7c7589a5eeca843b6926c7446149d18300b79ac8538dc4ae063bc478da2 AS loki
COPY --from=monitoring-tools --chown=10001:10001 /loki /loki
COPY --from=monitoring-tools /bin/busybox /bin/busybox
USER 10001:10001

FROM grafana/alloy:v1.16.1@sha256:51aeb9d829239345070619dad3edd6873186f913c84f45b365b74574fcb38ec0 AS alloy
COPY --from=monitoring-tools /bin/busybox /bin/busybox
USER 0
RUN mkdir -p /var/lib/alloy && chown -R 10001:10001 /var/lib/alloy
USER 10001:10001
