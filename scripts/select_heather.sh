#!/usr/bin/env bash
USERNAME=curious
PASSWORD=734qf7q35
OPTS="-u $USERNAME -p$PASSWORD tlb"
HEATHER_ID=4

# Schema only with "-d"
SCHEMA_DUMP="mysqldump -d $OPTS > tlb.schema.sql"

USER_META_DUMP="mysqldump $OPTS _user --where=id=$HEATHER_ID > tlb.heather_only.sql"

# heather's data only
USER_DUMP="mysqldump $OPTS discussion duration_pair entry plot_data --where=user_id=$HEATHER_ID > tlb.heather_data.sql"

# tags 
TAG_DUMP="mysqldump $OPTS entry_tag tag tag_group tag_group_properties tag_group_tag tag_stats > tlb.tags.sql"

# Concat
CONCAT="cat tlb.schema.sql tlb.heather_only.sql tlb.heather_data.sql tlb.tags.sql > tlb.heather.`date +"%Y-%m-%d"`.sql"

eval $SCHEMA_DUMP
eval $USER_META_DUMP
eval $USER_DUMP
eval $TAG_DUMP
eval $CONCAT
