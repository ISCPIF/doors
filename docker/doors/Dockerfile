
#FROM openjdk:alpine
FROM gafiatulin/alpine-sbt
RUN apk update && apk add bash git
RUN adduser doors -g "" -D -h /var/doors/

RUN cd /var/doors/ && \
    git clone https://github.com/ISCPIF/doors.git && \
    cd doors/application 

COPY LabSettings.scala /var/doors/doors/application/lab/src/main/scala/fr/iscpif/doors/lab/LabSettings.scala

RUN cd /var/doors/doors/application/ && sbt compile

WORKDIR /var/doors/doors/application/
CMD sbt "project lab" "run-main fr.iscpif.doors.lab.LabSettings"

