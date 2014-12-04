#!/bin/bash
service mysql start
export GRAILS_HOME="/vagrant/current-grails"
export PATH="{$PATH}:$GRAILS_HOME/bin"