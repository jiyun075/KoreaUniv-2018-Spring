// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BERSerializable.java

package org.snmp4j.asn1;

import java.io.IOException;
import java.io.OutputStream;

// Referenced classes of package org.snmp4j.asn1:
//            BERInputStream

public interface BERSerializable
{

    public abstract int getBERLength();

    public abstract int getBERPayloadLength();

    public abstract void decodeBER(BERInputStream berinputstream)
        throws IOException;

    public abstract void encodeBER(OutputStream outputstream)
        throws IOException;
}
