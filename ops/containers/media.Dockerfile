FROM eclipse-temurin:21.0.12.1_1-jre-jammy@sha256:f04fb34e053148344e83317976114ec3f37e4b830ec8bdab5a2fe3cecd7d010b AS codec
USER root
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl build-essential meson ninja-build pkg-config \
    libglib2.0-dev libexpat1-dev libexif-dev libjpeg-turbo8-dev libpng-dev libwebp-dev \
    libcap-dev libselinux1-dev clang-14 && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /codec
RUN curl --fail --location --retry 3 \
    https://github.com/libvips/libvips/releases/download/v8.18.7/vips-8.18.7.tar.xz -o vips.tar.xz && \
    echo '5baaead3b0bb20ffdb9e9ff09aa9fda08620923df77b63b436654cb5e0b3bf94  vips.tar.xz' | sha256sum -c - && \
    tar -xf vips.tar.xz && \
    meson setup vips-8.18.7/build vips-8.18.7 --buildtype=release --prefix=/usr/local \
      -Dintrospection=disabled -Dmodules=disabled && \
    meson compile -C vips-8.18.7/build && meson install -C vips-8.18.7/build
# Upstream v0.11.2 release asset and checksum: https://github.com/containers/bubblewrap/releases/tag/v0.11.2
# Build without setuid support; the production process must create unprivileged user namespaces.
# The tagged upstream CI also builds with Clang and these CFLAGS, retaining its -Werror security diagnostics.
# https://github.com/containers/bubblewrap/blob/v0.11.2/.github/workflows/check.yml
RUN curl --fail --location --retry 3 \
    https://github.com/containers/bubblewrap/releases/download/v0.11.2/bubblewrap-0.11.2.tar.xz -o bubblewrap.tar.xz && \
    echo '69abc30005d2186baf7737feacd8da35633b93cf5af38838ecff17c5f8e924f6  bubblewrap.tar.xz' | sha256sum -c - && \
    tar -xf bubblewrap.tar.xz && \
    /usr/bin/clang-14 --version && \
    dpkg-query -W clang-14 && \
    CC=/usr/bin/clang-14 CFLAGS='-O2 -Werror=unused-variable' \
      meson setup bubblewrap-0.11.2/build bubblewrap-0.11.2 --buildtype=release --prefix=/opt/bubblewrap \
      -Dsupport_setuid=false -Drequire_userns=true -Dselinux=enabled \
      -Dman=disabled -Dbash_completion=disabled -Dzsh_completion=disabled && \
    meson compile -C bubblewrap-0.11.2/build && meson install -C bubblewrap-0.11.2/build
COPY services/media/src/main/native/image-header.c /codec/image-header.c
RUN PKG_CONFIG_PATH=/usr/local/lib/x86_64-linux-gnu/pkgconfig:/usr/local/lib/aarch64-linux-gnu/pkgconfig \
    cc image-header.c -o /usr/local/bin/spoolin-image-header $(PKG_CONFIG_PATH=/usr/local/lib/x86_64-linux-gnu/pkgconfig:/usr/local/lib/aarch64-linux-gnu/pkgconfig pkg-config --cflags --libs vips)

FROM eclipse-temurin:21.0.12.1_1-jre-jammy@sha256:f04fb34e053148344e83317976114ec3f37e4b830ec8bdab5a2fe3cecd7d010b
RUN apt-get update && apt-get install -y --no-install-recommends \
    util-linux libcap2 libselinux1 libglib2.0-0 libexif12 libexpat1 libjpeg-turbo8 libpng16-16 \
    libwebp7 libwebpmux3 libwebpdemux2 && \
    rm -rf /var/lib/apt/lists/*
COPY --from=codec /usr/local /usr/local
COPY --from=codec /opt/bubblewrap/bin/bwrap /usr/bin/bwrap
RUN test "$(stat -c '%u:%g:%a' /usr/bin/bwrap)" = '0:0:755' && \
    test ! -u /usr/bin/bwrap && test ! -g /usr/bin/bwrap && \
    /usr/bin/bwrap --version | grep -Fx 'bubblewrap 0.11.2' && \
    /usr/bin/bwrap --help | grep -F -- '--size'
RUN ldconfig && /usr/local/bin/vips --version | grep -Fx 'vips-8.18.7' && \
    mkdir -p /lib64
WORKDIR /app
COPY services/media/build/libs/*-boot.jar /app/service.jar
RUN mkdir -p /var/spoolin/media /var/spoolin/seed-artwork && \
    chown 10001:10001 /var/spoolin/media /var/spoolin/seed-artwork
USER 10001:10001
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-jar", "/app/service.jar"]
