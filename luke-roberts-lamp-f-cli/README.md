# Luke Roberts with Smart Lamp Sequencer

A tool to programmatically pilot a Luke Roberts Lamp F. 

If you do not specify the speed manually (using the argument ``--tempo``), then the app tries to determine the tempo 
of the ambient music as it is received in the system's line-in audio source (either the micro input, a webcam, etc.).
 
The tempo detection algorithm used is provided by the Java API [minim](http://code.compartmental.net/tools/minim/). 

This package provides:

- a cli runnable class (for automation purpose, without human action)
- an interactive command tool (to send commands to the lamp manually)
- some sequencing scripts (for various types of use)

## Logback

You can adjust the way log statements are produced by the application by defining a logback configuration file. It 
would enable you to, for example, write the statements to a log file with the format of your choice, change the logs 
threshold, etc.  

The default configuration is suitable for most use case, but if you are going to use the interactive mode intensively, 
you may want to redirect the logs from stdout to a file.  

````xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- 
  def.xml 
  
  - statements from package "ch.sebpiller" (and sub-packages) equals or higher than DEBUG level are written  
    to a file, and those equals or higher than WARN to the console.
  - for all other packages, statement at INFO are written to the file, and ERROR to the console.
-->
<configuration>
    <appender name="file" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>DEBUG</level>
        </filter>
        <file>luke-roberts-lamp-f-sequencer.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>ARCHIVE-luke-roberts-lamp-f-sequencer.log.%d{yyyy-MM-dd}</fileNamePattern>
            <cleanHistoryOnStart>true</cleanHistoryOnStart>
        </rollingPolicy>
        <encoder>
            <pattern>%d{dd.MM.yyyy. HH:mm:ss} %level [%thread] %logger{20} - %msg%n</pattern>
        </encoder>
        <Encoding>utf-8</Encoding>
    </appender>

    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>WARN</level>
        </filter>
        <encoder>
            <pattern>%-4r [%t] %-5p %c - %m%n</pattern>
        </encoder>
    </appender>

    <root level="DEBUG">
        <appender-ref ref="file" />
        <appender-ref ref="console" />
    </root>

    <logger name="ch.sebpiller" level="DEBUG" additivity="false">
        <appender-ref ref="file" level="DEBUG" />
        <appender-ref ref="console" level="WARN" />
    </logger>
</configuration>
````

It is applied with the Java's system property "logback.configurationFile" with a command like this:
````shell script
java -Dlogback.configurationFile=def.xml ...
````

## Interactive mode to pilot the lamp

This command let you access a very basic \[yet in progress...] text interface to send commands to the lamp.

````shell script
java \ 
    -Dlogback.configurationFile=logback.xml \
    -jar luke-roberts-lamp-f-sequencer-x.y.z-jar-with-dependencies.jar \
      --mac=C4:AC:05:42:73:A4
      # when no script specified, the mode is interactive and waits for human interaction
````

## Notify the user of an event of type "alarm" 
 
````shell script
java \
    -Dlogback.configurationFile=logback.xml \
    -jar luke-roberts-lamp-f-sequencer-x.y.z-jar-with-dependencies.jar \
      --mac=C4:AC:05:42:73:A4 \
      --script=embedded:alarm
````

NB: since the script "alarm" does not define any content in its main loop, you don't need to specify 
a tempo neither a duration. Only '``before``' and '``after``' actions will execute. 

## Blink the lamp at each beat

This command invokes the embedded script "boom" that changes the brightness to 100% and immediately after to 0%, 
doing so 7 times, and blink 2 times at the 8th beat. It runs at 120bpm.

It does so during 30 seconds and then shutdown.

````shell script
java \ 
    -Dlogback.configurationFile=logback.xml \
    -jar luke-roberts-lamp-f-sequencer-x.y.z-jar-with-dependencies.jar \
      --mac=C4:AC:05:42:73:A4 \
      --script=embedded:boom \
      --duration=30 \ 
      --tempo=120
````

## Change temperature at each beat of the ambiant music

This command invokes the embedded script "temperature" that changes the temperature of the lamp to 4 different values 
at the tempo of the music playing in the air. 

It does so during 30 seconds and then shutdown.

````shell script
java \ 
    -Dlogback.configurationFile=logback.xml \
    -jar luke-roberts-lamp-f-sequencer-xy.z-jar-with-dependencies.jar \
      --mac=C4:AC:05:42:73:A4 \
      --script=embedded:temperature \
      --duration=30
````

## Write your own scripted sequence of changes

You can script the sequence of actions to play with your lamp using a descriptor in yaml (examples are available in 
the folder ``./src/main/resources/embedded-scripts``):

Just create a file "my-script.yaml" next to the sequencer's jar:
````yaml
## my-script.yaml

# define custom sequences to be referenced later 
sequences:
  green:
    - scene=3;
  red:
    - scene=4;
  before:
    - on;brightness=0;sleep=500
  after:
    - off;
  flash:
    - brightness=100;brightness=0;
  double-flash:
    - brightness=100;brightness=0;brightness=100;brightness=0;

# before allows to define the actions to be executed before the loop starts
before: seq=before

# after allows to set the actions to be executed after the loop exits (either with a timeout, an error, or SIGTERM)
after: seq=after

# the main loop that will execute until an exit condition is met. An element will be executed at each tick of 
# the metronom plugged in. Once the end is reached, the execution starts again from the beginning. 
loop:
  - seq=green;seq=flash;
  - seq=flash;
  - seq=flash;
  - seq=red;seq=flash;
  - seq=flash;
  - seq=flash;
  - seq=flash;
  - seq=double-flash;
````  

Then: 
````shell script
java \ 
    -Dlogback.configurationFile=logback.xml \
    -jar luke-roberts-lamp-f-sequencer-xy.z-jar-with-dependencies.jar \
      --mac=C4:AC:05:42:73:A4 \
      --script=my-script.yaml \
      --timeout=30 \ 
      --bpm=130
````

## Installation

The binaries are not available on any public repository. You have to compile the project yourself. By chance, 
this process is very simple:

- install tools on Linux debian (Debian, Ubuntu, Raspbian, etc...):
````shell script
sudo apt-get install bluetooth blueman bluez -y # bluetooth needed to run unit tests
sudo apt-get install git -y                     # git
sudo apt-get install openjdk-11-jdk maven -y    # java compilation and runtime
````
- checkout and compile the sources:
```shell script
git clone https://github.com/sebpiller/smart-lamp.git
cd smart-lamp
git checkout tags/0.1.5 # or any other tag you may want to build
mvn clean install -DskipTests # skip tests on windows or to speed up the build
```
- copy the file `./luke-roberts-lamp-f-sequencer/target/luke-roberts-lamp-f-sequencer-0.1.5-jar-with-dependencies.jar` to your raspberry.

- connect your Linux machine  with the lamp:
````shell script
> bluetoothctl
scan on
info C4:AC:05:42:73:A4
connect C4:AC:05:42:73:A4
trust C4:AC:05:42:73:A4
quit
```` 

- (optional) create a file with your device information, for example my-lamp-f.yaml :
```yaml
local-bt-adapter: hci0 # your adapter here (optional, default is hci0)
mac: C4:AC:05:42:73:A4 # your mac address here
```

- you are ready to play :-)  

