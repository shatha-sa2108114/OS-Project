#!/bin/bash

copy_to_server() {
    local file_path=$1
    local username="client2"
    local server_address="localhost"
    local dest_path="/home/client2"
    
    scp "$file_path" "${username}@${server_address}:${dest_path}"
}

cpu_info() {
    echo "==== CPU Info ===="
    top -bn1 | grep 'Cpu(s)'
    echo
    echo "---- Top 5 CPU Resource-Consuming Processes ----"
    ps aux --sort=-%cpu | head -n 6
}

memory_info() {
    echo "==== Memory Info ===="
    free -h
    echo
    echo "---- Top 5 Memory Resource-Consuming Processes ----"
    ps aux --sort=-%mem | head -n 6
}

while true; do
    proc_tree=$(pstree)
    zombie_procs=$(ps aux | grep -w 'Z')

    {
        echo "==== Process Info ===="
        echo "Process Tree:"
        echo "$proc_tree"
        echo
        echo "Zombie Processes:"
        echo "$zombie_procs"
        echo
        cpu_info
        echo
        memory_info
    } | tee process_details.log

    copy_to_server "process_details.log"
    sleep 3600
done 