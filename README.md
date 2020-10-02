# Smart Lamp 

A set of tools to pilot a smart lamp.

## Installation

The binaries are not available on any public repository. You have to compile the project yourself. By chance, 
this process is very simple:

- install tools on Linux debian (Debian, Ubuntu, Raspbian, etc...):
````shell script
sudo apt-get install git -y                     # git
sudo apt-get install bluetooth blueman bluez -y # bluetooth: needed in production and to run integration tests
sudo apt-get install openjdk-11-jdk maven -y    # java compilation and runtime
```` 
- checkout and compile the sources:
```shell script
cd ~
git clone https://github.com/sebpiller/smart-lamp.git
cd smart-lamp
git checkout smart-lamp-parent-0.1.5 # or any other tag you may want to build
mvn clean install -DskipTests        # skip tests on windows or to speed up the build
```
- checkout and compile metronom (optional, just disable some modules in pom.xml if you don't need the sequencer tool)
```shell script
cd ~
git clone https://github.com/sebpiller/metronom.git
cd metronom
git checkout metronom-0.1.1   # or any other tag you may want to build
mvn clean install -DskipTests # skip tests on windows or to speed up the build
```

