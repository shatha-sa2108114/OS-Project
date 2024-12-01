#!/bin/bash

for target in "$@"; do
    success=true  # to track the success of the pings
    
    #using loop instead of ping -c 3 to see at which exact attempt the ping has failed or succeeded 
    for i in {1..3}
     do
        echo "$i: pinging $target" | tee -a network.log
        if ping -c 1 -w 5 "$target" > /dev/null
         then
            echo "ping was successful" | tee -a network.log
        else
            echo "ping was unsuccessful" | tee -a network.log
            success=false  # success = false if any ping fails
        fi
    done

    if [ "$success" = true ]
     then
        echo "$(date '+%Y-%m-%d %H:%M:%S') connectivity with $target is OK :)" | tee -a network.log
    else
        echo "$(date '+%Y-%m-%d %H:%M:%S') connectivity with $target failed at one or all attempts :(" | tee -a network.log
        ./traceroute.sh "$target" | tee -a network.log
    fi
done

