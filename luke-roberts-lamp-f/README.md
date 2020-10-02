# luke-roberts-lamp-f

![Luke Roberts Lamp F][lampf]

Official product page: [productpage]
 
[lampf]: https://cdn.shopify.com/s/files/1/0015/0811/4547/products/luke-roberts-model-f-black-middle_600x.png?v=1574342741 
[productpage]: https://www.luke-roberts.com/collections/pendant-lamps/products/smart-lamp-model-f-black?ls=fr



## Affiliation

I (the developer) am not affiliated in any way with the Firm "Luke Roberts" located in Austria. 

The firm Luke Roberts owns the full copyrights for all their products.

All the code provided here has been written with the official Luke Roberts' bluetooth API documentation, 
which you can find a copy in the ./doc folder.  

## Disclaimer
The code provided has been tested on my instance of a Lamp F. It has proven to run smoothly, and so far, is very stable 
with my type of usage. As I didn't receive any kind of feedback or validation from Luke Roberts, I can't guarantee that 
everything is fine.

**In the worst case, the destruction of the device should be considered as possible (yet highly improbable).**
 
This is because the hardware has probably not been designed to support the type of manipulation made possible by this 
library (high frequency power variation and commutation, etc.).

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
- use the "immediate light" command to set a color \[development in progress]
 
Runs nicely at least under Raspbian arm/32v7 and Ubuntu 20.04/amd64 (BMC chip). Others linux-based systems may work too, 
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
                    .power(true).sleep(2000)
                    .setBrightness((byte) 50).sleep(2000)
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
