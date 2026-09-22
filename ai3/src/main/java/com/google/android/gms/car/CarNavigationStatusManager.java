package com.google.android.gms.car;
public interface CarNavigationStatusManager {
    interface CarNavigationStatusListener {
        void onStart(int i,int i2,int i3,int i4,int i5);
        void onStop();
    }
    void registerListener(CarNavigationStatusListener l) throws CarNotConnectedException;
    boolean sendNavigationStatus(int status) throws CarNotConnectedException;
    boolean sendNavigationTurnDistanceEvent(int distanceMeters,int timeSeconds,
            int displayDistanceMillis,int displayDistanceUnit) throws CarNotConnectedException;
    boolean sendNavigationTurnEvent(int event,String road,int turnAngle,int turnNumber,
            byte[] image,int turnSide) throws CarNotConnectedException;
    void unregisterListener();
}
