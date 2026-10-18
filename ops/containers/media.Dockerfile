FROM eclipse-temurin:21.0.12.1_1-jre-jammy@sha256:f04fb34e053148344e83317976114ec3f37e4b830ec8bdab5a2fe3cecd7d010b AS codec
USER root
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl build-essential meson ninja-build pkg-config \
    libglib2.0-dev libexpat1-dev libjpeg-turbo8-dev libpng-dev libwebp-dev && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /codec
RUN curl --fail --location --retry 3 \
    https://github.com/libvips/libvips/releases/download/v8.18.7/vips-8.18.7.tar.xz -o vips.tar.xz && \
    echo '5baaead3b0bb20ffdb9e9ff09aa9fda08620923df77b63b436654cb5e0b3bf94  vips.tar.xz' | sha256sum -c - && \
    tar -xf vips.tar.xz && \
    meson setup vips-8.18.7/build vips-8.18.7 --buildtype=release --prefix=/usr/local \
      -Dintrospection=disabled -Dmodules=disabled && \
    meson compile -C vips-8.18.7/build && meson install -C vips-8.18.7/build
COPY services/media/src/main/native/image-header.c /codec/image-header.c
RUN PKG_CONFIG_PATH=/usr/local/lib/x86_64-linux-gnu/pkgconfig:/usr/local/lib/aarch64-linux-gnu/pkgconfig \
    cc image-header.c -o /usr/local/bin/spoolin-image-header $(PKG_CONFIG_PATH=/usr/local/lib/x86_64-linux-gnu/pkgconfig:/usr/local/lib/aarch64-linux-gnu/pkgconfig pkg-config --cflags --libs vips)

FROM eclipse-temurin:21.0.12.1_1-jre-jammy@sha256:f04fb34e053148344e83317976114ec3f37e4b830ec8bdab5a2fe3cecd7d010b
RUN apt-get update && apt-get install -y --no-install-recommends \
    bubblewrap util-linux libglib2.0-0 libexpat1 libjpeg-turbo8 libpng16-16 libwebp7 libwebpmux3 libwebpdemux2 && \
    rm -rf /var/lib/apt/lists/*
COPY --from=codec /usr/local /usr/local
RUN ldconfig && /usr/local/bin/vips --version | grep -Fx 'vips-8.18.7'
WORKDIR /app
COPY services/media/build/libs/*-boot.jar /app/service.jar
RUN mkdir -p /var/spoolin/media && chown 10001:10001 /var/spoolin/media
USER 10001:10001
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-jar", "/app/service.jar"]
