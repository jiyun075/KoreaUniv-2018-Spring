// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BEROutputStream.java

package org.snmp4j.asn1;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class BEROutputStream extends OutputStream
{

    public BEROutputStream()
    {
        offset = 0;
        buffer = null;
    }

    public BEROutputStream(ByteBuffer buffer)
    {
        offset = 0;
        this.buffer = buffer;
        offset = buffer.position();
    }

    public void write(int b)
        throws IOException
    {
        buffer.put((byte)b);
    }

    public void write(byte b[])
        throws IOException
    {
        buffer.put(b);
    }

    public void write(byte b[], int off, int len)
        throws IOException
    {
        buffer.put(b, off, len);
    }

    public void close()
        throws IOException
    {
    }

    public void flush()
        throws IOException
    {
    }

    public ByteBuffer rewind()
    {
        return (ByteBuffer)buffer.position(offset);
    }

    public ByteBuffer getBuffer()
    {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer)
    {
        this.buffer = buffer;
        offset = buffer.position();
    }

    public void setFilledBuffer(ByteBuffer buffer)
    {
        this.buffer = buffer;
        offset = buffer.position();
        buffer.position(buffer.limit());
    }

    private ByteBuffer buffer;
    private int offset;
}
