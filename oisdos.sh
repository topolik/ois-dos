#!/bin/bash
if [ ! -e target/oisdos-1.0.jar ]; then
	mvn package
	echo
fi

java -Xmx25g -jar target/oisdos-1.0.jar $*