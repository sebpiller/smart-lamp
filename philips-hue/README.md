# Philips Hue 

A facade to a Philips Hue Smart Bulb. Only supports direct bluetooth connection.

__wifi bridge not supported!__. 


Warning: 

at the moment, with a RPi4 bluetooth chipset, the connection to the bulb fails almost 90% of the time with a trace like this:
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
        at ch.sebpiller.iot.bluetooth.BluetoothHelper.reconnectIfNeeded(BluetoothHelper.java:86)
        ... 16 common frames omitted
````

The same error occurs at the same frequency using bash ``bluetoothctl``.

When connecting to the bulb with an intel bluetooth chip on Ubuntu 20.04/amd64, the problem vanish.
