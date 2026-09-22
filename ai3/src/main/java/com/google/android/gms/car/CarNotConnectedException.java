package com.google.android.gms.car;
public class CarNotConnectedException extends Exception {
    public CarNotConnectedException(){}
    public CarNotConnectedException(Exception e){super(e);}
    public CarNotConnectedException(String s){super(s);}
    public CarNotConnectedException(String s,Throwable t){super(s,t);}
}
