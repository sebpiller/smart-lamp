# luke-roberts-lamp-f

See product page: https://www.luke-roberts.com/collections/pendant-lamps/products/smart-lamp-model-f-black?ls=fr

I (the developer) am not affiliated in any way with the Firm "Luke Roberts" located in Austria. 

The firm Luke Roberts owns the full copyrights for all their products.

All the code provided here has been written with only the official Luke Roberts' bluetooth API documentation, 
which you can find a copy in the ./doc folder. I didn't make use of any reverse-engineering technic or procedure. 

## Disclaimer

The code provided has been tested on my instance of a Lamp F. It has proven to run smoothly, and is very stable with 
my type of usage, but since I don't have any feedback or validation from Luke Roberts, there is no guarantee that 
everything is fine.

**In the worst case, destruction of the device is possible (yet highly improbable).**

By using the library, you agree that I (the developer) am not responsible for any damage resulting of the usage 
of any part of the code.


## Licensing

This code is not protected by any kind of license. Just use it in any way you like: copy, modify, alter, 
rename, remove any references to me, pretend you did everything yourself, etc. I don't mind :-)

But if you do something cool with it I would be very happy to know what you have done :-)


## Introduction
This project is a low level connector to drive Luke Roberts Lamp F from a Raspberry Pi's Bluetooth chipset. 

It enables integration in tools like bash, cron, java applications, IoT managers, Smart Home, etc...

Actually, the library supports: 
- power on/off a lamp
- select a scene
- change the brightness
- change the temperature of the main bulb
- fade in/out the brightness or the temperature
 
Runs nicely at least under Raspbian arm/32v7 and Ubuntu 20.04/amd64 (although Ubuntu can be a bit tricky to get 
working). Others linux-based systems may work too, especially Android (untested). 

Since it depends on some native packages built for Linux [Bluez](http://www.bluez.org), it doesn't work on Windows systems.

## Java library

Add the dependency: 
```xml
<dependency>
  <groupId>ch.sebpiller.iot</groupId>
  <artifactId>luke-roberts-lamp-f</artifactId>
  <version>0.1.5</version>
</dependency>
```

Then 
```java
public class Test {
        
    public static void main(String[] args) {
        LukeRoberts.LampF.Config config = LukeRoberts.LampF.Config.getDefaultConfig();
        if (System.getProperty("config") != null) {
            config = LukeRoberts.LampF.Config.loadOverriddenConfigFromSysprop("config");
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
(just a collection of commands I should not forget ^^)

## Connect to the Luke Roberts Lamp F from Linux Raspbian

### Install Raspbian required packages
`````shell script
sudo apt update -y && sudo apt upgrade -y && sudo apt install -y bluetooth blueman bluez 
`````

### bluetoothctl
Usefull to scan for the MAC address of the device, trust it, pair it, etc.: 
````
> bluetoothctl
scan on
info C4:AC:05:42:73:A4
connect C4:AC:05:42:73:A4
quit
````

### bluetooth traffic
Monitor the packets on the bluetooth bus:
````shell script
sudo dbus-monitor --system "destination='org.bluez'" "sender='org.bluez'"
````

### busctl 
Manually send data over the air to a device's service and characteristic:
```shell script
busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4 org.bluez.Device1 Connect
# brigthness 100%
busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4/service001d/char001e org.bluez.GattCharacteristic1 WriteValue aya{sv} 4 0xa0 0x01 0x03 0x7f 0
# brigthness 0%
busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4/service001d/char001e org.bluez.GattCharacteristic1 WriteValue aya{sv} 4 0xa0 0x01 0x03 0x00 0
```

## XXX
### Docker
\[in progress]
Build a docker image to run on raspberry pi 4 
````shell script
docker buildx build --platform linux/arm/v7 -t sebpiller/luke-roberts-lamp-f-bt-rpi:latest .
````

### Bash aliases
To invoke some commands directly from bash, no java needed.
```shell script
> cat ~/.bash_aliases

alias lk_full='busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4 org.bluez.Device1 Connect && busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4/service001d/char001e org.bluez.GattCharacteristic1 WriteValue aya{sv} 4 0xa0 0x01 0x03 0x7f 0'
alias lk_mid ='busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4 org.bluez.Device1 Connect && busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4/service001d/char001e org.bluez.GattCharacteristic1 WriteValue aya{sv} 4 0xa0 0x01 0x03 0x40 0'
alias lk_low ='busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4 org.bluez.Device1 Connect && busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4/service001d/char001e org.bluez.GattCharacteristic1 WriteValue aya{sv} 4 0xa0 0x01 0x03 0x00 0'
```
(works at least on ubuntu & raspberry pi)

### java remote debugging
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
possible.
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
