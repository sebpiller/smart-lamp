package ch.sebpiller.iot.bluetooth.bluez.scan;

import org.freedesktop.dbus.interfaces.Properties;

import java.time.LocalDateTime;

public class ScanPropertiesChangedData {
    protected final String address;
    protected final Properties.PropertiesChanged properties;
    protected final LocalDateTime date;

    public ScanPropertiesChangedData(String address, Properties.PropertiesChanged properties, LocalDateTime date) {
        this.address = address;
        this.properties = properties;
        this.date = date;
    }

    public String getAddress() {
        return address;
    }

    public Properties.PropertiesChanged getProperties() {
        return properties;
    }

    public LocalDateTime getDate() {
        return date;
    }
}