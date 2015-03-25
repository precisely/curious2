#!/bin/bash
service mysql start
export JAVA_HOME=/usr/java/jdk1.7.0_51
export GRAILS_HOME="/vagrant/current-grails"
export PATH="$PATH:$GRAILS_HOME/bin"
