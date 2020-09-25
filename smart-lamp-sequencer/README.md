# Smart Lamp Sequencer

A tool to programmatically pilot a smart lamp to do things synchronized with a metronom.

````shell script
java \ 
  -Dlogback.configurationFile=logback.xml \
  -jar sequencer.jar \
  --mac-addr=C4:AC:05:42:73:A4 \
  --timeout=20
````