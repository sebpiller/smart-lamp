# Smart Lamp Sequencer

A tool to programmatically record a list of manipulations to do on a smart lamp.
 
That in rhythm with a metronome that dispatch the . 

## Write your own scripted sequence of changes

You can script the sequence of actions to play with your lamp using a descriptor in yaml (examples are available in 
the folder ``./src/main/resources/embedded-scripts``):

Just create a file "my-script.yaml" next to the sequencer's lamp implementation jar:
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
# the metronome plugged in. Once the end is reached, the execution starts again from the beginning. 
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
    -jar <lamp-cli>-*-jar-with-dependencies.jar \
      --mac=XX:XX:XX:XX:XX:XX \
      --script=my-script.yaml \
      --duration=30 \ 
      --tempo=130
```` 