#!/bin/bash

echo "disk usage and info about HOME directory at date: $(date '+%Y-%m-%d %H:%M:%S') :" | tee -a disk_info.log
du -sh $HOME | tee -a disk_info.log # disk usage of home directory in MB/KB etc 

# sizes of files in the home directory
echo "sizes of files in $HOME:" | tee -a disk_info.log
if [ "$(du -h "$HOME"/* 2>/dev/null | grep -v '/$')" ]
 then
    du -h "$HOME"/* 2>/dev/null | grep -v '/$' | tee -a disk_info.log # exclude directories 
else
    echo "No files in $HOME." | tee -a disk_info.log
fi

# sizes of subdirectories in the home directory
echo "sizes of subdirectories in $HOME:" | tee -a disk_info.log
if [ "$(du -h "$HOME"/* 2>/dev/null | grep '/$')" ]
 then
    du -h "$HOME"/* 2>/dev/null | grep '/$' | tee -a disk_info.log #only directories
else
    echo "no subdirectories in $HOME." | tee -a disk_info.log
fi

# go through each subdirectory to display its file sizes
for subdir in "$HOME"/*
 do
    if [ -d "$subdir" ]
     then
        echo "sizes of files in $subdir:" | tee -a disk_info.log
        if [ "$(du -h "$subdir"/* 2>/dev/null | grep -v '/$')" ] #checks if theres an output 
         then
            du -h "$subdir"/* 2>/dev/null | grep -v '/$' | tee -a disk_info.log #exclude directories 
        else
            echo "no files in $subdir." | tee -a disk_info.log
        fi
        
        echo "sizes of subdirectories in $subdir:" | tee -a disk_info.log 
        if [ "$(du -h "$subdir"/* 2>/dev/null | grep '/$')" ]
         then
            du -h "$subdir"/* 2>/dev/null | grep '/$' | tee -a disk_info.log #only directories
        else
            echo "no subdirectories in $subdir." | tee -a disk_info.log
        fi
    fi
done

echo "system information at date: $(date '+%Y-%m-%d %H:%M:%S')" | tee -a mem_cpu_info.log
#get memory used and calculate percentage, only taking col 3 and col 2 (used mem and total mem) 
free -m | grep Mem | awk '{printf "memory used: %s MB out of %s MB (%.2f%% used)\n", $3, $2, ($3/$2)*100}' | tee -a mem_cpu_info.log 
#get total and used memory and calculate free percentage
free -m | grep Mem | awk '{printf "free memory left: %s MB out of %s MB (%.2f%% free)\n", $2-$3, $2, (($2-$3)/$2)*100}' | tee -a mem_cpu_info.log
echo "CPU model:" | tee -a mem_cpu_info.log
lscpu | grep "Model name" | tee -a mem_cpu_info.log # get model name from cpu info ls
echo "number of CPU cores:" | tee -a mem_cpu_info.log
lscpu | grep "^CPU(s):" | tee -a mem_cpu_info.log # get number of CPU cores from cpu info ls
