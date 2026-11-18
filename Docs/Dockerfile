# Headless Tectonicus image: renders with EGL through Mesa, no X server or GPU needed.
#
# Build (put the Tectonicus jar next to this file as Tectonicus.jar, e.g. from build/libs/):
#   docker build -t tectonicus .
#
# Run (paths in the config must be the container paths below, and the config needs
# <rasteriser type="lwjgl" useEGL="true"/> and an explicit minecraftJar):
#   docker run --rm \
#     -v /path/to/world:/world:ro \
#     -v /path/to/minecraft/versions/<version>/<version>.jar:/mc/client.jar:ro \
#     -v /path/to/config-dir:/config:ro \
#     -v /path/to/output:/output \
#     tectonicus -c /config/tectonicus.xml
#
# Without a GPU passed to the container, Mesa renders on the CPU (llvmpipe).
# gcompat and libstdc++ let LWJGL's glibc-built natives load on Alpine's musl.
# fontconfig and ttf-dejavu are for Java's headless image code.

FROM alpine:3.24
RUN apk add --no-cache openjdk21-jre-headless mesa-egl mesa-gl mesa-dri-gallium gcompat libstdc++ fontconfig ttf-dejavu
WORKDIR /app
COPY Tectonicus.jar /app/Tectonicus.jar
ENTRYPOINT ["java", "-jar", "/app/Tectonicus.jar"]
CMD ["-c", "/config/tectonicus.xml"]
