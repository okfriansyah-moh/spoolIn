FROM eclipse-temurin:21.0.12.1_1-jre-jammy@sha256:f04fb34e053148344e83317976114ec3f37e4b830ec8bdab5a2fe3cecd7d010b
WORKDIR /app
COPY services/core/build/libs/*-boot.jar /app/service.jar
COPY ops/containers/core-timing.jfc /app/core-timing.jfc
RUN mkdir -p /var/spoolin/timing && chown 10001:10001 /var/spoolin/timing && chmod 0700 /var/spoolin/timing
USER 10001:10001
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-jar", "/app/service.jar"]
