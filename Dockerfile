# syntax=docker/dockerfile:1

# Build a Linux image with GraalVM JDK 21 + Espresso and sbt, then run the server
# Works on macOS hosts via Docker (use buildx for arm64 -> linux/amd64 if needed)

FROM ubuntu:22.04 AS base

ENV DEBIAN_FRONTEND=noninteractive

# Install base tools and sbt
RUN apt-get update -y && \
    apt-get install -y --no-install-recommends \
      curl ca-certificates gnupg unzip zip bash git findutils coreutils && \
    rm -rf /var/lib/apt/lists/*

# Install sbt (using direct download method to avoid GPG key issues)
ARG SBT_VERSION=1.10.6
RUN curl -fsSL -o /tmp/sbt-${SBT_VERSION}.zip https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.zip && \
    unzip -q /tmp/sbt-${SBT_VERSION}.zip -d /opt && \
    rm /tmp/sbt-${SBT_VERSION}.zip && \
    ln -s /opt/sbt/bin/sbt /usr/local/bin/sbt && \
    chmod +x /usr/local/bin/sbt

# Install GraalVM Community JDK 21 (Linux x64) and Espresso
ARG GRAALVM_VERSION=21.0.2
ARG GRAALVM_DIST=graalvm-community-jdk-21.0.2_linux-x64_bin.tar.gz
ARG GRAALVM_URL=https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-${GRAALVM_VERSION}/${GRAALVM_DIST}

RUN mkdir -p /opt && \
    curl -fsSL -o /tmp/graalvm.tar.gz ${GRAALVM_URL} && \
    tar -C /opt -xzf /tmp/graalvm.tar.gz && \
    rm /tmp/graalvm.tar.gz && \
    ln -s /opt/graalvm-community-openjdk-21.0.2+13.1 /opt/graalvm && \
    /opt/graalvm/bin/java -version

ENV JAVA_HOME=/opt/graalvm
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Note: Espresso is no longer available as a separate installable component in recent GraalVM versions
# The base GraalVM JDK 21 should be sufficient for Java execution
# Point guest runtime to Java 21 (same GraalVM by default)
ENV ESPRESSO_JAVA_HOME=${JAVA_HOME}

WORKDIR /app

# Pre-copy build metadata first to leverage Docker layer caching
COPY project ./project
COPY build.sbt ./build.sbt

# Fetch sbt dependencies (layer cache)
RUN sbt -no-colors update

# Copy the rest of the project
COPY . .

# Ensure build scripts are executable
RUN chmod +x ./build-instruments.sh || true

# Build instrument JARs and compile app
RUN ./build-instruments.sh && sbt -no-colors compile

EXPOSE 8080

# For Linux, Espresso supports native libraries, no extra flags needed.
CMD ["sbt", "--no-colors", "run"]

