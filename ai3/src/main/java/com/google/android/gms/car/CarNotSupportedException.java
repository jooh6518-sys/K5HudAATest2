package com.google.android.gms.car;
public class CarNotSupportedException extends Exception {
    public CarNotSupportedException(){}
    public CarNotSupportedException(Exception e){super(e);}
    public CarNotSupportedException(String s){super(s);}
    public CarNotSupportedException(String s,Throwable t){super(s,t);}
}
