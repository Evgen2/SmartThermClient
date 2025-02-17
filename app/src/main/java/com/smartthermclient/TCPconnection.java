package com.smartthermclient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

class Msg1 implements Serializable
{   short cmd0; //команда
    short cmd; //команда
    short ind; //параметр
    byte[] Buf;
    int len;
    Msg1()
    {   Buf = new byte[120];
        cmd0 = cmd = ind = 0;
        len = 0;
    }
}

public class TCPconnection {
    private static final int CMD_HAND_SHAKE = 0x2020;
    private static final int CMD_IDENTIFY = 0x80;

    private static final byte[] HAND_SHAKE_INP=  "TCPiptEsT\0".getBytes();
    private static final byte[] HAND_SHAKE_OUT=  "ipTCPTeSt\0".getBytes();
    private static final int DS_SMARTSERVER = 0x1000;
    private static final int DS_OPENTHERM = 2;
    int timeoutAnswer;
    int timeoutConnect;
    String address;
    Socket socket;
    InputStream sin;
    OutputStream sout;
    DataInputStream in;
    DataOutputStream out;
    byte  Mac_Addr[] = new byte[6];

    int port;                   //номер порта на сервере
    short indcmd;  //
    int servertype;
    int servercode;
    int server_id;
    int server_Vers;
    int server_SubVers;
    int server_SubVers1;
    String server_date;
    String serverIdentify;
    String errmsg;
    Date Last_work;//время последнего принятого пакета
    boolean work;

