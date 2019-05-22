requirements
============

* java 8

running
=======

build the project:

```
$ ./mvnw clean install
[INFO] Scanning for projects...
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Build Order:
[INFO]
[INFO] publication-explorer                                               [pom]
[INFO] publication-explorer-nlp                                           [jar]

...

[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary:
[INFO]
[INFO] publication-explorer 1.0.0-SNAPSHOT ................ SUCCESS [  0.398 s]
[INFO] publication-explorer-nlp 1.0.0.-SNAPSHOT ........... SUCCESS [02:02 min]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:03 min
[INFO] Finished at: 2019-05-22T10:36:45+02:00
[INFO] ------------------------------------------------------------------------
```

After the project has been built, you can run it. The jar itself is a fat jar, and contains all libraries and other resources that are needed to run it (except the jvm, of course)

```
$ java -jar ./publication-explorer-nlp/target/publication-explorer-nlp-1.0.0.-SNAPSHOT-shaded.jar
```

To learn what happens, start digging through the code starting with the main: `helt.pubex.Main`.

development
===========

import the maven projects into the IDE of your choice, and start working.

data input
==========

create a directory tree with this layout:

```
* data
  * input
    * pdf   <--- Put pdf files here
    * txt   <--- put *.txt files here
  * output
```
