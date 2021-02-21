package ch.sebpiller.iot.bluetooth.scan;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Objects;

public class ScanData {
    protected static final int RSSI_UNSET = Integer.MAX_VALUE;
    protected static final int TXPOWER_UNSET = Integer.MAX_VALUE;

    private static final String dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";

    private final String adapterDeviceName;
    private final String address;
    private final String name;
    private final int rssi;
    private final int txPower;
    private final LocalDateTime date;
    private final String logPrefix;

    public ScanData(String adapterDeviceName, String address, String name, int rssi, int txPower, LocalDateTime date) {
        this.adapterDeviceName = Objects.requireNonNull(adapterDeviceName);
        this.address = Objects.requireNonNull(address);
        this.name = name;
        this.rssi = rssi;
        this.txPower = txPower;
        this.date = date;
        this.logPrefix = "[" + adapterDeviceName + "] " + address + " ";
    }

    public String getAdapterDeviceName() {
        return adapterDeviceName;
    }

    public String getAddress() {
        return address;
    }

    public String getName() throws NoSuchFieldException {
        if (name == null) {
            throw new NoSuchFieldException(logPrefix + " Name not found.");
        }
        return name;
    }

    public int getRssi() throws NoSuchFieldException {
        if (rssi == RSSI_UNSET) {
            throw new NoSuchFieldException(logPrefix + " RSSI not found.");
        }
        return rssi;
    }

    public int getTxPower() throws NoSuchFieldException {
        if (txPower == TXPOWER_UNSET) {
            throw new NoSuchFieldException(logPrefix + " TxPower not found.");
        }
        return txPower;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getDateString() {
        return new SimpleDateFormat(dateFormat).format(date);
    }

    @Override
    public String toString() {
        return logPrefix +
                " name: " + name +
                " rssi: " + (rssi != RSSI_UNSET ? String.valueOf(rssi) : "n/a") +
                " txPower: " + (txPower != TXPOWER_UNSET ? String.valueOf(txPower) : "n/a") +
                " date: " + getDateString();
    }
}
