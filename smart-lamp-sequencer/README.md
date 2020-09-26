# Smart Lamp Sequencer

A tool to programmatically pilot a smart lamp, in rythm with a metronom. 

Contains a cli runnable class, actually plugged to a Luke Roberts' Lamp F.

````shell script
java \ 
  -Dlogback.configurationFile=logback.xml \
  -jar sequencer.jar \
  --mac-addr=C4:AC:05:42:73:A4 \
  --timeout=20 \
  --script=/etc/scripts/fiesta.yaml
````