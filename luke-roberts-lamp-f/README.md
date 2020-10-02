# luke-roberts-lamp-f
See product page: https://www.luke-roberts.com/collections/pendant-lamps/products/smart-lamp-model-f-black?ls=fr

I (the developer) am not affiliated in any way with the Firm "Luke Roberts" located in Austria. 

The firm Luke Roberts owns the full copyrights for all their products.

All the code provided here has been written with only the official Luke Roberts' bluetooth API documentation, 
which you can find a copy in the ./doc folder. I didn't make use of any reverse-engineering technic or procedure. 

## Disclaimer
The code provided has been tested on my instance of a Lamp F. It has proven to run smoothly, and so far, is very stable 
with my type of usage. But as I don't have any feedback or validation from Luke Roberts, I can not guarantee that 
everything is fine.

**In the worst case, destruction of the device should be considered as possible (yet highly improbable).**

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
- use the "immediate light" command \[development in progress]
 
Runs nicely at least under Raspbian arm/32v7 and Ubuntu 20.04/amd64. Others linux-based systems may work too, 
especially Android (untested). 

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

        SmartLampFacade smartLamp = new LampFBle(config);

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
