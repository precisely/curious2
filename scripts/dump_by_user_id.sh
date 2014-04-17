#!/usr/bin/env bash
USERNAME=curious
PASSWORD=734qf7q35
OPTS="-u $USERNAME -p$PASSWORD tlb"
USER_IDS="(4, 115, 110, 109, 1)"
WHERE_CLAUSE="'user_id in $USER_IDS'"
alias dd='date +"%Y-%m-%d"'
DUMP_FILE='tlb.approved_users.2014-04-15.sql'
#RUN="echo"
RUN="eval"

# Schema only with "-d"
SCHEMA_DUMP="mysqldump -d $OPTS > $DUMP_FILE"

# User meta data.
USER_META_DUMP="mysqldump $OPTS _user --where='id in $USER_IDS' >> $DUMP_FILE"

# Select permitted users only.
USER_DUMP="mysqldump $OPTS discussion duration_pair entry plot_data --where=$WHERE_CLAUSE >> $DUMP_FILE
"

# All tags related info since it is anonymous.
TAG_DUMP="mysqldump $OPTS entry_tag tag tag_group tag_group_properties tag_group_tag tag_stats >> $DUMP
_FILE"

$RUN $SCHEMA_DUMP
$RUN $USER_META_DUMP
$RUN $USER_DUMP
$RUN $TAG_DUMP
