#!/bin/bash -eu

DEBUG=${DEBUG-false}
HOSTNAME=${HOST} #"phplist.iscpif.fr" #${HOSTNAME-smtp}
RELAY_DOMAINS= #Empty to relay all emails

postconf -e myhostname=${HOSTNAME}
postconf -e relay_domains=${RELAY_DOMAINS}
postconf -e smtpd_sasl_auth_enable=no
postconf -e "mynetworks= 127.0.0.0/8 172.0.0.0/8"
postconf -# mydestination
postconf -F '*/*/chroot = n'

postconf -x mydestination
[ "$DEBUG" == "true" ] && sed -i 's/smtpd$/smtpd -v/g' /etc/postfix/master.cf

rsyslogd
postfix start

tail -F /dev/null
