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
