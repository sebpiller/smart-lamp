FROM raspbian/stretch
MAINTAINER me@sebpiller.ch
LABEL vendor="S. Piller"
LABEL version="1.0-SNAP"
LABEL description="Connector to bind a Raspberry Pi 4 to a Luke Roberts Lamp F using bluetooth."

RUN apt update -y && \
    apt install -y bluez bluetooth blueman && \
    apt install -y default-jre-headless

RUN service bluetooth status

# copy only the artifacts we need from the first stage and discard the rest
COPY target/*-jar-with-dependencies.jar /luke-roberts/app/connector.jar
COPY src /luke-roberts/start/docker-entrypoint.sh

VOLUME /luke-roberts

ENTRYPOINT ["/luke-roberts/start/docker-entrypoint.sh"]
