# Smart Lamp 

A set of tools to pilot a smart lamp.

## Installation

The binaries are not available on any public repository. You have to compile the project yourself. By chance, 
this process is very simple:

- install tools on Linux debian (Debian, Ubuntu, Raspbian, etc...):
````shell script
sudo apt-get install git -y                     # git
sudo apt-get install bluetooth blueman bluez -y # bluetooth: needed in production and to run integration tests
sudo apt-get install default-jdk maven -y       # java compilation and runtime
```` 

- checkout and compile metronome 
(optional, just disable some modules in pom.xml if you don't need the sequencer tool)
```shell script
cd ~
git clone https://github.com/sebpiller/metronome.git
cd metronome
git checkout develop                              # or any other tag you may want to build
mvn clean install -DskipUTs -DskipITs -DskipTests # skip tests on windows or to speed up the build
```

- checkout and compile the sources:
```shell script
cd ~
git clone https://github.com/sebpiller/smart-lamp.git
cd smart-lamp
git checkout develop                               # or any other tag you may want to build
mvn clean install -DskipUTs -DskipITs -DskipTests  # skip tests on windows or to speed up the build
```

