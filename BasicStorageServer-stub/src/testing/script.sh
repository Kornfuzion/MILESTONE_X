HOST=$1
PORT=$2

ssh -o "UserKnownHostsFile=/dev/null" -o "StrictHostKeyChecking=no" $HOST "nohup java -jar $(pwd)/ms3-server.jar $PORT >/dev/null &"
#ssh -o "UserKnownHostsFile=/dev/null" -o "StrictHostKeyChecking=no" $HOST "nohup java -jar $(pwd)/ms3-server.jar 3930 >/dev/null &"
#ssh -o "UserKnownHostsFile=/dev/null" -o "StrictHostKeyChecking=no" $HOST "nohup java -jar $(pwd)/ms3-server.jar 3525 >/dev/null &"
#ssh -o "UserKnownHostsFile=/dev/null" -o "StrictHostKeyChecking=no" $HOST "nohup java -jar $(pwd)/ms3-server.jar 3555 >/dev/null &"
#ssh -o "UserKnownHostsFile=/dev/null" -o "StrictHostKeyChecking=no" $HOST "nohup java -jar $(pwd)/ms3-server.jar 4634 >/dev/null &"

