#!/bin/bash
exec /usr/sbin/sshd -D

source "/root/.gvm/bin/gvm-init.sh"
gvm install grails 2.4.3
