#!/bin/bash
######
## Disco mode changes the brightness in a loop...
## Later it will use deep learning to recognize music style and act accordingly.
######

# let the disco begin...
##discos=(0 64 127 64 32 10 2 5 10 2 32 127 64 127)

## strobo-disco
discos=(0 127)

# let's connect
busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4 org.bluez.Device1 Connect

# let's run the disco
while true; do
  for i in ${!discos[@]}; do
    echo "$i element: setting to ${discos[$i]}..."
    busctl call org.bluez /org/bluez/hci0/dev_C4_AC_05_42_73_A4/service001d/char001e org.bluez.GattCharacteristic1 WriteValue aya{sv} 4 0xa0 0x01 0x03 ${discos[$i]} 0

    #sleep 0.1
  done

done
