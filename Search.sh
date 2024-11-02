#!/bin/bash

bigfile="bigfile"
system_admin_email="reemakib0@gmail.com"  
sender_email="operatingsys2024@gmail.com"  

find "$HOME" -type f -size +1M > "$bigfile"
echo "current date: $(date '+%Y-%m-%d %H:%M:%S')" >> "$bigfile"
echo "number of files fo: $(wc -l < "$bigfile")" >> "$bigfile"

if [ -s "$bigfile" ]
 then

    msmtp --from="$sender_email" "$system_admin_email" <<EOF
Subject: Big Files Report

 the files larger than 1MB:

$(cat "$bigfile")

EOF
fi