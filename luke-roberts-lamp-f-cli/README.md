# Luke Roberts Lamp F - with Smart Lamp Sequencer

A cli tool to pilot and sequence commands for a Luke Roberts' Lamp F. 

## Interactive mode

In interactive mode, the user sends commands to the lamp using a text interface.

````shell script
java \ 
  [-Dlogback.configurationFile=logback.xml] \
  -jar luke-roberts-lamp-f-cli-*-jar-with-dependencies.jar \
    --mac C4:AC:05:42:73:A4
    # when no script specified, the mode is interactive and waits for human interaction
````

## Scripted mode

Using the parameter ``--script`` you enter the scripted mode. You can pilot a sequence of transition to be played in 
loop, synchronized with a tempo (either dynamic or static), for a predefined duration or forever.

If you do not specify the speed manually (using the argument ``--tempo``), then the app tries to determine the tempo 
of the ambient music as it is received in the system's line-in audio source (either the micro input, a webcam, etc.).
 
The tempo detection algorithm used is provided by the Java API [minim](http://code.compartmental.net/tools/minim/). 

### Play "alarm" script 
 ````shell script
java \
  [-Dlogback.configurationFile=logback.xml] \
  -jar luke-roberts-lamp-f-cli-*-jar-with-dependencies.jar \
      --mac C4:AC:05:42:73:A4 \
      --script embedded:alarm
````
NB: since the script "alarm" does not define any content in its main loop, you don't need to specify 
a tempo neither a duration. Only '``before``' and '``after``' actions will execute. 

### Blink the lamp at each beat
This command invokes the embedded script "boom" that changes the brightness to 100% and immediately after to 0%, 
doing so 7 times, and blink 2 times at the 8th beat. It runs at 120bpm.

It does so during 30 seconds and then shutdown.
````shell script
java \ 
  [-Dlogback.configurationFile=logback.xml] \
  -jar luke-roberts-lamp-f-cli-*-jar-with-dependencies.jar \
      --mac C4:AC:05:42:73:A4 \
      --script embedded:boom \
      --duration 30 \ 
      --tempo 120 
````

### Change temperature at each beat of the ambient music
This command invokes the embedded script "temperature" that changes the temperature of the lamp to 4 different values 
at the tempo of the music playing in the air. 

It does so during 30 seconds and then shutdown.
````shell script
java \ 
  [-Dlogback.configurationFile=logback.xml] \
  -jar luke-roberts-lamp-f-cli-*-jar-with-dependencies.jar \
      --mac C4:AC:05:42:73:A4 \
      --script embedded:temperature \
      --duration 30
````

