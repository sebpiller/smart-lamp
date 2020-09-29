# Smart Lamp Sequencer

A tool to programmatically pilot a smart lamp, in rythm with a metronom. 

Contains a cli runnable class, (actually hard-plugged to a Luke Roberts' Lamp F), and some embedded scripts.

## Notify the user of an event of type "alarm" 
 
This command let you access a very basic command line tool to pilot manually the lamp.

````shell script
java \
    -Dlogback.configurationFile=logback.xml \
    -jar smart-lamp-sequencer-x.y.z-jar-with-dependencies.jar \
      --mac=C4:AC:05:42:73:A4 \
      --script=embedded:alarm
````

## Blink the lamp at each beat

This command invokes the embedded script "boom" that changes the brightness to 100% and immediately after to 0%, 
doing so 7 times, and blink 2 times at the 8th beat. It runs at 120bpm.

It does so during 30 seconds and then shutdown.

````shell script
java \ 
    -Dlogback.configurationFile=logback.xml \
    -jar smart-lamp-sequencer-x.y.z-jar-with-dependencies.jar \
      --mac=C4:AC:05:42:73:A4 \
      --script=embedded:boom \
      --timeout=30 \ 
      --bpm=120
````

## Change temperature at each beat

This command invokes the embedded script "temperature" that changes the temperature of the lamp to 4 values 
at the tempo of 130 bpm. It does so during 30 seconds and then shutdown.

````shell script
java \ 
    -Dlogback.configurationFile=logback.xml \
    -jar smart-lamp-sequencer-x.y.z-jar-with-dependencies.jar \
      --mac=C4:AC:05:42:73:A4 \
      --script=embedded:temperature \
      --timeout=30 \ 
      --bpm=130
````


## Access a CLI to pilot the lamp

This command let you access a very basic command line tool to pilot manually the lamp \[in progress\].

````shell script
java \ 
    -Dlogback.configurationFile=logback.xml \
    -jar smart-lamp-sequencer-x.y.z-jar-with-dependencies.jar \
      --mac=C4:AC:05:42:73:A4 \
      --cli
````