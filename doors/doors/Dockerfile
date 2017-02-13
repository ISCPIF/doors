
#FROM openjdk:alpine
FROM gafiatulin/alpine-sbt

RUN apk update && apk add bash git sudo
RUN adduser doors -g "" -D -h /var/doors/

USER doors

<<<<<<< HEAD
RUN cd /var/doors/ && git clone https://github.com/ISCPIF/doors.git
=======
RUN cd /var/doors/ && \
    git clone https://github.com/ISCPIF/doors.git

>>>>>>> e97928c77c433c26e6d854a49b2d992b15efb6b0
RUN cd /var/doors/doors/application/ && sbt compile

COPY LabSettings.scala /var/doors/doors/application/lab/src/main/scala/fr/iscpif/doors/lab/LabSettings.scala

USER root
WORKDIR /var/doors/doors/application/
<<<<<<< HEAD

CMD chown -R doors /var/doors/ && sudo -u doors sbt "project server" "run-main fr.iscpif.doors.server.Doors -s /var/doors/.doors/settings"

=======
CMD chown -R doors /var/doors/ && sudo -u doors sbt "project lab" "run-main fr.iscpif.doors.lab.LabSettings"
>>>>>>> e97928c77c433c26e6d854a49b2d992b15efb6b0
