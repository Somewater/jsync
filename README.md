# jSync
Cross platform client/server app for sync code between teacher and students computers

## Purpose
1. Monitoring multiple developers at the same time with the maximum convenience.
2. Live coding collaboration (teacher can fix the student's code and changes will be updated on the source computer).

## Features:
1. Free and cross platform, only JRE 11+ required.
2. Minimal configuration, can work out of the box in a local network, Automatic Gateway discover.
3. Network bandwidth economy: only changed files are transmitted (single line changes are in the plan but not yet in development).
4. Automatically filter files with unexpected extension, ignore large files and excessive changes per file.
5. Security: intruder can't execute malicious code (of course, if you do not run the malicious code by yourself, be careful!).

## Principle of operation
student project dir
```
- JavaBasics/
    custom_file.txt
    Test.java
    Test2.java
```

teacher projects dir
```
- PROJECTS/
    - MrStudent/
        - JavaBasics/
            Test.java
            Test2.java
    - OtherStudent/
        - JavaBasics/
            Test.java 
...       
```

## Build
```
mvn package
```

## Run server
```
java -jar server\target\jsync-server-0.0.1.jar

# or with params, use "--help" for more info
java -jar server\target\jsync-server-0.0.1.jar --help
```


## Run client
You can distribute a single jar file from `client\target\jsync-client-0.0.1.jar` between student machines 
(only Java JRE required), place it into the learning project directory and launch it as:

```
java -jar jsync-client-0.0.1.jar

# or with params, use "--help" for more info
java -jar jsync-client-0.0.1.jar --help

# for example, select another project directory for sync:
java -jar jsync-client-0.0.1.jar -d "/path/to/project"

# assign predefined server host/ port:
java -jar jsync-client-0.0.1.jar -h my.server.com -p 80

# set custom synchronizable file extensions:
java -jar jsync-client-0.0.1.jar -e h,c,hpp,cpp,Makefile

# custom student and project name. Otherwise, Project dir name used as project name.
# User name also can be changed globally by putting the file "jsync-config.txt" containing user name to the user home
java -jar jsync-client-0.0.1.jar -u MrStudent -p JavaBasics
```  