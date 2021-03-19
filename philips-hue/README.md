# Philips Hue 

A facade to a Philips Hue Smart Bulb. Only supports direct bluetooth connection.

__wifi bridge not supported!__. 

## Warning 

At the moment, with a RPi4 bluetooth chipset, the connection to the bulb fails almost 90% of the time with a trace like this:
````
Caused by: org.freedesktop.dbus.exceptions.DBusExecutionException: Software caused connection abort
        at jdk.internal.reflect.GeneratedConstructorAccessor10.newInstance(Unknown Source)
        at java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessor                                                                                                                        Impl.java:45)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:490)
        at org.freedesktop.dbus.errors.Error.getException(Error.java:157)
        at org.freedesktop.dbus.errors.Error.throwException(Error.java:187)
        at org.freedesktop.dbus.RemoteInvocationHandler.executeRemoteMethod(RemoteInvocationHandler.java:164)
        at org.freedesktop.dbus.RemoteInvocationHandler.invoke(RemoteInvocationHandler.java:228)
        at com.sun.proxy.$Proxy22.Connect(Unknown Source)
        at com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice.connect(BluetoothDevice.java:408)
        at BluetoothHelper.reconnectIfNeeded(BluetoothHelper.java:86)
        ... 16 common frames omitted
````

The same error occurs at the same frequency using bash ``bluetoothctl > connect XXXX``.

When connecting to the bulb with a BCM bluetooth chip on Ubuntu 20.04/amd64, the problem vanish.

Setup that can connect to Philips Hue:
```shell script
dmesg | grep -i blue
[    3.916966] Bluetooth: Core ver 2.22
[    3.916984] Bluetooth: HCI device and connection manager initialized
[    3.916990] Bluetooth: HCI socket layer initialized
[    3.916992] Bluetooth: L2CAP socket layer initialized
[    3.916994] Bluetooth: SCO socket layer initialized
[    4.094691] Bluetooth: hci0: BCM: chip id 102
[    4.095679] Bluetooth: hci0: BCM: features 0x2f
[    4.111692] Bluetooth: hci0: spiller-desktop-ubu
[    4.113710] Bluetooth: hci0: BCM20703A1 (001.001.005) build 0481
```



