package com.google.android.gms.car;
import java.io.IOException;
public interface CarVendorExtensionManager {
    interface CarVendorExtensionListener { void onData(byte[] data); }
    byte[] getServiceData() throws CarNotConnectedException;
    String getServiceName() throws CarNotConnectedException;
    void registerListener(CarVendorExtensionListener l);
    void release();
    void sendData(byte[] data) throws CarNotConnectedException, IOException;
    void sendData(byte[] data,int off,int len) throws CarNotConnectedException, IOException;
    void unregisterListener();
}
