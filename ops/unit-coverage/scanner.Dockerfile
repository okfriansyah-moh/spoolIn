# Only the portable Java libraries are copied; the bundled amd64 JRE is never run.
FROM --platform=linux/amd64 sonarsource/sonar-scanner-cli:12.3.0.4285_8.1.0@sha256:47956b669bf486bdad919e1cbd0533223515aa0b6d1d31763e5a3754942df6d7 AS distribution
FROM node:24.21.0-bookworm-slim@sha256:d6aa754f16b3197301076f047b5def2f02ea1dbbc2ca920407d46d7ec7f87b20 AS node
FROM eclipse-temurin:21.0.12.1_1-jdk-jammy@sha256:742daacb9316be4b6fc60258f6e4ac2a11882f6c597373c92290a57bfad0d2e0
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=distribution /opt/sonar-scanner/lib/ /opt/sonar-scanner/lib/
COPY --from=distribution /opt/sonar-scanner/conf/ /opt/sonar-scanner/conf/
WORKDIR /workspace
ENTRYPOINT ["java", "-Dscanner.home=/opt/sonar-scanner", "-Dproject.home=/workspace", "-cp", "/opt/sonar-scanner/lib/*", "org.sonarsource.scanner.cli.Main", "-Dsonar.scanner.skipJreProvisioning=true"]
