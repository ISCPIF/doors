
#FROM openjdk:alpine
FROM gafiatulin/alpine-sbt

RUN apk update && apk add bash git sudo
RUN adduser doors -g "" -D -h /var/doors/

USER doors

RUN cd /var/doors/ && git clone https://github.com/ISCPIF/doors.git
RUN cd /var/doors/doors/application/ && sbt compile

USER root
WORKDIR /var/doors/doors/application/

CMD chown -R doors /var/doors/ && sudo -u doors sbt "project server" "run-main fr.iscpif.doors.server.Doors -s /var/doors/.doors/settings"

