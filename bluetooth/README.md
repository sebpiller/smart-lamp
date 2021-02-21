# Bluetooth Base Lamp

An abstract implementation of a smart lamp using bluetooth BLE as communication protocol.

## Useful commands and references
(just a collection of commands I should not forget ^^)

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

````shell script
sudo btmon -w btsnoop.data -p error
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
