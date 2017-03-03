HOST=$1

ssh -o "UserKnownHostsFile=/dev/null" -o "StrictHostKeyChecking=no" $HOST "nohup java -jar /nfs/ug/homes-4/k/kovictor/ece419/MILESTONE_X/BasicStorageServer-stub/ms3-server.jar 3000 >/dev/null &"
ssh -o "UserKnownHostsFile=/dev/null" -o "StrictHostKeyChecking=no" $HOST "nohup java -jar /nfs/ug/homes-4/k/kovictor/ece419/MILESTONE_X/BasicStorageServer-stub/ms3-server.jar 3930 >/dev/null &"
ssh -o "UserKnownHostsFile=/dev/null" -o "StrictHostKeyChecking=no" $HOST "nohup java -jar /nfs/ug/homes-4/k/kovictor/ece419/MILESTONE_X/BasicStorageServer-stub/ms3-server.jar 3525 >/dev/null &"
ssh -o "UserKnownHostsFile=/dev/null" -o "StrictHostKeyChecking=no" $HOST "nohup java -jar /nfs/ug/homes-4/k/kovictor/ece419/MILESTONE_X/BasicStorageServer-stub/ms3-server.jar 3555 >/dev/null &"
ssh -o "UserKnownHostsFile=/dev/null" -o "StrictHostKeyChecking=no" $HOST "nohup java -jar /nfs/ug/homes-4/k/kovictor/ece419/MILESTONE_X/BasicStorageServer-stub/ms3-server.jar 4634 >/dev/null &"

