#!/usr/bin/env bash
# shell script to run strain-vt-annotation-pipeline
. /etc/profile

APPNAME=strain-vt-annotation-pipeline
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=$(hostname -s | tr '[a-z]' '[A-Z]')
EMAIL_LIST=mtutaj@mcw.edu

cd $APPDIR
java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/$APPNAME.jar "$@" > run.log 2>&1

if [ $? -eq 0 ]; then
  STATUS=OK
else
  STATUS=FAILED
fi

mailx -s "[$SERVER] strain vt annotation pipeline $STATUS" $EMAIL_LIST < $APPDIR/logs/summary.log
