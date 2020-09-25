# luke-roberts-lamp-f

## disclaimer
I (the developer) am not affiliated in any way with the Firm "Luke Roberts" located in Austria. 

The firm Luke Roberts owns the full copyrights of all its products.

Reverse engineering is forbidden. All the code provided here has been written with only the official Luke Roberts' 
bluetooth API documentation, which you can find a copy in the ./doc folder.

## License
This code is not protected by any kind of license. Just use it in any way you'd like, but if you do something cool 
with it I'd be happy to know :-)

## Introduction
This project is a low level connector to drive Luke Roberts Lamp F from a Raspberry Pi's Bluetooth chipset. 

It enables integration in tools like bash, cron, java applications, IoT managers, Smart Home, etc...

Actually, the library supports: 
- power on/off a lamp
- select a scene
- change the brightness
- change the temperature of the main bulb
- fade in/out the brightness or the temperature
 
Runs nicely at least under Raspbian arm/32v7 and Ubuntu 20.04/amd64. Others linux based systems may work too, especially 
Android (untested). 

But since it depends on native bluez package on Linux, it doesn't work on Windows systems.

## Quickstart demo

The binaries are not available on any public repository. You have to compile yourself. By chance, this process 
is very simple:

- checkout and compile the sources:
```shell script
sudo apt-get install bluetooth blueman bluez -y  # bluetooth needed to build
sudo apt-get install git -y                      # git
sudo apt-get install git openjdk-11-jdk maven -y # java compilation
git clone https://github.com/sebpiller/luke-roberts-lamp-f.git
cd luke-roberts-lamp-f
git checkout tags/0.0.1-alpha1 # or any other tag you may want to build
mvn clean package
```
- copy the file `./target/luke-roberts-lamp-f-0.0.1-SNAPSHOT-jar-with-dependencies.jar` to your raspberry.

- (optional) create a file with your device information, for example my-lamp-f.yaml :
```yaml
local-bt-adapter: hci0 # your adapter here (optional, default is hci0)
mac: C4:AC:05:42:73:A4 # your mac address here
```

Then, to show some capabilities, you may want to:
- ... enter a very basic cli tool to pilot your lamp:
```shell script
java -Dconfig=my-lamp-f.yaml -jar luke-roberts-lamp-f-0.0.1-SNAPSHOT-jar-with-dependencies.jar
# or 
java -Dlamp.f.mac=XX:XX:XX:XX:XX:XX -jar luke-roberts-lamp-f-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```
This tool allows you to play a bit with the lamp. Actually, you can change the scene, fade in/out the brightness or 
the temperature, and start a blinking loop. This tool is made for geeks, and is in a 'work in progress' state.

- ... launch a scripted demo of some command:
```shell script
java -Dconfig=luke-roberts-config.yaml -jar luke-roberts-lamp-f-0.0.1-SNAPSHOT-jar-with-dependencies.jar demo
# or 
java -Dlamp.f.mac=XX:XX:XX:XX:XX:XX -jar luke-roberts-lamp-f-0.0.1-SNAPSHOT-jar-with-dependencies.jar demo
```
This is just some lines of java code invocations, and shows different type of automation. 

## Java library
(see class ch.sebpiller.iot.bluetooth.lamp.luke.roberts.lamp.f.Cli and all the units tests to see some usage 
example of the API.)

Add the dependency: 
```xml
  <dependency>
      <groupId>ch.sebpiller.iot</groupId>
      <artifactId>luke-roberts-lamp-f</artifactId>
      <version>0.0.1-SNAPSHOT</version>
  </dependency>
```

Then 
```java
public class Test {
        
    public static void main(String[] args) {
        LukeRoberts.LampF.Config config = LukeRoberts.LampF.Config.getDefaultConfig();
        if (System.getProperty("smart-lamp/luke-roberts/src/main/resources/config") != null) {
            config = LukeRoberts.LampF.Config.loadOverriddenConfigFromSysprop("smart-lamp/luke-roberts/src/main/resources/config");
        }
        if (System.getProperty("lamp.f.mac") != null) {
            config.setMac(System.getProperty("lamp.f.mac"));
        }

        LukeRobertsLampFController smartLamp = new LukeRobertsLampFController(config);

        try {
            smartLamp
                    .power(true).wait(2000)
                    .setBrightness((byte) 50).wait(2000)
                    .fadeFromToBrightness((byte) 0, (byte) 100, SmartLampFacade.FadeStyle.FAST).get();
            
            // blink
            for (int i = 0; i < 25; i++) {
                smartLamp.power(true).wait(50).power(false).wait(150);
            }
            
            smartLamp.power(true);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
```

# Usefull commands and references
## Connect to the Luke Roberts Lamp F from Linux Raspbian

### Install Raspbian required packages
`````shell script
sudo apt update -y && sudo apt upgrade -y && sudo apt install -y bluetooth blueman bluez 
`````

### Docker
\[in progress]
Build a docker image to run on raspberry pi 4 
````shell script
docker buildx build --platform linux/arm/v7 -t sebpiller/luke-roberts-lamp-f-bt-rpi:latest .
````
## Linux configuration and tooling

### Bash aliases
To invoke some commands directly from bash, no java needed.
```shell script
> cat ~/.bash_aliases

alias lk_full='busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4 org.bluez.Device1 Connect && busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4/service001d/char001e org.bluez.GattCharacteristic1 WriteValue aya{sv} 4 0xa0 0x01 0x03 0x7f 0'
alias lk_mid ='busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4 org.bluez.Device1 Connect && busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4/service001d/char001e org.bluez.GattCharacteristic1 WriteValue aya{sv} 4 0xa0 0x01 0x03 0x40 0'
alias lk_low ='busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4 org.bluez.Device1 Connect && busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4/service001d/char001e org.bluez.GattCharacteristic1 WriteValue aya{sv} 4 0xa0 0x01 0x03 0x00 0'
```
(works at least on ubuntu & raspberry pi)

### bluetoothctl
Usefull to scan for the MAC address of the device, trust it, pair it, etc.: 
````
> bluetoothctl
scan on
connect C4:AC:05:42:73:A4
quit
````

### busctl 
To manually send data over the air to a device's service and characteristic:
```shell script
> busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4 org.bluez.Device1 Connect
# brigthness 100%
> busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4/service001d/char001e org.bluez.GattCharacteristic1 WriteValue aya{sv} 4 0xa0 0x01 0x03 0x7f 0

# brigthness 0%
> busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4/service001d/char001e org.bluez.GattCharacteristic1 WriteValue aya{sv} 4 0xa0 0x01 0x03 0x00 0
```

### debuging
#### bluetooth traffic
````shell script
> sudo dbus-monitor --system "destination='org.bluez'" "sender='org.bluez'"
````

#### java remote debugging
`````shell script
> java \
    -Xdebug \
    -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=*:1111 \
    -Dlogback.configurationFile=./config/logback.xml \
    -Dconfig=./config/luke-roberts-lamp-f.yaml \
  -jar connector.jar 
`````

## Disco mode
### bash loop
A very basic "disco mode" sample is implemented in ./src/test/bash/disco.sh file and make the lamp blink as fast as 
possible :

```shell script
#!/bin/bash
discos=(0 127)
busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4 org.bluez.Device1 Connect
while true
do
  for i in ${!discos[@]};
  do
      echo "$i element: setting to ${discos[$i]}..."
      busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4/service001d/char001e org.bluez.GattCharacteristic1 WriteValue aya{sv} 4 0xa0 0x01 0x03 ${discos[$i]} 0
  done
done
```
