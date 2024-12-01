#!/bin/bash

AttempsLog="invalid_attempts.log"

function login() {
    local user="$1"
    local pass="$2"
    local server="localhost"  #server ip

    #expects terminal request, and sends answer
    expect << EOF
        set timeout 5
        spawn ssh -o StrictHostKeyChecking=no "$user@$server"
        expect {
            "password:" {
                send "$pass\r"
                expect {
                    "Permission denied" {
                        exit 1
                    }
                    "$ " {
                        exit 0
                    }
                }
            }
            "no match for username" {
                exit 1
            }
            "Connection closed" {
                exit 1
            }
            eof {
                exit 1
            }
        }
EOF
}

function scp_with_expect() {
    local user="$1"
    local pass="$2"
    local server="localhost"  
    local file="$4"
    local path="$5"

    expect << EOF
        set timeout 10
        spawn scp -o StrictHostKeyChecking=no "$file" "$user@$server:$path"
        expect {
            "password:" {
                send "$pass\r"
                exp_continue
            }
            "Permission denied" {
                exit 1
            }
            eof {
                exit 0
            }
        }
EOF
}

function handle_excessive_attempts() {
    echo "unauthorized user!!!!"
    echo "3 attempts exceeded" >> "$AttempsLog"
    echo "--------------------------------------------------------------------------" >> "$AttempsLog"
    if scp_with_expect "client1" "amira1998" "localhost" "$AttempsLog" "./"; then
        echo "log file copied to server :)."
    else
        echo "fail to copy log file to server." | tee "$AttempsLog"
    fi
}

function main() {
    local login_attempt=0
    local logged=false
    
    local user="client1"  
    local pass="amira1998"
    
    timeStamp=$(date +"%A, %B %d, %Y at %I:%M %p")
    
    if login "$user" "$pass" ; then
        echo "login successful :)!"
        exit 0
    else
        echo "login fail :("
        echo "$timeStamp: fail login attempt for user $user" | tee "$AttempsLog"
        exit 1
    fi
}

main