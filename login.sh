#!/bin/bash

AttempsLog="invalid_attempts.log"

function login() {
    local user="$1"
    local pass="$2"
    local server="$3"

    #using expect for handling password prompts
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

#handle sending files with scp
function scp_with_expect() {
    local user="$1"
    local pass="$2"
    local server="$3"
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


#if attempts > 3
function handle_excessive_attempts() {
    echo "unauthorized user!!!!"
    echo "3 attempts exceeded" >> "$AttempsLog"
    echo "--------------------------------------------------------------------------" >> "$AttempsLog"
    if scp_with_expect "client1" "amira1998" "192.168.1.100" "$AttempsLog" "/home/client1/"; then
        echo "log file copied to server :)."
    else
        echo "fail to copy log file to server." >> "$AttempsLog"
    fi
    sleep 30
    gnome-session-quit --logout --no-prompt
}

function main() {
    local login_attempt=0
    local logged=false
    local server="192.168.1.100" #serverip

    while [ "$logged" != true ]; do
        read -p "please enter username: " user
        read -sp "please enter password: " pass
        echo 
        timeStamp=$(date +"%A, %B %d, %Y at %I:%M %p")

        if login "$user" "$pass" "$server"; then
            logged=true
            echo "login successful :)!"
        else
            echo "login fail :("
            echo "$timeStamp: fail login attempt for user $user" >> "$AttempsLog"
            login_attempt=$((login_attempt + 1))

            if [ "$login_attempt" -eq 3 ]; then
                handle_excessive_attempts
                break
            fi
        fi
    done
}

main

