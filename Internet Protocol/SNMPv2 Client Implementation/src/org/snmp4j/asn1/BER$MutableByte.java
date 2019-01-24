// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BER.java

package org.snmp4j.asn1;


// Referenced classes of package org.snmp4j.asn1:
//            BER

public class BER$MutableByte
{

    public void setValue(byte value)
    {
        this.value = value;
    }

    public byte getValue()
    {
        return value;
    }

    byte value;

    public BER$MutableByte()
    {
        value = 0;
    }

    public BER$MutableByte(byte value)
    {
        this.value = 0;
        setValue(value);
    }
}
