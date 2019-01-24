// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BER.java

package org.snmp4j.asn1;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

// Referenced classes of package org.snmp4j.asn1:
//            BERInputStream, BERSerializable

public class BER
{
    public static class MutableByte
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

        public MutableByte()
        {
            value = 0;
        }

        public MutableByte(byte value)
        {
            this.value = 0;
            setValue(value);
        }
    }


    public BER()
    {
    }

    public static void encodeHeader(OutputStream os, int type, int length)
        throws IOException
    {
        os.write(type);
        encodeLength(os, length);
    }

    public static void encodeHeader(OutputStream os, int type, int length, int numBytesLength)
        throws IOException
    {
        os.write(type);
        encodeLength(os, length, numBytesLength);
    }

    public static int getBERLengthOfLength(int length)
    {
        if(length < 0)
            return 5;
        if(length < 128)
            return 1;
        if(length <= 255)
            return 2;
        if(length <= 65535)
            return 3;
        return length > 0xffffff ? 5 : 4;
    }

    public static void encodeLength(OutputStream os, int length)
        throws IOException
    {
        if(length < 0)
        {
            os.write(-124);
            os.write(length >> 24 & 0xff);
            os.write(length >> 16 & 0xff);
            os.write(length >> 8 & 0xff);
            os.write(length & 0xff);
        } else
        if(length < 128)
            os.write(length);
        else
        if(length <= 255)
        {
            os.write(-127);
            os.write(length);
        } else
        if(length <= 65535)
        {
            os.write(-126);
            os.write(length >> 8 & 0xff);
            os.write(length & 0xff);
        } else
        if(length <= 0xffffff)
        {
            os.write(-125);
            os.write(length >> 16 & 0xff);
            os.write(length >> 8 & 0xff);
            os.write(length & 0xff);
        } else
        {
            os.write(-124);
            os.write(length >> 24 & 0xff);
            os.write(length >> 16 & 0xff);
            os.write(length >> 8 & 0xff);
            os.write(length & 0xff);
        }
    }

    public static void encodeLength(OutputStream os, int length, int numLengthBytes)
        throws IOException
    {
        os.write(numLengthBytes | 0xffffff80);
        for(int i = (numLengthBytes - 1) * 8; i >= 0; i -= 8)
            os.write(length >> i & 0xff);

    }

    public static void encodeInteger(OutputStream os, byte type, int value)
        throws IOException
    {
        int integer = value;
        int intsize = 4;
        int mask;
        for(mask = 0xff800000; ((integer & mask) == 0 || (integer & mask) == mask) && intsize > 1; integer <<= 8)
            intsize--;

        encodeHeader(os, type, intsize);
        mask = 0xff000000;
        while(intsize-- > 0) 
        {
            os.write((integer & mask) >> 24);
            integer <<= 8;
        }
    }

    public static void encodeBigInteger(OutputStream os, byte type, BigInteger value)
        throws IOException
    {
        byte bytes[] = value.toByteArray();
        encodeHeader(os, type, bytes.length);
        os.write(bytes);
    }

    public static int getBigIntegerBERLength(BigInteger value)
    {
        int length = value.toByteArray().length;
        return length + getBERLengthOfLength(length) + 1;
    }

    public static void encodeUnsignedInteger(OutputStream os, byte type, long value)
        throws IOException
    {
        int len = 1;
        if((value >> 24 & 255L) != 0L)
            len = 4;
        else
        if((value >> 16 & 255L) != 0L)
            len = 3;
        else
        if((value >> 8 & 255L) != 0L)
            len = 2;
        if((value >> 8 * (len - 1) & 128L) != 0L)
            len++;
        encodeHeader(os, type, len);
        if(len == 5)
        {
            os.write(0);
            for(int x = 1; x < len; x++)
                os.write((int)(value >> (8 * (4 - x) & 0xff)));

        } else
        {
            for(int x = 0; x < len; x++)
                os.write((int)(value >> (8 * (len - 1 - x) & 0xff)));

        }
    }

    public static void encodeString(OutputStream os, byte type, byte string[])
        throws IOException
    {
        encodeHeader(os, type, string.length);
        os.write(string);
    }

    public static void encodeSequence(OutputStream os, byte type, int length)
        throws IOException
    {
        os.write(type);
        encodeLength(os, length);
    }

    public static int getOIDLength(int value[])
    {
        int length = 1;
        if(value.length > 1)
            length = getSubIDLength(value[0] * 40 + value[1]);
        for(int i = 2; i < value.length; i++)
            length += getSubIDLength(value[i]);

        return length;
    }

    private static int getSubIDLength(int subID)
    {
        long v = (long)subID & 0xffffffffL;
        int length;
        if(v < 128L)
            length = 1;
        else
        if(v < 16384L)
            length = 2;
        else
        if(v < 0x200000L)
            length = 3;
        else
        if(v < 0x10000000L)
            length = 4;
        else
            length = 5;
        return length;
    }

    public static void encodeOID(OutputStream os, byte type, int oid[])
        throws IOException
    {
        encodeHeader(os, type, getOIDLength(oid));
        int encodedLength = oid.length;
        int rpos = 0;
        if(oid.length < 2)
        {
            os.write(0);
            encodedLength = 0;
        } else
        {
            int firstSubID = oid[0];
            if(checkFirstSubID012 && (firstSubID < 0 || firstSubID > 2))
                throw new IOException("Invalid first sub-identifier (must be 0, 1, or 2)");
            encodeSubID(os, oid[1] + firstSubID * 40);
            encodedLength -= 2;
            rpos = 2;
        }
        while(encodedLength-- > 0) 
            encodeSubID(os, oid[rpos++]);
    }

    private static void encodeSubID(OutputStream os, int subID)
        throws IOException
    {
        long subid = (long)subID & 0xffffffffL;
        if(subid < 127L)
        {
            os.write((int)subid & 0xff);
        } else
        {
            long mask = 127L;
            long bits = 0L;
            long testmask = 127L;
            for(long testbits = 0L; testmask != 0L; testbits += 7L)
            {
                if((subid & testmask) > 0L)
                {
                    mask = testmask;
                    bits = testbits;
                }
                testmask <<= 7;
            }

            while(mask != 127L) 
            {
                if(mask == 0x1e00000L)
                    mask = 0xfe00000L;
                os.write((int)((subid & mask) >> (int)bits | -128L));
                mask >>= 7;
                bits -= 7L;
            }
            os.write((int)(subid & mask));
        }
    }

    public static void encodeUnsignedInt64(OutputStream os, byte type, long value)
        throws IOException
    {
        int len;
        for(len = 8; len > 1 && (value >> 8 * (len - 1) & 255L) == 0L; len--);
        if((value >> 8 * (len - 1) & 128L) != 0L)
            len++;
        encodeHeader(os, type, len);
        if(len == 9)
        {
            os.write(0);
            len--;
        }
        for(int x = 0; x < len; x++)
            os.write((int)(value >> (8 * (len - 1 - x) & 0xff)));

    }

    public static int decodeLength(BERInputStream is)
        throws IOException
    {
        return decodeLength(is, true);
    }

    public static int decodeLength(BERInputStream is, boolean checkLength)
        throws IOException
    {
        int length = 0;
        int lengthbyte = is.read();
        if((lengthbyte & 0xffffff80) > 0)
        {
            lengthbyte &= 0x7f;
            if(lengthbyte == 0)
                throw new IOException("Indefinite lengths are not supported");
            if(lengthbyte > 4)
                throw new IOException("Data length > 4 bytes are not supported!");
            for(int i = 0; i < lengthbyte; i++)
            {
                int l = is.read() & 0xff;
                length |= l << 8 * (lengthbyte - 1 - i);
            }

            if(length < 0)
                throw new IOException("SNMP does not support data lengths > 2^31");
        } else
        {
            length = lengthbyte & 0xff;
        }
        if(checkLength)
            checkLength(is, length);
        return length;
    }

    public static int decodeHeader(BERInputStream is, MutableByte type, boolean checkLength)
        throws IOException
    {
        byte t = (byte)is.read();
        if((t & 0x1f) == 31)
        {
            throw new IOException((new StringBuilder()).append("Cannot process extension IDs").append(getPositionMessage(is)).toString());
        } else
        {
            type.setValue(t);
            return decodeLength(is, checkLength);
        }
    }

    public static int decodeHeader(BERInputStream is, MutableByte type)
        throws IOException
    {
        return decodeHeader(is, type, true);
    }

    public static int decodeInteger(BERInputStream is, MutableByte type)
        throws IOException
    {
        int value = 0;
        type.setValue((byte)is.read());
        if(type.value != 2 && type.value != 67 && type.value != 65)
            throw new IOException((new StringBuilder()).append("Wrong ASN.1 type. Not an integer: ").append(type.value).append(getPositionMessage(is)).toString());
        int length = decodeLength(is);
        if(length > 4)
            throw new IOException((new StringBuilder()).append("Length greater than 32bit are not supported  for integers: ").append(getPositionMessage(is)).toString());
        int b = is.read() & 0xff;
        if((b & 0x80) > 0)
            value = -1;
        do
        {
            if(length-- <= 0)
                break;
            value = value << 8 | b;
            if(length > 0)
                b = is.read();
        } while(true);
        return value;
    }

    public static BigInteger decodeBigInteger(BERInputStream is, MutableByte type)
        throws IOException
    {
        type.setValue((byte)is.read());
        if(type.value != 2)
            throw new IOException((new StringBuilder()).append("Wrong ASN.1 type. Not an INTEGER: ").append(type.value).append(getPositionMessage(is)).toString());
        int length = decodeLength(is);
        if(length < 0)
            throw new IOException((new StringBuilder()).append("Length greater than 2147483647 are not supported  for integers: ").append(getPositionMessage(is)).toString());
        byte bytes[] = new byte[length];
        int actualRead = is.read(bytes);
        if(actualRead != length)
            throw new IOException((new StringBuilder()).append("Length of INTEGER (").append(length).append(") is greater than number of bytes left in BER stream: ").append(actualRead).toString());
        else
            return new BigInteger(bytes);
    }

    private static String getPositionMessage(BERInputStream is)
    {
        return (new StringBuilder()).append(" at position ").append(is.getPosition()).toString();
    }

    public static long decodeUnsignedInteger(BERInputStream is, MutableByte type)
        throws IOException
    {
        long value = 0L;
        type.setValue((byte)is.read());
        if(type.value != 2 && type.value != 67 && type.value != 65 && type.value != 66 && type.value != 71)
            throw new IOException((new StringBuilder()).append("Wrong ASN.1 type. Not an unsigned integer: ").append(type.value).append(getPositionMessage(is)).toString());
        int length = decodeLength(is);
        int b = is.read();
        if(length > 5 || length > 4 && b != 0)
            throw new IOException((new StringBuilder()).append("Only 32bit unsigned integers are supported").append(getPositionMessage(is)).toString());
        if(b == 0)
        {
            if(length > 1)
                b = is.read();
            length--;
        }
        for(int i = 0; i < length; i++)
        {
            value = value << 8 | (long)(b & 0xff);
            if(i + 1 < length)
                b = is.read();
        }

        return value;
    }

    public static byte[] decodeString(BERInputStream is, MutableByte type)
        throws IOException
    {
        type.setValue((byte)is.read());
        if(type.value != 4 && type.value != 36 && type.value != 64 && type.value != 68 && type.value != 3 && type.value != 69)
            throw new IOException((new StringBuilder()).append("Wrong ASN.1 type. Not a string: ").append(type.value).append(getPositionMessage(is)).toString());
        int length = decodeLength(is);
        byte value[] = new byte[length];
        if(length > 0)
        {
            int read = is.read(value, 0, length);
            if(read < 0 || read < length)
                throw new IOException((new StringBuilder()).append("Wrong string length ").append(read).append(" < ").append(length).toString());
        }
        return value;
    }

    public static int[] decodeOID(BERInputStream is, MutableByte type)
        throws IOException
    {
        type.setValue((byte)is.read());
        if(type.value != 6)
            throw new IOException((new StringBuilder()).append("Wrong type. Not an OID: ").append(type.value).append(getPositionMessage(is)).toString());
        int length = decodeLength(is);
        int oid[] = new int[length + 2];
        if(length == 0)
            oid[0] = oid[1] = 0;
        int pos = 1;
        int subidentifier;
        while(length > 0) 
        {
            subidentifier = 0;
            int b;
            do
            {
                int next = is.read();
                if(next < 0)
                    throw new IOException((new StringBuilder()).append("Unexpected end of input stream").append(getPositionMessage(is)).toString());
                b = next & 0xff;
                subidentifier = (subidentifier << 7) + (b & 0x7f);
            } while(--length > 0 && (b & 0xffffff80) != 0);
            oid[pos++] = subidentifier;
        }
        subidentifier = oid[1];
        if(subidentifier == 43)
        {
            oid[0] = 1;
            oid[1] = 3;
        } else
        if(subidentifier >= 0 && subidentifier < 80)
        {
            if(subidentifier < 40)
            {
                oid[0] = 0;
                oid[1] = subidentifier;
            } else
            {
                oid[0] = 1;
                oid[1] = subidentifier - 40;
            }
        } else
        {
            oid[0] = 2;
            oid[1] = subidentifier - 80;
        }
        if(pos < 2)
            pos = 2;
        int value[] = new int[pos];
        System.arraycopy(oid, 0, value, 0, pos);
        return value;
    }

    public static void decodeNull(BERInputStream is, MutableByte type)
        throws IOException
    {
        type.setValue((byte)(is.read() & 0xff));
        if(type.value != 5 && type.value != -128 && type.value != -127 && type.value != -126)
            throw new IOException((new StringBuilder()).append("Wrong ASN.1 type. Is not null: ").append(type.value).append(getPositionMessage(is)).toString());
        int length = decodeLength(is);
        if(length != 0)
            throw new IOException((new StringBuilder()).append("Invalid Null encoding, length is not zero: ").append(length).append(getPositionMessage(is)).toString());
        else
            return;
    }

    public static long decodeUnsignedInt64(BERInputStream is, MutableByte type)
        throws IOException
    {
        type.setValue((byte)is.read());
        if(type.value != 2 && type.value != 70)
            throw new IOException((new StringBuilder()).append("Wrong type. Not an integer 64: ").append(type.value).append(getPositionMessage(is)).toString());
        int length = decodeLength(is);
        int b = is.read() & 0xff;
        if(length > 9)
            throw new IOException((new StringBuilder()).append("Invalid 64bit unsigned integer length: ").append(length).append(getPositionMessage(is)).toString());
        if(b == 0)
        {
            if(length > 1)
                b = is.read();
            length--;
        }
        long value = 0L;
        for(int i = 0; i < length; i++)
        {
            value = value << 8 | (long)(b & 0xff);
            if(i + 1 < length)
                b = is.read();
        }

        return value;
    }

    public static boolean isCheckSequenceLength()
    {
        return checkSequenceLength;
    }

    public static void setCheckSequenceLength(boolean checkSequenceLen)
    {
        checkSequenceLength = checkSequenceLen;
    }

    public static void checkSequenceLength(int expectedLength, BERSerializable sequence)
        throws IOException
    {
        if(isCheckSequenceLength() && expectedLength != sequence.getBERPayloadLength())
            throw new IOException((new StringBuilder()).append("The actual length of the SEQUENCE object ").append(sequence.getClass().getName()).append(" is ").append(sequence.getBERPayloadLength()).append(", but ").append(expectedLength).append(" was expected").toString());
        else
            return;
    }

    public static void checkSequenceLength(int expectedLength, int actualLength, BERSerializable sequence)
        throws IOException
    {
        if(isCheckSequenceLength() && expectedLength != actualLength)
            throw new IOException((new StringBuilder()).append("The actual length of the SEQUENCE object ").append(sequence.getClass().getName()).append(" is ").append(actualLength).append(", but ").append(expectedLength).append(" was expected").toString());
        else
            return;
    }

    private static void checkLength(BERInputStream is, int length)
        throws IOException
    {
        if(!checkValueLength)
            return;
        if(length < 0 || length > is.getAvailableBytes())
            throw new IOException((new StringBuilder()).append("The encoded length ").append(length).append(" exceeds the number of bytes left in input").append(getPositionMessage(is)).append(" which actually is ").append(is.getAvailableBytes()).toString());
        else
            return;
    }

    public boolean isCheckValueLength()
    {
        return checkValueLength;
    }

    public void setCheckValueLength(boolean checkValueLength)
    {
        checkValueLength = checkValueLength;
    }

    public static boolean isCheckFirstSubID012()
    {
        return checkFirstSubID012;
    }

    public static void setCheckFirstSubID012(boolean checkFirstSubID012)
    {
        checkFirstSubID012 = checkFirstSubID012;
    }

    public static final byte ASN_BOOLEAN = 1;
    public static final byte ASN_INTEGER = 2;
    public static final byte ASN_BIT_STR = 3;
    public static final byte ASN_OCTET_STR = 4;
    public static final byte ASN_NULL = 5;
    public static final byte ASN_OBJECT_ID = 6;
    public static final byte ASN_SEQUENCE = 16;
    public static final byte ASN_SET = 17;
    public static final byte ASN_UNIVERSAL = 0;
    public static final byte ASN_APPLICATION = 64;
    public static final byte ASN_CONTEXT = -128;
    public static final byte ASN_PRIVATE = -64;
    public static final byte ASN_PRIMITIVE = 0;
    public static final byte ASN_CONSTRUCTOR = 32;
    public static final byte ASN_LONG_LEN = -128;
    public static final byte ASN_EXTENSION_ID = 31;
    public static final byte ASN_BIT8 = -128;
    public static final byte INTEGER = 2;
    public static final byte INTEGER32 = 2;
    public static final byte BITSTRING = 3;
    public static final byte OCTETSTRING = 4;
    public static final byte NULL = 5;
    public static final byte OID = 6;
    public static final byte SEQUENCE = 48;
    public static final byte IPADDRESS = 64;
    public static final byte COUNTER = 65;
    public static final byte COUNTER32 = 65;
    public static final byte GAUGE = 66;
    public static final byte GAUGE32 = 66;
    public static final byte TIMETICKS = 67;
    public static final byte OPAQUE = 68;
    public static final byte COUNTER64 = 70;
    public static final int NOSUCHOBJECT = 128;
    public static final int NOSUCHINSTANCE = 129;
    public static final int ENDOFMIBVIEW = 130;
    private static final int LENMASK = 255;
    public static final int MAX_OID_LENGTH = 127;
    private static boolean checkSequenceLength = true;
    private static boolean checkValueLength = true;
    private static boolean checkFirstSubID012 = true;

}
