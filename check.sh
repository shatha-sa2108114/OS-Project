#!/bin/bash

logfile="perm_change.log"
path="." #current directory

result_files=$(find $path -type f -perm 0777)

#if empty
if [ -z "$result_files" ]; then
  echo "No files with 777 permissions found." >> "$logfile"
else
      echo "Files with 777 permissions found:" >> "$logfile"
      printf "%s\n" "${result_files[@]}" | tee -a "$logfile"

    
    #change the mod to 700 of each file
    for file in $result_files; do
        chmod 700 "$file"
        echo "changed permissions of file: '$file' from 777 to 700" >> "$logfile"
    done
fi

