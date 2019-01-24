package IPTP;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

import org.snmp4j.asn1.*;

public class IPTP {
   public static int main_requestID = 0;
   //PDU type
   static final int getRequest = 0xa0;
   static final int getNextRequest = 0xa1;
   static final int setRequest = 0xa3;
   //Data Type
   static final int INTEGER = 0x02;
   static final int STRING = 0x04;
   static final int OID = 0x06;
   static final int NULL = 0x05;
   //state for program UI
   public static boolean state = true;
   public static int sel;
   //for snmpClient thread
   public static String oid,value,type;
   public static int bufflen;

   public static String log;
   public static String logResult;

   public static void main(String args[]) {
	   try {
       logResult = "**SNMP Walk Log**\r\n\r\n";
	         while(state)
	         {System.out.println("Select function with number");
            System.out.println("1. snmpGet");
	         System.out.println("2. snmpSet");
	         System.out.println("3. snmpWalk");
           System.out.println("0. Terminate the program");

	         Scanner sc = new Scanner(System.in);
	         Scanner sc2 = new Scanner(System.in);
	         Scanner sc3 = new Scanner(System.in);
	         Scanner sc4 = new Scanner(System.in);
	         sel = sc.nextInt();
	         if(sel == 0) {
	           state = false;
	           break;
	         }
           System.out.print("OID: ");
	         oid = sc2.nextLine();
           if(oid.isEmpty()){
             System.out.print("Error: Wrong input");
             break;
           }

	         DatagramSocket socket;
	         socket = new DatagramSocket();

	         if (sel == 1) {
	            // snmp get
	            snmpClient snmp = new snmpClient(socket, oid);
	            snmp.run();
	            System.out.println("");
	         }

	         else if (sel == 2) {
	            // snmp set
              System.out.print("Value: ");
	            value = sc3.nextLine();
              System.out.print("Type of Value: ");
	            type = sc4.nextLine();

              if(value.isEmpty() || type.isEmpty()){
                System.out.print("Error: Wrong input");
                break;
              }
	            snmpClient snmp = new snmpClient(socket, oid, value, type);
	            snmp.run();
	            System.out.println("");
	         }
	         else if (sel == 3) {
	            // snmp walk
	        	 log = "Input OID: "+ oid+ "\r\n";
	            snmpClient snmp = new snmpClient(socket, oid);
	            snmp.run();
	            System.out.println("End of SNMP Walk\n");
              logResult = logResult.concat(log+"\r\n");
	         }
	         socket.close();}
           try {
       makingLog(logResult);
     } catch (Exception e) {
       // TODO Auto-generated catch block
       e.printStackTrace();
     }
	      } catch (SocketException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   //OID conversion string to int array
   public static int[] toIntArr(String oid) {
      String[] str = oid.split("\\.");
      if (str[0].equals("iso")) {str[0] = "1";}
      if (str[1].equals("org")) {str[0] = "3";}
      if (str[2].equals("dod")) {str[0] = "6";}
      int[] result = new int[str.length];
      for (int i=0; i<str.length; i++) {
        result[i] = Integer.parseInt(str[i]);
      }
      return result;
   }

   //OID conversion (arr shaped) string to string
   public static String makeResultOID(String oid) {
     String result = oid.substring(1, oid.length() - 1);
     result = result.replace(", ", ".");
     result = "iso" + result.substring(1);
     return result;
   }

   public static void makingLog(String log) throws Exception {
     File logfile = new File("SNMPwalkLog.txt");
	   FileWriter fw = new FileWriter(logfile);
	   String text = log;
	   fw.write(log);
     fw.flush();
	   fw.close();
   }

   ////////////////////////
   // ** SNMP Client ** //
   ////////////////////////
   public static class snmpClient extends Thread {
      public static int requestID = main_requestID;
      public int[] oid, init, respOID;
      public String value;
      public int type;
      DatagramSocket socket;
      String receive;
      boolean nextExist = true;
      public snmpClient (DatagramSocket socket, String oid) {
         this.socket = socket;
         this.oid = toIntArr(oid);
         this.value = null;
         this.type = NULL;
      }
      public snmpClient (DatagramSocket socket, String oid, String value, String type) {
         this.socket = socket;
         this.oid = toIntArr(oid);
         this.value = value;
         if (type.equals("integer")) this.type = INTEGER;
         else if (type.equals("string")) this.type = STRING;
         else if (type.equals("oid")) this.type = OID;
         else this.type = NULL;
      }

      /////////////////////////
      // ** Encoding part ** //
      /////////////////////////
      public byte[] encodeData() {
         if (sel == 1) {
            try {
               ByteBuffer buf = ByteBuffer.allocate(1024);
               BEROutputStream berOutput = new BEROutputStream(buf);
               ByteBuffer temp = ByteBuffer.allocate(1024);
               BEROutputStream tempBO = new BEROutputStream(temp);
               BER.encodeOID(tempBO, BER.OID, this.oid);
               BER.encodeHeader(tempBO, BER.NULL, 0);
               int varLength = tempBO.getBuffer().position();
               // for calculate variable length...

               BER.encodeHeader(berOutput, BER.SEQUENCE, varLength + 26); // sequence
               BER.encodeInteger(berOutput, BER.INTEGER, 1); // version
               BER.encodeString(berOutput, BER.OCTETSTRING, "public".getBytes()); // commnunity
               BER.encodeHeader(berOutput, (byte)getRequest, varLength + 13); // PDU header
               BER.encodeInteger(berOutput, BER.INTEGER, requestID); // Request ID
               BER.encodeInteger(berOutput, BER.INTEGER, 0); // error status
               BER.encodeInteger(berOutput, BER.INTEGER, 0); // error index
               BER.encodeHeader(berOutput, BER.SEQUENCE, varLength+2); // sequence of
               BER.encodeHeader(berOutput, BER.SEQUENCE, varLength); // sequence
               BER.encodeOID(berOutput, BER.OID, this.oid); // oid
               BER.encodeHeader(berOutput, BER.NULL, 0); // value

               bufflen = 3;
               for (int i=0;i<oid.length;i++) {
                  if (oid[i] == 0)
                     ++bufflen;
               } // if oid includes 0 values, buffer length should increase
               for (int i=0; i<1024;i++) {
                  if (buf.get(i) != 0) {
                     ++bufflen;
                  }
                  //calculate buffer length
               }
               byte[] retBuf = berOutput.getBuffer().array();
               return retBuf;
            } catch (IOException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
               return null;
            }

         } else if (sel == 2) {
            try {
               ByteBuffer buf1 = ByteBuffer.allocate(1024);
               BEROutputStream berOutput1 = new BEROutputStream(buf1);
               ByteBuffer temp1 = ByteBuffer.allocate(1024);
               BEROutputStream tempBO1 = new BEROutputStream(temp1);
               BER.encodeOID(tempBO1, BER.OID, this.oid);
               if (this.type == INTEGER) BER.encodeInteger(tempBO1, BER.INTEGER, Integer.parseInt(this.value));
               else if (this.type == STRING) BER.encodeString(tempBO1, BER.OCTETSTRING, this.value.getBytes());
               else if (this.type == NULL) BER.encodeHeader(tempBO1, BER.NULL, 0);
               else if (this.type == OID) BER.encodeOID(tempBO1, BER.OID, toIntArr(this.value));
               int varLength = tempBO1.getBuffer().position();
               // for calculate variable length...

               BER.encodeHeader(berOutput1, BER.SEQUENCE, varLength + 25); // sequence
               BER.encodeInteger(berOutput1, BER.INTEGER, 1); // version
               BER.encodeString(berOutput1, BER.OCTETSTRING, "write".getBytes()); // commnunity
               BER.encodeHeader(berOutput1, (byte)setRequest, varLength + 13); // PDU header
               BER.encodeInteger(berOutput1, BER.INTEGER, requestID); // Request ID
               BER.encodeInteger(berOutput1, BER.INTEGER, 0); // error status
               BER.encodeInteger(berOutput1, BER.INTEGER, 0); // error index
               BER.encodeHeader(berOutput1, BER.SEQUENCE, varLength+2); // sequence of
               BER.encodeHeader(berOutput1, BER.SEQUENCE, varLength); // sequence
               BER.encodeOID(berOutput1, BER.OID, this.oid); // oid

               if (this.type == INTEGER) BER.encodeInteger(berOutput1, BER.INTEGER, Integer.parseInt(this.value));
               else if (this.type == STRING) BER.encodeString(berOutput1, BER.OCTETSTRING, this.value.getBytes());
               else if (this.type == OID) BER.encodeOID(berOutput1, BER.OID, toIntArr(this.value));
               else BER.encodeHeader(berOutput1, BER.NULL, 0);
               // value

               bufflen = 2;
               for (int i=0; i<1024;i++) {
                  if (buf1.get(i) != 0) {
                     ++bufflen;
                  }
                  //calculate buffer length
               }
               byte[] retBuf = berOutput1.getBuffer().array();
               return retBuf;

            } catch (IOException e) {
               e.printStackTrace();
               return null;
            }
         } else if (sel == 3) {
            try {
               ByteBuffer buf2 = ByteBuffer.allocate(1024);
               BEROutputStream berOutput2 = new BEROutputStream(buf2);
               ByteBuffer temp2 = ByteBuffer.allocate(1024);
               BEROutputStream tempBO2 = new BEROutputStream(temp2);
               BER.encodeOID(tempBO2, BER.OID, this.oid);
               BER.encodeHeader(tempBO2, BER.NULL, 0);
               int varLength = tempBO2.getBuffer().position();
               // for calculate variable length...

               BER.encodeHeader(berOutput2, BER.SEQUENCE, varLength + 26); // sequence
               BER.encodeInteger(berOutput2, BER.INTEGER, 1); // version
               BER.encodeString(berOutput2, BER.OCTETSTRING, "public".getBytes()); // commnunity
               BER.encodeHeader(berOutput2, (byte)getNextRequest, varLength + 13); // PDU header
               BER.encodeInteger(berOutput2, BER.INTEGER, requestID); // Request ID
               BER.encodeInteger(berOutput2, BER.INTEGER, 0); // error status
               BER.encodeInteger(berOutput2, BER.INTEGER, 0); // error index
               BER.encodeHeader(berOutput2, BER.SEQUENCE, varLength+2); // sequence of
               BER.encodeHeader(berOutput2, BER.SEQUENCE, varLength); // sequence
               BER.encodeOID(berOutput2, BER.OID, this.oid); // oid
               BER.encodeHeader(berOutput2, BER.NULL, 0); // value

               bufflen = 3;
               for (int i=0;i<oid.length;i++) {
                  if (oid[i] == 0)
                     ++bufflen;
               }
               // if oid includes 0 values, buffer length should increase
               for (int i=0; i<1024;i++) {
                  if (buf2.get(i) != 0) {
                     ++bufflen;
                  }
                  //calculate buffer length
               }
               byte[] retBuf = berOutput2.getBuffer().array();
               return retBuf;
            }  catch (Exception e) {
               e.printStackTrace();
               return null;
            }
         }
         return null;
      }

      /////////////////////////
      // ** Decoding part ** //
      /////////////////////////
      public String decodeData(byte[] buf) {
         BER.MutableByte mutData = new BER.MutableByte();
         String oid;
         int tag;
         ByteBuffer bytebuff = ByteBuffer.wrap(buf);
         BERInputStream berInput = new BERInputStream(bytebuff);

         if (sel == 3) {
            try {
               BER.decodeHeader(berInput, mutData); // decode snmp header
               BER.decodeInteger(berInput, mutData); // decode snmp ver
               BER.decodeString(berInput, mutData); // decode snmp community
               BER.decodeHeader(berInput, mutData); // decode pdu header
               BER.decodeInteger(berInput, mutData); // decode requestID
               BER.decodeInteger(berInput, mutData); // decode error status
               BER.decodeInteger(berInput, mutData); // decode error index
               BER.decodeHeader(berInput, mutData); // decode sequence of
               BER.decodeHeader(berInput, mutData); // decode sequence
               respOID = BER.decodeOID(berInput, mutData); // decode oid
               oid = Arrays.toString(respOID);
               tag = berInput.read();
               berInput.getBuffer().position(berInput.getBuffer().position() - 1);
               if (nextExist) {
                  nextExist = subtree(init, respOID);
                  this.oid = respOID;
                  makeResultOID(oid);
                  switch (tag) {
                  case BER.INTEGER: {
                     return makeResultOID(oid) + " =" + " INTEGER: " + Integer.toString(BER.decodeInteger(berInput, mutData));
                  }
                  case BER.OCTETSTRING: {
                     String temp = new String(BER.decodeString(berInput, mutData));
                     return makeResultOID(oid) + " =" + " STRING: " + temp;
                  }
                  case BER.OID: {
                     return makeResultOID(oid) + " =" + " OID: " + makeResultOID(Arrays.toString(BER.decodeOID(berInput, mutData)));
                  }
                  case BER.NOSUCHOBJECT: {
                     return makeResultOID(oid) + " =" + " No such object";
                  }
                  case BER.TIMETICKS: {
                     Integer timeticks = BER.decodeInteger(berInput, mutData);
                     return makeResultOID(oid) + " =" + " Timeticks: " + timeticks.toString();
                  }
                  default: {
                     return makeResultOID(oid) + " =" + " type undefined";
                  }
                  }
               }
               return null;
            } catch (Exception e) {
               System.out.println(e);
               return null;
            }
         } else {
            try {
               BER.decodeHeader(berInput, mutData); // decode snmp header
               BER.decodeInteger(berInput, mutData); // decode snmp ver
               BER.decodeString(berInput, mutData); // decode snmp community
               BER.decodeHeader(berInput, mutData); // decode pdu header
               BER.decodeInteger(berInput, mutData); // decode requestID
               BER.decodeInteger(berInput, mutData); // decode error status
               BER.decodeInteger(berInput, mutData); // decode error index
               BER.decodeHeader(berInput, mutData); // decode sequence of
               BER.decodeHeader(berInput, mutData); // decode sequence
               oid = Arrays.toString(BER.decodeOID(berInput, mutData)); // decode oid
               tag = berInput.read();
               berInput.getBuffer().position(berInput.getBuffer().position() - 1);
               switch (tag) {
               case BER.INTEGER: {
                  return makeResultOID(oid) + " =" + " INTEGER " + Integer.toString(BER.decodeInteger(berInput, mutData));
               }
               case BER.OCTETSTRING: {
                  String temp = new String(BER.decodeString(berInput, mutData));
                  return makeResultOID(oid) + " =" + " STRING " + temp;
               }
               case BER.OID: {
                  return makeResultOID(oid) + " =" + " OID " + makeResultOID(Arrays.toString(BER.decodeOID(berInput, mutData)));
               }
               case BER.NOSUCHOBJECT: {
                  return makeResultOID(oid) + " =" + " No such object";
               }
               case BER.TIMETICKS: {
                  Integer timeticks = BER.decodeInteger(berInput, mutData);
                  return makeResultOID(oid) + " =" + " Timeticks " + timeticks.toString();
               }
               default: {
                  return makeResultOID(oid) + " =" + " Type undefined";
               }
               }

            } catch (Exception e) {
               System.out.println(e);
               return null;
            }
         }
      }

      public void run() {
         try {
           //Host name & port
           InetAddress addr = InetAddress.getByName("kuwiden.iptime.org");
           int port = 11161;
           // SNMP Walk
           if (sel == 3) {
               init = this.oid;
               while (nextExist) {
                  ++requestID;
                  byte[] buf = encodeData(); // encode data
                  DatagramPacket packet = new DatagramPacket(buf, bufflen, addr, port);
                  socket.send(packet);
                  buf = new byte[1024];
                  packet = new DatagramPacket(buf, buf.length, addr, port);
                  socket.receive(packet);
                  receive = decodeData(packet.getData()); //decode data
                  if(receive != null && !receive.isEmpty() && !receive.equals("null ")) {
                    log = log.concat(receive+"\r\n");
                    System.out.println(receive);
                  }
               }
            } else {
               // SNMP get , set
               ++requestID;
               byte[] buf = encodeData(); // encode data
               DatagramPacket packet = new DatagramPacket(buf, bufflen, addr, port);
               socket.send(packet);
               buf = new byte[1024];
               packet = new DatagramPacket(buf, buf.length, addr, port);
               socket.receive(packet);
               receive = decodeData(packet.getData()); //decode data
               System.out.println(receive);
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   //SNMP Walk -> subtree
   public static boolean subtree(int[] init, int[] respOID){
      for (int i=0; i<init.length; i++) {
         if (init[i] != respOID[i]) return false;
      }
      return true;
   }
}
