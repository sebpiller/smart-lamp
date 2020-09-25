#!/bin/sh
# Starting bluetooth service
# dbus (/etc/init.d/dbus start) and bluez (
service bluetooth start

# Start java code
cd /luke-roberts/app

java \
-Dconfig=/luke-roberts/config/luke-roberts-lamp-f.yaml \
-Dlogback.configurationFile=/luke-roberts/config/logback.xml \
-jar /luke-roberts/app/connector.jar demo
