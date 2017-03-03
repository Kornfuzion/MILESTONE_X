ps aux | grep "ms3-server" | awk '{print $2 }' | xargs kill

