#!/bin/bash
mkdir -p out output
javac src/*.java -d out/
java -cp out Main input/grammar2.txt input/input_valid.txt
java -cp out Main input/grammar2.txt input/input_errors.txt
