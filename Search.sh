#!/bin/bash

bigfile="bigfile"
system_admin_email="sa2103114@qu.edu.qa" #assuming shatha  is the system adminstrator since shes the host
sender_email="ra2103056@qu.edu.qa" #my email

find . -type f -size +1M > "$bigfile"
echo "Search Date: $(date)" >> "$bigfile"
echo "Number of files found: $(wc -l < "$bigfile")" >> "$bigfile"

if [ -s "$bigfile" ]; then
    mail -s "Big Files Report" "$system_admin_email" -r "$sender_email" < "$bigfile"
fi
