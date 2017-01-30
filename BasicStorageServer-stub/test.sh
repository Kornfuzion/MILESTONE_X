#!/bin/bash
ant
for i in {1..3}
do
rm logs/server/*
sleep 3
rm storage/*
sleep 3
java -jar ms1-server.jar 1234
sleep 3
done
