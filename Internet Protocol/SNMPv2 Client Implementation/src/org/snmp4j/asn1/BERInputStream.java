// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BERInputStream.java

package org.snmp4j.asn1;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class BERInputStream extends InputStream
{

    public BERInputStream(ByteBuffer buffer)
    {
        this.buffer = buffer;
        buffer.mark();
    }

    public ByteBuffer getBuffer()
    {
        return buffer;
    }

    public void setBuffer(ByteBuffer buf)
    {
        buffer = buf;
    }

    public int read()
        throws IOException
    {
        try
        {
            return buffer.get() & 0xff;
        }
        catch(BufferUnderflowException ex)
        {
            throw new IOException((new StringBuilder()).append("Unexpected end of input stream at position ").append(getPosition()).toString());
        }
    }

    public int available()
        throws IOException
    {
        return buffer.remaining();
    }

    public void close()
        throws IOException
    {
    }

    public synchronized void mark(int readlimit)
    {
        buffer.mark();
    }

    public boolean markSupported()
    {
        return true;
    }

    public int read(byte b[])
        throws IOException
    {
        if(buffer.remaining() <= 0)
        {
            return -1;
        } else
        {
            int read = Math.min(buffer.remaining(), b.length);
            buffer.get(b, 0, read);
            return read;
        }
    }

    public int read(byte b[], int off, int len)
        throws IOException
    {
        if(buffer.remaining() <= 0 && len > 0)
        {
            return -1;
        } else
        {
            int read = Math.min(buffer.remaining(), b.length);
            buffer.get(b, off, len);
            return read;
        }
    }

    public synchronized void reset()
        throws IOException
    {
        buffer.reset();
    }

    public long skip(long n)
        throws IOException
    {
        long skipped = Math.min(buffer.remaining(), n);
        buffer.position((int)((long)buffer.position() + skipped));
        return skipped;
    }

    public long getPosition()
    {
        return (long)buffer.position();
    }

    public boolean isMarked()
    {
        return true;
    }

    public int getAvailableBytes()
    {
        return buffer.limit();
    }

    private ByteBuffer buffer;
}