    public TCPconnection()
    {   port = 0;
        indcmd = 0;
        socket = null;
        serverIdentify = null;
        errmsg = null;
        servertype = servercode = server_id = 0;
        server_Vers = server_SubVers = server_SubVers1 = 0;
        Mac_Addr[0] = Mac_Addr[1] = Mac_Addr[2] = Mac_Addr[3] = Mac_Addr[4] = Mac_Addr[5] = 0;
        timeoutAnswer = 3000;
        timeoutConnect = 3000;
        Last_work = new Date(0);
        work = false;
    }
/*  rc = 0 Ok
    rc = 1 UnknownHostException
    rc = 2 IOException ioe
    rc = 3 failed to connect
* */
    public int createTCPconnection(String _address, int serverPort, int _timeoutConnect) throws Exception
    {   int ierr = 0;
        address = _address;
        String str;
        timeoutConnect =_timeoutConnect;
/*
        try {
            InetAddress ipAddress = InetAddress.getByName(_address); // создаем объект который отображает вышеописанный IP-адрес.
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace();
            errmsg = uhe.toString();
            ierr = 1;
            return ierr;
        }

 */
        try {
            str =String.format("Connecting to %s port %d ", address, serverPort);
            errmsg = str;
            socket = new Socket(); // создаем сокет
            socket.connect(new InetSocketAddress(address, serverPort), timeoutConnect);
//                        socket = new Socket(address, serverPort); // создаем сокет используя IP-адрес и порт сервера.
//            socket.setSoTimeout(5000);
            // Берем входной и выходной потоки сокета, теперь можем получать и отсылать данные клиентом.
            sin = socket.getInputStream();
            sout = socket.getOutputStream();
            in = new DataInputStream(sin);
            out = new DataOutputStream(sout);
            System.out.println(out);
            errmsg = "Connected";
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace();
            errmsg = uhe.toString();
            ierr = 1;
        } catch (IOException ioe) {
                      ioe.printStackTrace();
            //           errmsg1 = ioe.getMessage();
            errmsg = ioe.toString();
            if(errmsg.contains("failed to connect"))
                ierr = 3;
            else
               ierr = 2;
        } finally {
            if(ierr > 0) {
                try {
                    if (socket != null) {
                        socket.close();
                        socket = null;
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                return ierr;
            }
        }

        return 0;
    }
    public void CloseConnection()
    {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            socket = null; //??
        }

    }
    /*  rc = 0 Ok
        rc = 1 Connection failed
        rc = 2 Connection ok, response wrong
        rc = 0x10 Connection ok, handshake wrong
    * */
    public int InitClientConnection() {
        int  l, rc, rc0;
        Msg1 ucmd = new Msg1();
        Msg1 outcmd = new Msg1();

//        System.out.format("L=%d\n", HAND_SHAKE_INP.length);
//        for (i = 0; i < 9; i++) {
//            System.out.format("%c", HAND_SHAKE_INP[i]);
//        }
        l = HAND_SHAKE_INP.length;
        ucmd.cmd = CMD_HAND_SHAKE;
        System.arraycopy(HAND_SHAKE_INP, 0, ucmd.Buf,0, HAND_SHAKE_INP.length);
        rc = SendAndConfirm( ucmd, l,  outcmd, l);
        if(rc != 0)
        {   System.out.printf("Connection to %s failed, rc=%d\n", address, rc);
            if(rc == 4 || rc == 5 || rc == 6)
                return 2;
            return 1;
        }
        l = HAND_SHAKE_OUT.length;
        rc0 = mem_cmp(HAND_SHAKE_OUT,outcmd.Buf,l);
        if(rc0 != 0)
        {   return 0x10;
        }
/*
        ucmd.cmd = CMD_IDENTIFY;
        rc =  SendAndConfirm2(ucmd, 6, outcmd,126, 6, 1,0x12);
        if(rc != 0)
            return 0x11;
        { int idcode, idtype, idNumber;
            short sl;
            byte  tmp[] = new byte[20];
            byte  tmpmac[] = new byte[6];

            sl = (short) ((outcmd.Buf[0]& 0xff) | (outcmd.Buf[1]&0xff) << 8);

            System.arraycopy(outcmd.Buf, 2,tmp,0,4);
            servertype = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            System.arraycopy(outcmd.Buf, 6,tmp,0,4);
            servercode = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
//   servertype  DS_SMARTSERVER
//               DS_OPENTHERM
            System.arraycopy(outcmd.Buf, 10,tmp,0,4);
            server_id = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            System.arraycopy(outcmd.Buf, 14,tmp,0,4);
            server_Vers = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            System.arraycopy(outcmd.Buf, 18,tmp,0,4);
            server_SubVers = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            System.arraycopy(outcmd.Buf, 22,tmp,0,4);
            server_SubVers1 = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            System.arraycopy(outcmd.Buf, 26,tmp,0,12);
            tmp[12] = 0; tmp[13] = 0;
            server_date = new String(tmp,0, 11);

            System.arraycopy( outcmd.Buf, 38, tmpmac,0,6);
            System.arraycopy(tmpmac,0, Mac_Addr, 0,6);

            serverIdentify = new String(outcmd.Buf,44, sl-44+2);
        }
*/

        return 0;
    }

    int SendAndConfirm(Msg1 ucmd, int len, Msg1 outcmd, int lenout)
    {
        return SendAndConfirm_v(ucmd, len, outcmd, lenout, lenout);

    }

    //послать буфер с подтверждением
    //rc = 0 Ok *
//rc = 1 Timeout
//rc = 2 прочие ошибки
//rc = 3 плохой сокет
//rc = 4 длина входного буфера неправильная
//rc = 5 получено не то количество байт, которое должно быть *
//rc = 6 ответ: неизвестная команда *
//rc = -1 CONNABORTED
//rc = -2 timeout 2, connection closed (?)

    int SendAndConfirm_v(Msg1 ucmd, int len, Msg1 outcmd, int lenout, int lenoutmax)
    {   long t0,  t;

        byte[] bufin = new byte[len+6];
        byte[] bufout = new byte[1500];
        int  nb=0, nba;
        ucmd.cmd0 =  0x22;
        indcmd = (short)((indcmd+1) & 0xffff);
        ucmd.ind =  indcmd;
        bufin[0] = (byte) (ucmd.cmd0 & 0xff);
        bufin[1] = (byte)((ucmd.cmd0>>8) & 0xff);
        bufin[2] = (byte) (ucmd.cmd  & 0xff);
        bufin[3] = (byte)((ucmd.cmd >>8) & 0xff);
        bufin[4] = (byte) (ucmd.ind  & 0xff);
        bufin[5] = (byte)((ucmd.ind >>8) & 0xff);
        System.arraycopy( ucmd.Buf, 0,bufin, 6, len);

//        System.out.format("len=%d bufin.length=%d\n", len,bufin.length);
//        for(i=0; i<bufin.length; i++)
//        {
//            System.out.format("%2x ", bufin[i]);
//        }
//        System.out.format("\n");
//        System.out.println(out);

        try{
            out.write(bufin); // отсылаем введенную строку текста серверу.
        }catch(IOException e){
            e.printStackTrace();
        }

        try{
            out.flush(); // заставляем поток закончить передачу данных.
        }catch(IOException e){
            e.printStackTrace();
        }
//        System.out.println("send done ");
        nba = 0;
//timeoutAnswer

        t0 = System.currentTimeMillis();

        do {

            try {
                nba = in.available();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if(nba < 6)
            {   t = System.currentTimeMillis();
                if(t - t0 > timeoutAnswer) {
                    if(nba == 0)
                        return -2;
                    else
                        return 1;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } while( nba < 6);

        try{
            nb = in.read(bufout); // ожидаем пока клиент пришлет
        }catch(IOException e){
            e.printStackTrace();
            return 2;
        }
        if(nb == -1)
            return -1; //conn
        if(nb < 6)
            return 5;
        outcmd.cmd0 = (short) ((bufout[0]& 0xff) | (bufout[1]&0xff) << 8);
        outcmd.cmd  = (short) ((bufout[2]& 0xff) | (bufout[3]&0xff) << 8);
        outcmd.ind  = (short) ((bufout[4]& 0xff) | (bufout[5]&0xff) << 8);
        if((outcmd.cmd0 &0x8000) == 0x8000)
        {//   if(verboze)
            //  { printf("\a\nКоманда %d (0x%x) неизвестна контроллеру\n", pm2->cmd, pm2->cmd);
            //  }
            return 6;
        }
        nba = nb - 6;
        outcmd.len = nba;

        if(nba <  lenout || nba > lenoutmax)
            return 5;
//        if(nb !=  lenout + 6)
//            return 5;

        System.arraycopy( bufout, 6,outcmd.Buf, 0, nba);
//        System.out.println("сервер sent me " +  nb + "bytes");
        Last_work = new Date();
        work = true;

        return 0;
    }
//послать буфер с подтверждением с переменной длиной ответа
//FirstByte - первый байт в данных ответа должен равняться FirstByte
//size0 - размер в байтах одного элемента ответа
//shiftLansw - смещение в байтах от начала ответа длины ответа в элементах (short int)
//             la = (int) *((short int *) &buff_out[shiftLansw]); - длина ответа в элеменатх
//             la * size0 + shiftLansw + 2; - общая длина ответа
// пример:	rc =  SendAndConfirm2((char *)&ucmd, 6, (char *)&outcmd, sizeof(outcmd), 6, 1,0x12);
//смещение 6, элементы по 1 байту, первый байт должен быть 0x12

    //rc = 0 Ok
//rc = 1 Timeout
//rc = 2 прочие ошибки
//rc = 3 плохой сокет
//rc = 4 длина входного буфера неправильная
//rc = 5 длина сообщения не соответствует переменной длине
//rc = 6 ответ: неизвестная команда *
//rc = 7 первый байт != FirstByte
//int TCPconnection::SendAndConfirm2(char bufin[], int len, char bufout[], int lenout, int shiftLansw, int size0, int FirstByte)
    int SendAndConfirm2(Msg1 ucmd, int len, Msg1 outcmd, int lenout, int shiftLansw, int size0, int FirstByte)
    {   long t0,  t;
        byte[] bufin = new byte[len+6];
        byte[] bufout = new byte[1500];
        short  sl;
        int  nb=0, nba, l;

        ucmd.cmd0 =  0x12;
        indcmd = (short)((indcmd+1) & 0xffff);
        ucmd.ind =  indcmd;
        bufin[0] = (byte) (ucmd.cmd0 & 0xff);
        bufin[1] = (byte)((ucmd.cmd0>>8) & 0xff);
        bufin[2] = (byte) (ucmd.cmd  & 0xff);
        bufin[3] = (byte)((ucmd.cmd >>8) & 0xff);
        bufin[4] = (byte) (ucmd.ind  & 0xff);
        bufin[5] = (byte)((ucmd.ind >>8) & 0xff);
        System.arraycopy( ucmd.Buf, 0,bufin, 6, len);

        try{
            out.write(bufin); // отсылаем введенную строку текста серверу.
        }catch(IOException e){
            e.printStackTrace();
        }

        try{
            out.flush(); // заставляем поток закончить передачу данных.
        }catch(IOException e){
            e.printStackTrace();
        }
        System.out.println("send done ");

        t0 = System.currentTimeMillis();

        do {
            try {
                nba = in.available();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if(nba < 6)
            {   t = System.currentTimeMillis();
                if(t - t0 > timeoutAnswer) {
                    if(nba == 0)
                        return -2;
                    else
                        return 1;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } while( nba < 6);

        try{
            nb = in.read(bufout); // ожидаем пока клиент пришлет
        }catch(IOException e){
            e.printStackTrace();
            return 1;
        }
        if(nb < shiftLansw + 2)
            return 5;

        if((bufout[0]& 0xff) != FirstByte)
            return 7;
        outcmd.cmd0 = (short) ((bufout[0]& 0xff) | (bufout[1]&0xff) << 8);
        outcmd.cmd  = (short) ((bufout[2]& 0xff) | (bufout[3]&0xff) << 8);
        outcmd.ind  = (short) ((bufout[4]& 0xff) | (bufout[5]&0xff) << 8);
        if((outcmd.cmd0 &0x8000) == 0x8000)
        {//   if(verboze)
            //  { printf("\a\nКоманда %d (0x%x) неизвестна контроллеру\n", pm2->cmd, pm2->cmd);
            //  }
            return 6;
        }
        sl = (short) ((bufout[shiftLansw]& 0xff) | (bufout[shiftLansw+1]&0xff) << 8);
        l = sl * size0 +  shiftLansw + 2;
        if(nb < l)
        {
            System.out.printf("Error: сервер sent %d, should send %d\n",nb, l);
            return 8;
        }
        if(lenout < nb )
        {
            System.out.printf("Error: сервер sent %d, lenout = %d\n",nb, lenout);
            return 9;
        }
        System.arraycopy( bufout, shiftLansw, outcmd.Buf, 0, sl * size0+2);
        System.out.println("сервер sent me " +  nb + "bytes");
        Last_work = new Date();
        work = true;

        return 0;

    }
    public static int mem_cmp(byte b1[], byte b2[], int sz){
        for(int i = 0; i < sz; i++){
            if(b1[i] != b2[i]){
                if((b1[i] >= 0 && b2[i] >= 0)||(b1[i] < 0 && b2[i] < 0))
                    return b1[i] - b2[i];
                if(b1[i] < 0 && b2[i] >= 0)
                    return 1;
                if(b2[i] < 0 && b1[i] >=0)
                    return -1;
            }
        }
        return 0;
    }

}
