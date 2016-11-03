
#FROM openjdk:alpine
FROM gafiatulin/alpine-sbt

RUN apk update && apk add bash git sudo
RUN adduser doors -g "" -D -h /var/doors/

USER doors

RUN cd /var/doors/ && \
    git clone https://github.com/ISCPIF/doors.git

COPY LabSettings.scala /var/doors/doors/application/lab/src/main/scala/fr/iscpif/doors/lab/LabSettings.scala

RUN cd /var/doors/doors/application/ && sbt compile

USER root
WORKDIR /var/doors/doors/application/
CMD chown -R doors /var/doors/ && sudo -u doors sbt "project lab" "run-main fr.iscpif.doors.lab.LabSettings"

