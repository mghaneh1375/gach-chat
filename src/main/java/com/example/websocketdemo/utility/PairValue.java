package com.example.websocketdemo.utility;

public class PairValue {

    private final Object Key;
    private final Object Value;

    public PairValue(Object Key, Object Value)
    {
        this.Key = Key;
        this.Value = Value;
    }

    public Object getKey() {
        return Key;
    }

    public Object getValue() {
        return Value;
    }

}