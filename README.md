# CS4031 - Compiler Construction | Assignment 02
## LL(1) Parser — Spring 2026
**Roll Numbers:** 23i0743 | 23i0623 | **Section:** E | **Language:** Java

## Compilation
    mkdir -p out
    javac src/*.java -d out/

## Run
    java -cp out Main input/grammar2.txt input/input_valid.txt
    java -cp out Main input/grammar2.txt input/input_errors.txt
    java -cp out Main input/grammar1.txt input/input_edge_cases.txt
    java -cp out Main input/grammar3.txt input/input_valid.txt
    java -cp out Main input/grammar4.txt input/input_valid.txt

## Grammar File Format
    NonTerminal -> alt1 | alt2 | ...
    Non-terminals: start with uppercase (e.g. Expr, Term)
    Epsilon: use 'epsilon' or '@'

## Input File Format
    One input string per line, tokens space-separated.

## Pipeline
    1. Read CFG  2. Left Factoring  3. Left Recursion Removal
    4. FIRST sets  5. FOLLOW sets  6. LL(1) Table
    7. Stack-based parsing with trace  8. Parse trees

## Known Limitations
    Grammar4 is non-LL(1) after transformation — conflicts reported correctly.
