#!/bin/bash

# print the hostname of the machine 
echo "hostname: $(hostname)" | tee -a network.log

# print the routing table 
echo "routing table:" | tee -a network.log
route -n | tee -a network.log

# display the DNS server configuration
echo "DNS server config:" | tee -a network.log
cat /etc/resolv.conf | tee -a network.log

# test DNS server by looking up the IP address of google.com
echo "testing DNS Server:" | tee -a network.log
nslookup google.com | tee -a network.log

# trace route to google.com, if connectivity fials - reboot
echo "tracing route to google.com:" | tee -a network.log
if traceroute google.com | tee -a network.log
 then
         echo "traceroute to google.com successful" | tee -a network.log
 else 
     echo "traceroute to google.com failed, rebooting the machine" | tee -a network.log
    sudo reboot
fi

# ping google.com once, if connectivity fails - reboot
echo "pinging google:" | tee -a network.log
if ping -c 1 -w 5 google.com > /dev/null
         then
            echo "ping to google was successful" | tee -a network.log
        else
            echo "ping to google was unsuccessful, rebooting the machine" | tee -a network.log
    	    sudo reboot
fi

echo "traceroute completed at $(date '+%Y-%m-%d %H:%M:%S')." | tee -a network.log

