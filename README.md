# WIF Repair Tool

Tool for fixing common typos in Bitcoin private keys. Useful in cases where a key may have been transcribed
incorrectly or otherwise garbled; currently handles transpositions, substitutions, bulk substitutions (e.g. all 9s became lowercase Gs), and combinations of these.

Currently can recover Bitcoin private keys, both compressed (starting with K or L) and uncompressed (starting with 5). Cannot handle addresses due to the leading zero bytes at the moment.

Build using IntelliJ IDEA (Community Edition will suffice). JavaMain runs GUI; main.kt's main function runs command-line interface.

A pre-built JAR for each of these will be uploaded to releases eventually.

Tip jar: 1518kLejjYgsY4jj25gQFbaZrnmADGwhTn
