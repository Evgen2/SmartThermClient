package com.smartthermclient;

import static android.graphics.PorterDuff.Mode.SRC;
import static java.time.Instant.ofEpochSecond;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;

import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SmartTherm {
    private static final int IDENTIFY_TYPE = 0x0100;
    private static final int IDENTIFY_CODE = 0x0200;
    int versionCode = 0x0000;
    int Vers = 0;
    int SubVers = 0;
    int SubVers1 = 0;
    String IdentifyStr = "";
    String BuildDate = "";
    private static final int MCMD_GETTIME = 0x10; // читать/задать время RTC
    private static final int MCMD_SETTIME = 0x11; // читать/задать время RTC
    private static final int MCMD_SET_TCPSERVER = 0x22; // задать TCP сервер, порт и время обновления информации
    private static final int MCMD_GET_OT_INFO = 0x23; // get Open Therm info (applcation ask controller)
    private static final int MCMD_SET_OT_DATA = 0x24; // set controller & OpenTherm data

    private static final int MCMD_SET_OT_DEBUG = 0x30; // set Open Therm debug 0/1
    private static final int MCMD_GET_CAP = 0x31; // get capabilities
    private static final int ACMD_ASK_STS_S = 0x50; // applшcation ask is controller known  (applcation ask server)
    private static final int ACMD_GET_STS_S = 0x51; // applcation get controller sts  (applcation ask server)
    private static final int ACMD_SET_STATE_C = 0x52; // applcation set controller state  (applcation send controller)
    private static final int ACMD_SET_STATE_S = 0x53; //  applcation set controller state via server (applcation send server)
    private static final int SCMD_SET_STATE_C = 0x54; // server set controller state (server send controller)


    private static final int MCMD_IDENTIFY = 0x80;
    private static final int MCMD_INTRODUCESELF = 0x81; // applcation send to server selfinfo  (applcation send to server)
    final static String SetupFile = "SmartThermClient_settings";
    public int config_change_event = 0;
    int sts;
    int sts_controller = 0;
    int sts_server = 0;
    int np_server = 0; // число пакетов от сервера после коннекта
    //0 - init, 1 - connecting, 2 - Соединение установлено, 3 - Инициализация обмена,  4- обмен ok,
    // 11,12 - connecting error, 19-26 ошибки обмена, 30 - пауза
    // 0x40 сервер не знает MAC контроллера
//    int IknowMycontroller = 0;
    int NeedSendControllerCmd = 1;
    int NeedSendControllerCmd2 = 0;
    int NeedSendServerCmd = 1;
    int NeedSendServerCmd2 = 0;
    /***********************************************/
    static int needSavesetup;
    static int needRestartSetup;

    int LocalHomeNetsts =-1; //-1 - неизвестно, 0 Не локальная или не домашняя сеть 1 - локальная домашняя сеть -
    String ServerIpAddress;
    String Server_default_IpAddress;
    String ServerUserName;
    String ServerUserPwd;
    int ServerTimeout;
    int ControllerTimeout;
    int ControllerPort;
    int ServerPort;
    int ServerReportTime = 15;
    int ControllerReportTime = 5;
    int server_user_online_report_period = 10;
    TCPconnection controller_server;
    TCPconnection remote_server;
    Instant time_of_controller_connect;
    Instant time_of_server_connect;
    Instant time_of_last_message;
    int LastMessage_isfrom = -1; //-1 none 0 - controller, 1 - remote server
//    boolean UseDebugLog;
//    String DebugLogFname;
    /************ Threads & Connect & Sleep/Resume ******************/
    static ControllerThread user_ConrollerThread;
    static int ControllerThreadRun = 0;
    boolean ControllerThreadRunNeedExit = false;
    static RemoteServerThread user_ServerThread;
    static int RemoteServerThreadRun = 0;
    boolean RemoteServerThreadRunNeedExit = false;
    private volatile boolean exit = false;
    public int need_update_event = 0; // обновить таблицу
    public int need_update_connect_info_event = 0; // обновить информатор о связи
    public int ActivityesFlag; // флаги активностей. Каждая при onResume() устанавливет флаг, в OnStop сбрасывает
    boolean Theads_Sleep = false;
    /*****************************************************************/
    final String className = "SmartTherm";
    String infomsg;
    Date Last_server_ST_work;//время последнего принятого пакета сервером от контроллера (см также time_of_server_connect)
    Date server_start_work;  //время запуска сервера


    /************************/
    boolean TsetChanged; // Tset контроллера изменился
    boolean NeedSetControllerData; // Нужно передать данные контроллеру

    SmartUtils sutils = new SmartUtils();
    static public SmartBoiler myboiler = new SmartBoiler();
    static final int MaxBoilers = 4;
    static public SmartBoiler[] boiler = new SmartBoiler [MaxBoilers];
    static public int indCurrentBoiler;
    int Nboilers = 0;

    SmartTherm() {
        sts = 0;
        ServerIpAddress = "default";
        Server_default_IpAddress ="80.237.33.121";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            time_of_controller_connect = time_of_server_connect = time_of_last_message = ofEpochSecond(0);
        }

        for(int i=0; i<MaxBoilers; i++)
            boiler[i] = new SmartBoiler();
        Nboilers = 1;
        boiler[0].Name = "Котёл 1";
        myboiler = boiler[0];
        indCurrentBoiler = 0;

        ServerTimeout = 10000;
        ControllerTimeout = 5000;
        ControllerPort = 6769;
        ServerPort = 8876;
        ServerUserName = "User";
        ServerUserPwd = "pwd";
        needSavesetup = 0;
        needRestartSetup = 0;
        infomsg = null;
        Last_server_ST_work = new Date(0);
        server_start_work = Last_server_ST_work;
        //UseDebugLog = false;
        //DebugLogFname = "smarttherm.log";

        TsetChanged = false;
        NeedSetControllerData = false;

    }
    void AddBoiler()
    {
        if(Nboilers < MaxBoilers)
        {   String str;
            str = String.format("Котёл %d", Nboilers+1 );
            boiler[Nboilers].Name = str;
            str = String.format("127.0.0.%d", Nboilers+1 );
            boiler[Nboilers].ControllerIpAddress = str;

            Nboilers++;
        }

    }
    void DelBoiler(int ind)
    {   if(Nboilers < 2)
            return;
        if(ind == Nboilers -1)
        {   Nboilers--;
            if(indCurrentBoiler >= Nboilers)
                indCurrentBoiler = Nboilers-1;
        } else {
            for (int i= ind; i < Nboilers-1; i++)
                boiler[i] =  boiler[i+1];
            Nboilers--;
            if(indCurrentBoiler == ind)
                indCurrentBoiler = ind;
            else if(indCurrentBoiler > ind)
                indCurrentBoiler = ind -1;
        }
        myboiler = boiler[indCurrentBoiler];
    }

    int read(FileInputStream fin) {
        int rc, nb = 0;
        String varname;
        String value;
        System.out.print("SmartTherm read\n");
        BufferedReader reader = null;

        try {
            InputStreamReader in = new InputStreamReader(fin);
            reader = new BufferedReader(in);
            String line = reader.readLine();
            while (line != null) {
                System.out.printf("Read %s \n", line);
                String[] pars = line.split("=");
                if (pars.length == 2) {
                    varname = pars[0];
                    value = pars[1];
                    ParsePar(varname, value);
                }
//                Log.d("jopa", line);
                // read next line
                line = reader.readLine();
            }

            reader.close();
//            rc = thm.Read(fin);
        } catch (FileNotFoundException e) {
            System.out.printf("file %s not found\n", SetupFile);
            e.printStackTrace();
            return 1;
        } catch (IOException e) {
            System.out.print("io error\n");
            e.printStackTrace();
            return 2;
        }
        config_change_event = 1;
        return 0;
    }

    int ParsePar(String name, String value) {
        int known = 0, v;
        if (name.equals("ControllerMac")) {
            String[] addrs = value.split(":");
            {
                if (addrs.length == myboiler.MacAddr.length) {
                    for (int i = 0; i < addrs.length; i++) {
                        myboiler.MacAddr[i] = Integer.decode("0x" + addrs[i]).byteValue();
                    }
                    known = 1;
                }
            }
        } else if (name.equals("ControllerMac0")) {
            String[] addrs = value.split(":");
            if (addrs.length == myboiler.MacAddr.length) {
                for (int i = 0; i < addrs.length; i++) {
                    boiler[0].MacAddr[i] = Integer.decode("0x" + addrs[i]).byteValue();
                }
            }
        } else if (name.equals("ControllerMac1")) {
            String[] addrs = value.split(":");
            if (addrs.length == myboiler.MacAddr.length) {
                for (int i = 0; i < addrs.length; i++) {
                    boiler[1].MacAddr[i] = Integer.decode("0x" + addrs[i]).byteValue();
                }
            }
        } else if (name.equals("ControllerMac2")) {
            String[] addrs = value.split(":");
            if (addrs.length == myboiler.MacAddr.length) {
                for (int i = 0; i < addrs.length; i++) {
                    boiler[2].MacAddr[i] = Integer.decode("0x" + addrs[i]).byteValue();
                }
            }
        } else if (name.equals("ControllerMac3")) {
            String[] addrs = value.split(":");
            if (addrs.length == myboiler.MacAddr.length) {
                for (int i = 0; i < addrs.length; i++) {
                    boiler[3].MacAddr[i] = Integer.decode("0x" + addrs[i]).byteValue();
                }
            }
        } else if (name.equals("Nboilers")) {
            Nboilers = Integer.parseInt(value);
        } else if (name.equals("indCurrentBoiler")) {
            indCurrentBoiler = Integer.parseInt(value);
            if(indCurrentBoiler < 0) indCurrentBoiler = 0;
            else if (indCurrentBoiler >= MaxBoilers)
                indCurrentBoiler = MaxBoilers -1;
            myboiler = boiler[indCurrentBoiler];

        } else if (name.equals("IknowMycontroller0")) {
            boiler[0].IknowMycontroller = Integer.parseInt(value);
        } else if (name.equals("IknowMycontroller1")) {
            boiler[1].IknowMycontroller = Integer.parseInt(value);
        } else if (name.equals("IknowMycontroller2")) {
            boiler[2].IknowMycontroller = Integer.parseInt(value);
        } else if (name.equals("IknowMycontroller3")) {
            boiler[3].IknowMycontroller = Integer.parseInt(value);
        } else if (name.equals("IknowMycontroller")) {
            myboiler.IknowMycontroller = Integer.parseInt(value);
            known = 0;
        } else if (name.equals("ControllerIpAddress0")) {
            boiler[0].ControllerIpAddress = value;
        } else if (name.equals("ControllerIpAddress1")) {
            boiler[1].ControllerIpAddress = value;
        } else if (name.equals("ControllerIpAddress2")) {
            boiler[2].ControllerIpAddress = value;
        } else if (name.equals("ControllerIpAddress3")) {
            boiler[3].ControllerIpAddress = value;
        } else if (name.equals("ControllerIpAddress")) {
            myboiler.ControllerIpAddress = value;
            known = 2;
        } else if (name.equals("ControllerName0")) {
            boiler[0].Name = value;
        } else if (name.equals("ControllerName1")) {
            boiler[1].Name = value;
        } else if (name.equals("ControllerName2")) {
            boiler[2].Name = value;
        } else if (name.equals("ControllerName3")) {
            boiler[3].Name = value;
        } else if (name.equals("ControllerName")) {
            myboiler.Name = value;
        } else if (name.equals("Use_remoteTCPserver0")) {
            v = Integer.parseInt(value);
            if( v == 1)   boiler[0].Use_remoteTCPserver = true;
            else          boiler[0].Use_remoteTCPserver = false;
        } else if (name.equals("Use_remoteTCPserver1")) {
            v = Integer.parseInt(value);
            if( v == 1)   boiler[1].Use_remoteTCPserver = true;
            else          boiler[1].Use_remoteTCPserver = false;
        } else if (name.equals("Use_remoteTCPserver2")) {
            v = Integer.parseInt(value);
            if( v == 1)   boiler[2].Use_remoteTCPserver = true;
            else          boiler[2].Use_remoteTCPserver = false;
        } else if (name.equals("Use_remoteTCPserver3")) {
            v = Integer.parseInt(value);
            if( v == 1)   boiler[3].Use_remoteTCPserver = true;
            else          boiler[3].Use_remoteTCPserver = false;
        } else if (name.equals("Use_remoteTCPserver")) {
            v = Integer.parseInt(value);
            if( v == 1)   myboiler.Use_remoteTCPserver = true;
            else          myboiler.Use_remoteTCPserver = false;
        } else if (name.equals("ControllerPort")) {
            ControllerPort = Integer.parseInt(value);
            known = 3;
        } else if (name.equals("ServerIpAddress")) {
            ServerIpAddress = value;
            known = 4;
        } else if (name.equals("ServerPort")) {
            ServerPort = Integer.parseInt(value);
            known = 5;
//        } else if (name.equals("controllerUseDebugLog")) {
//            UseDebugLog = Boolean.valueOf(value);
//            known = 6;
//        } else if (name.equals("controllerDebugLogFile")) {
//            DebugLogFname = value;
//            known = 7;
        }
        return known;
    }

    int Write(FileOutputStream fout) throws IOException {
        String str;
        int i, v;


        str = String.format(Locale.ROOT, "Nboilers=%d\n", Nboilers);
        fout.write(str.getBytes());
        str = String.format(Locale.ROOT, "indCurrentBoiler=%d\n", indCurrentBoiler);
        fout.write(str.getBytes());
        if(Nboilers > 1)
        {   for(i=0; i<Nboilers;i++)
            {
                str = String.format(Locale.ROOT, "IknowMycontroller%d=%d\n", i, boiler[i].IknowMycontroller);
                fout.write(str.getBytes());
                str = String.format(Locale.ROOT, "ControllerMac%d=%02x:%02x:%02x:%02x:%02x:%02x\n",i, boiler[i].MacAddr[0], boiler[i].MacAddr[1], boiler[i].MacAddr[2], boiler[i].MacAddr[3], boiler[i].MacAddr[4], boiler[i].MacAddr[5]);
                fout.write(str.getBytes());
                str = String.format(Locale.ROOT, "ControllerIpAddress%d=%s\n", i, boiler[i].ControllerIpAddress);
                fout.write(str.getBytes());
                str = String.format(Locale.ROOT, "ControllerName%d=%s\n", i, boiler[i].Name);
                fout.write(str.getBytes());
                v = 0;
                if(boiler[i].Use_remoteTCPserver) v = 1;
                str = String.format(Locale.ROOT, "Use_remoteTCPserver%d=%d\n",i, v);
                fout.write(str.getBytes());
            }
        }
        str = String.format(Locale.ROOT, "IknowMycontroller=%d\n", myboiler.IknowMycontroller);
        fout.write(str.getBytes());
//        str = String.format(Locale.ROOT, "%s.ControllerMac=%02x:%02x:%02x:%02x:%02x:%02x\n", className, MacAddr[0],  MacAddr[1],MacAddr[2],MacAddr[3],MacAddr[4],MacAddr[5]);
        str = String.format(Locale.ROOT, "ControllerMac=%02x:%02x:%02x:%02x:%02x:%02x\n", myboiler.MacAddr[0], myboiler.MacAddr[1], myboiler.MacAddr[2], myboiler.MacAddr[3], myboiler.MacAddr[4], myboiler.MacAddr[5]);
        fout.write(str.getBytes());
        str = String.format(Locale.ROOT, "ControllerIpAddress=%s\n", myboiler.ControllerIpAddress);
        fout.write(str.getBytes());
        str = String.format(Locale.ROOT, "ControllerName=%s\n",  myboiler.Name);
        fout.write(str.getBytes());

        v = 0;
        if(myboiler.Use_remoteTCPserver) v = 1;
        str = String.format(Locale.ROOT, "Use_remoteTCPserver=%d\n", v);
        fout.write(str.getBytes());

        str = String.format(Locale.ROOT, "ControllerPort=%d\n", ControllerPort);
        fout.write(str.getBytes());
        str = String.format(Locale.ROOT, "ServerIpAddress=%s\n", ServerIpAddress);
        fout.write(str.getBytes());
        str = String.format(Locale.ROOT, "ServerPort=%d\n", ServerPort);
        fout.write(str.getBytes());
//        str = String.format(Locale.ROOT, "controllerUseDebugLog=%b\n", UseDebugLog);
//        fout.write(str.getBytes());
//        str = String.format(Locale.ROOT, "controllerDebugLogFile=%s\n", DebugLogFname);
//        fout.write(str.getBytes());
        needSavesetup = 0;
        return 0;
    }

    // периодический опрос контроллера
    int loop_net() {
        int rc;
        if(NeedSendControllerCmd2 != 0)
        {   NeedSendControllerCmd = NeedSendControllerCmd2;
            NeedSendControllerCmd2 = 0;
        }
        switch (NeedSendControllerCmd) {
            case 1:
                rc = GetContollerState();
                break;
            case 4:
                rc = GetTime(0);
                NeedSendControllerCmd = 5;
                break;
            case 5:
                rc = GetTime(0);
                NeedSendControllerCmd = 0;
                break;
            case 6:
                rc = SetContollerState(0);
                NeedSendControllerCmd = 1;
                break;
            default:
                rc = GetContollerState();
        }

        if (rc == 0)
            LastMessage_isfrom = 0;
        else if (rc < 0) {
            try {
                if (controller_server.socket != null) {
                    controller_server.socket.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return rc;
    }

    int loop_server_net() {
        int rc = 0;
        switch (NeedSendServerCmd) {
            case 1:
                rc = GetContollerState_from_remote_server();
                if (rc == 0)
                    np_server++;

                break;

            case 6:
                rc = SetContollerState(1);
                NeedSendServerCmd = 1;
                break;
        }

        if (rc == 0)
            LastMessage_isfrom = 1;

        return rc;
    }

    int Init()  {
        int i, is;
        System.out.print("SmartTherm Init\n");
        /* если запущены нитки связи контроллером или ужаленным сервером выходим из них */
        if(ControllerThreadRun != 0 || RemoteServerThreadRun != 0) {
            ControllerThreadRunNeedExit = true;
            RemoteServerThreadRunNeedExit = true;
            do {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            } while (ControllerThreadRun != 0 || RemoteServerThreadRun != 0);
        }
        myboiler = boiler[indCurrentBoiler];
        CheckLocalHomeNet();
        is = 0;
        for(i=0; i<6; i++) {
            if (myboiler.MacAddr[i] != 0) {
                is = 1;
                break;
            }
        }
        if(is == 0) myboiler.IknowMycontroller = 0;

        if(LocalHomeNetsts == 1 || myboiler.IknowMycontroller == 0 || !myboiler.Use_remoteTCPserver)
        {   sts_controller = 1;
            sts_server = 0;
        } else {
            sts_controller = 0;
            sts_server =  1;
        }

        return 0;
    }

    int ControllerConnection() {
        if (ControllerThreadRun == 0) {
            ControllerThreadRun = 1;
            ControllerThreadRunNeedExit = false;
            user_ConrollerThread = new ControllerThread();
            Thread threadInfo = new Thread(user_ConrollerThread, "T1");
            threadInfo.start();
            System.out.println("user_Thread.exit=" + exit);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (RemoteServerThreadRun == 0) {
            RemoteServerThreadRun = 1;
            RemoteServerThreadRunNeedExit = false;
            user_ServerThread = new RemoteServerThread();
            Thread threadInfo1 = new Thread(user_ServerThread, "T2");
            threadInfo1.start();
            System.out.println("user_Thread.exit=" + exit);
        }

        return 0;
    }

    class ControllerThread implements Runnable {
        public void run() {
            int rc, rc0;
            int np = 0, raz = 0;
            int need_reconnext = 0;
            int sl;
            System.out.println("The UserThread  thread is running");
            ControllerThreadRun = 1;
            controller_server = new TCPconnection();
            while (true) {
                if (ControllerThreadRunNeedExit)
                    break;
                if (sts_controller == 0 || Theads_Sleep) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }
                controller_server.CloseConnection();

                sts = 1; // 1 - connecting
                try {
                    rc = controller_server.createTCPconnection(myboiler.ControllerIpAddress, ControllerPort, 3000);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (rc == 0) {
                    infomsg = "Connecting";
                    raz = 0;
                    need_reconnext = 0;
                    sts_controller = 2;
                    need_update_connect_info_event++;
                    if (sts == 0) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            time_of_controller_connect = Instant.now();
                        }
                    }
                    System.out.printf("createTCPconnection rc  %d\n", rc);
                    rc = controller_server.InitClientConnection();
                    if (rc == 0) {
                        String str;
                        infomsg = "InitClientConnection ok";
                        sts = 3; // 3 - Соединение установлено
                        sts_controller = 3;
                        str = String.format("\n%s type %x code %d", controller_server.serverIdentify, controller_server.servercode, controller_server.servertype);
                        infomsg += str;
                        need_update_connect_info_event++;

/************************/
                        rc = GetContollerCapabilities();
/**===**/
                        if (rc == 0) {
                            if (myboiler.IknowMycontroller == 0) {
                                myboiler.IknowMycontroller = 1;
                                needSavesetup = 1;
                            }
                            rc = GetContollerState();
                            rc = GetTime(0);
                            rc = SetContoller_Tcpserver();

/************************/
                            while (Theads_Sleep != true) {
                                if (ControllerThreadRunNeedExit)
                                    break;
                                rc = loop_net();
                                need_update_connect_info_event++;
                                if (rc != 0) {
//rc = 1 Timeout
//rc = 2 прочие ошибки
//rc = 3 плохой сокет
//rc = 4 длина входного буфера неправильная
//rc = 5 получено не то количество байт, которое должно быть *
//rc = 6 ответ: неизвестная команда *
//rc = -1 CONNABORTED
//rc = -2;
                                    sts = 20 + rc;
                                    need_reconnext = 1;
                                    break;
                                }
                                np++;
                                sts = 4;
//todo
//                            str = String.format("Пакетов %d", np);
//                            info_connect_msg = str;

                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
/**===**/
                        } else {
                            sts = 10;
                        }
                    } else {
                        if (rc == 1)
                            sts_controller = 0x41; // Connection failed
                        else if (rc == 2)
                            sts_controller = 0x42; // Connection ok, response wrong
                        else if (rc == 3)
                            sts_controller = 0x43; // Connection ok, handshake wrong
                        need_update_connect_info_event++;

                        sts = 10;
                    }

                } else {
                    sts = 10 + rc; // 11/12 - createTCPconnection rc = 1/
                    sts_controller = 10;
                    raz++;
//                    System.out.printf("end run rc=%d\n", rc);
//                    System.out.println("The user thread is now stopped");

                    sl = 2000 + raz * 100;
                    if (raz == 2 || raz > 10) {
                        if(myboiler.IknowMycontroller != 0 && myboiler.Use_remoteTCPserver) {
                            controller_server.CloseConnection();
                            sts_controller = 0;
                            sts_server = 1;
                        }
                        if (raz > 100)
                            raz = 100;
                    }

                    try {
                        Thread.sleep(sl);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (sts_controller > 0)
                        sts_controller = 1;

                }
                if (ControllerThreadRunNeedExit)
                    break;
                if (need_reconnext > 0 || Theads_Sleep)
                    controller_server.CloseConnection();
                sl = 5000;

                System.out.print("Sleep2 5000\n");
                if (sts_controller > 0 && sts_controller < 0X40)
                    sts_controller = 10;

                try {
                    Thread.sleep(sl);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (sts_controller > 0) sts_controller = 1;

            }

            controller_server.CloseConnection();
            ControllerThreadRun = 0;
        }
    }

    class RemoteServerThread implements Runnable {
        public void run() {
            int rc, rc0;
            int np = 0, np1 = 0, raz = 0;
            int need_reconnext = 0;
            int sl;
            String str;
            String ServerAddress;
            RemoteServerThreadRun = 1;

//          System.out.println("The RemoteServerThread   thread is running");
            remote_server = new TCPconnection();
            while (true) {
                if (RemoteServerThreadRunNeedExit)
                    break;
                if (sts_server == 0 || Theads_Sleep) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }
                if (myboiler.IknowMycontroller == 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }
                if(ServerIpAddress.equals("default"))
                    ServerAddress = Server_default_IpAddress;
                else
                    ServerAddress = ServerIpAddress;

                try {
                    rc0 = remote_server.createTCPconnection(ServerAddress, ServerPort, 6000);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (rc0 == 0) {
                    infomsg = "Connected";
                    System.out.printf("remote_server connected\n");

                    raz = 0;
                    need_reconnext = 0;
                    sts_server = 2;
                    np_server = 0;
                    need_update_connect_info_event++;
                    System.out.printf("createTCPconnection rc0  %d\n", rc0);
                    rc = remote_server.InitClientConnection();
                    if (rc == 0) {
                        np_server++;
                        need_update_connect_info_event++;
                        if (myboiler.have_remote_server_ClientId) {
                            rc = loop_server_net();
                            if (rc != 0)
                                myboiler.have_remote_server_ClientId = false;
                            else {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            need_update_connect_info_event++;
                        }
                        if (!myboiler.have_remote_server_ClientId) {
                            System.out.printf("remote_server InitClientConnection\n");
                            rc = Ask_RemoteServer_MyContoller();
                            System.out.printf("Ask_RemoteServer_MyContoller rc=%d\n", rc);
                            need_update_connect_info_event++;
                        }
                        if (rc == 0x0) {
                            infomsg = "InitServerConnection ok";
                            sts = 3; // 3 - Соединение установлено
                            sts_server = 3;
                            str = String.format("\n%s type %x code %d", remote_server.serverIdentify, remote_server.servercode, remote_server.servertype);
                            infomsg += str;
                            if (!myboiler.have_remote_server_ClientId) {
                                rc = Identify_server();
                                System.out.printf("Identify_server rc=%d\n", rc);
                                need_update_connect_info_event++;
                            }
                            if (rc == 0) {
                                NeedSendServerCmd = 1;
                                np1 = 0;
                                while (Theads_Sleep != true) {
                                    if (RemoteServerThreadRunNeedExit)
                                        break;
                                    rc = loop_server_net();
                                    need_update_connect_info_event++;
                                    System.out.printf("loop_server_net rc=%d %d %d\n", rc, np_server, np1);
                                    if (rc != 0) {
                                        sts = 20 + rc;
                                        need_reconnext = 1;
                                        break;
                                    }
                                    np++;
                                    np1++;
                                    sts = 4;
                                    sl = server_user_online_report_period;
                                    if (sl < 2) sl = 2;
                                    else if (sl > 15) sl = 15;
                                    try {
                                        Thread.sleep(sl * 1000);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            } else {
                                sts_server = 13;
                            }
                        } else if (rc == 0x10) //сервер не знает MAC контроллера
                            sts_server = 0x40;

/************************/
                    } else {
                        if (rc == 1)
                            sts_server = 0x41; // Connection failed
                        else if (rc == 2)
                            sts_server = 0x42; // Connection ok, response wrong
                        else if (rc == 3)
                            sts_server = 0x43; // Connection ok, handshake wrong
                    }
                    if (sts == 0) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            time_of_server_connect = Instant.now();
                        }
                    }
                    System.out.printf("create server TCPconnection rc  %d\n", rc);
                    //todo
                } else {
                    sts = 10 + rc0; // 11/12 - createTCPconnection rc = 1/
                    if (rc0 == 1)
                        sts_server = 11; // host unknown
                    else if (rc0 == 3)
                        sts_server = 0x44; // Failed  to connect"
                    else
                        sts_server = 10;
                    raz++;
                    need_update_connect_info_event++;

//                  sl = 2000;
//todo test
                    sl = 4000 + raz * 100;

                    if (raz == 2 || raz > 10) {
                        sts_controller = 1;
                        sts_server = 0;
                        remote_server.CloseConnection();
                        if (raz > 100)
                            raz = 100;
                    }


                    try {
                        Thread.sleep(sl);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (sts_server > 0)
                        sts_server = 1;
                }

                if (RemoteServerThreadRunNeedExit)
                    break;

                sl = 5000;
//              System.out.print("Sleep2 S 5000\n");
                if (sts_server > 0 && sts_server < 0x40)
                    sts_server = 10;

                need_update_connect_info_event++;
                try {
                    Thread.sleep(sl);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (sts_server > 0) sts_server = 1;

                remote_server.CloseConnection();
            }
            RemoteServerThreadRun = 0;
        }
    }

    //MCMD_SET_OT_DATA
//ACMD_SET_STATE_C   mode 0
//ACMD_SET_STATE_S   mode 1
    int SetContollerState(int mode) {
        int rc, dst;
        byte tmpmac[] = new byte[6];
        short b_flags;

        ByteBuffer bb = ByteBuffer.allocate(6);

        Msg1 ucmd = new Msg1();
        Msg1 outcmd = new Msg1();
        dst = 0;

        if(mode == 0)
            ucmd.cmd = ACMD_SET_STATE_C;
        else {
            ucmd.cmd = ACMD_SET_STATE_S;

            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putInt(myboiler.remote_server_ClientId);
            System.arraycopy(bb.array(), 0, ucmd.Buf, dst, 4);
            dst += 4;
        }

        b_flags = 0;
        if (myboiler.enable_CentralHeating_toSet)
            b_flags |= 0x01;
        if (myboiler.enable_HotWater_toSet)
            b_flags |= 0x02;


        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort(b_flags);
        System.arraycopy(bb.array(), 0, ucmd.Buf, dst, 2);
        bb.clear();
        dst += 2;

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putFloat(myboiler.Tset_toSet);
        System.arraycopy(bb.array(), 0, ucmd.Buf, dst, 4); //Tset
        bb.clear();
        dst += 4;

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putFloat(myboiler.TroomTarget_toSet);
        System.arraycopy(bb.array(), 0, ucmd.Buf, dst, 4); //roomSetpointT
        bb.clear();
        dst += 4;

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putFloat(myboiler.TdhwSet_toSet);
        System.arraycopy(bb.array(), 0, ucmd.Buf, dst, 4); //TdhwSet
        bb.clear();
        dst += 4;

        if(mode == 0)
            rc = controller_server.SendAndConfirm(ucmd, dst, outcmd, 0); // ACMD_SET_STATE_C dst=14
        else
            rc = remote_server.SendAndConfirm(ucmd, dst, outcmd, 0);    // ACMD_SET_STATE_S dst = 18

        return rc;
    }

    //MCMD_GET_OT_INFO
    int GetContollerState() {
        int rc, itmp;
        byte tmp[] = new byte[4];
        byte tmpmac[] = new byte[6];
        short b_flags, itmp2;

        ByteBuffer bb = ByteBuffer.allocate(6);
        Msg1 ucmd = new Msg1();
        Msg1 outcmd = new Msg1();
        ucmd.cmd = MCMD_GET_OT_INFO;
        //formal TCPserver_report_period
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(0);
        System.arraycopy(bb.array(), 0, ucmd.Buf, 0, 4);
        bb.clear();
        //formal  TCPserver_close_on_send
        itmp2 = 0;
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort(itmp2);
        System.arraycopy(bb.array(), 0, ucmd.Buf, 4, 2);

        rc = controller_server.SendAndConfirm(ucmd, 6, outcmd, 72);
        if (rc == 0) {
            FillSmartStsFromMsg(outcmd, 0);
            System.out.printf("BoilerStatus = %x ", myboiler.BoilerStatus);
            System.out.printf("BoilerT = %f ", myboiler.BoilerT);
            System.out.printf("RetT = %f ", myboiler.RetT);
            System.out.printf("Dhw_T = %f\n", myboiler.Dhw_T);
            need_update_event++;
        } else {
            System.out.printf("SendAndConfirm rc = %x\n", rc);
        }
//todo
        return rc;
    }

    //ACMD_GET_STS_S
//аналогично MCMD_GET_OT_INFO плюс информация о времени последнего сеанса связи
    int GetContollerState_from_remote_server() {
        int rc, itmp;
        byte tmp[] = new byte[4];
        byte tmpmac[] = new byte[6];
        short b_lags, itmp2;
        ByteBuffer bb = ByteBuffer.allocate(4);
//        ByteBuffer bb = ByteBuffer.allocate(6);
        Msg1 ucmd = new Msg1();
        Msg1 outcmd = new Msg1();
        ucmd.cmd = ACMD_GET_STS_S;

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(myboiler.remote_server_ClientId);
        System.arraycopy(bb.array(), 0, ucmd.Buf, 0, 4);

        rc = remote_server.SendAndConfirm(ucmd, 4, outcmd, 80);
        if (rc == 0) {
            FillSmartStsFromMsg(outcmd, 1); // 80
            need_update_event++;
        }
        return rc;
    }

    //разбор ответа на команды
    //MCMD_GET_OT_INFO mode 0, 72 bytes
    //ACMD_GET_STS_S mode 1, 80 bytes
    void FillSmartStsFromMsg(Msg1 outcmd, int mode) //mode =1 - ACMD_GET_STS_S else MCMD_GET_OT_INFO
    {
        int itmp;
        byte tmp[] = new byte[4];
        short b_flags, itmp2;
        float x;
        System.arraycopy(outcmd.Buf, 0, tmp, 0, 2);
        b_flags = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
        myboiler.CapabilitiesFlags = b_flags & ~(0x03|0x200);
        if ((b_flags & 0x01) != 0) myboiler.enable_CentralHeating = true;
        else myboiler.enable_CentralHeating = false;
        if ((b_flags & 0x02) != 0) myboiler.enable_HotWater = true;
        else myboiler.enable_HotWater = false;
        if ((b_flags & 0x10) != 0) myboiler.HotWater_present = true;
        else myboiler.HotWater_present = false;
        if ((b_flags & 0x20) != 0) myboiler.CH2_present = true;
        else myboiler.CH2_present = false;
        if ((b_flags & 0x40) != 0) myboiler.Toutside_present = true;
        else myboiler.Toutside_present = false;
        if ((b_flags & 0x80) != 0) myboiler.Pressure_present = true;
        else myboiler.Pressure_present = false;

        if ((b_flags & 0x100) != 0) myboiler.MQTT_present = true;
        else myboiler.MQTT_present = false;
        if ((b_flags & 0x200) != 0) myboiler.MQTT_used = true;
        else myboiler.MQTT_used = false;
        if ((b_flags & 0x400) != 0) myboiler.PID_present = true;
        else myboiler.PID_present = false;
        if ((b_flags & 0x800) != 0) myboiler.PID_used = true;
        else myboiler.PID_used = false;

        System.arraycopy(outcmd.Buf, 2, tmp, 0, 2);
        itmp2 = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
        myboiler.stsOT = itmp2;
        long imp1;
        System.arraycopy(outcmd.Buf, 4, tmp, 0, 4);
        imp1 = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
        if(imp1 == 0)
        {
            System.out.println("imp1 == 0\n");

        }
        myboiler.Last_OT_work = new Timestamp(imp1 * 1000);
        System.out.printf("Last_OT_work =%s\n", myboiler.Last_OT_work);
        System.arraycopy(outcmd.Buf, 8, tmp, 0, 4);
        itmp = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
        myboiler.BoilerStatus = itmp;

        System.arraycopy(outcmd.Buf, 12, tmp, 0, 4);
        x = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
        myboiler.BoilerT = x;
        System.arraycopy(outcmd.Buf, 16, tmp, 0, 4);
        x = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
        myboiler.RetT = x;

        System.arraycopy(outcmd.Buf, 20, tmp, 0, 4);
        x = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
        if (x != myboiler.Tset) {
            myboiler.Tset = x;
            TsetChanged = true; //??
        }
        System.arraycopy(outcmd.Buf, 24, tmp, 0, 4);
        x = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
        myboiler.Tset_r = x;

        System.arraycopy(outcmd.Buf, 28, tmp, 0, 4);
        x = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
        myboiler.Dhw_T = x;

        System.arraycopy(outcmd.Buf, 32, tmp, 0, 4);
        x = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
        myboiler.TdhwSet = x;

        System.arraycopy(outcmd.Buf, 36, tmp, 0, 4);
        x = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
        myboiler.FlameModulation = x;

        System.arraycopy(outcmd.Buf, 40, tmp, 0, 4);
        x = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
        myboiler.Pressure = x;

        System.arraycopy(outcmd.Buf, 44, tmp, 0, 4);
        itmp = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
        myboiler.status_D18b20 = itmp;

        System.arraycopy(outcmd.Buf, 48, tmp, 0, 4);
        x = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
        myboiler.t1 = x;

        System.arraycopy(outcmd.Buf, 52, tmp, 0, 4);
        x = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
        myboiler.t2 = x;

        System.arraycopy(outcmd.Buf, 56, tmp, 0, 4);
        x = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
        myboiler.Toutside = x;

        System.arraycopy(outcmd.Buf, 60, tmp, 0, 4);
        x = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
        myboiler.TempIndoor = x;
        System.arraycopy(outcmd.Buf, 64, tmp, 0, 4);
        x = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
        myboiler.TempOutdoor = x;

        System.arraycopy(outcmd.Buf, 68, tmp, 0, 4);
        x = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
        myboiler.TroomTarget = x;
        if (mode == 1) {
            System.arraycopy(outcmd.Buf, 72, tmp, 0, 4);
            imp1 = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            Last_server_ST_work = new Timestamp(imp1 * 1000);
            System.out.printf("Last_server_ST_work =%s\n", Last_server_ST_work);
            System.arraycopy(outcmd.Buf, 76, tmp, 0, 4);
            imp1 = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            server_start_work = new Timestamp(imp1 * 1000);
        }

        if (myboiler.ToSet_start == 1 && myboiler.stsOT != -2) {
            myboiler.enable_CentralHeating_toSet = myboiler.enable_CentralHeating;
            myboiler.enable_HotWater_toSet = myboiler.enable_HotWater;
            myboiler.Tset_toSet = myboiler.Tset;
            myboiler.TdhwSet_toSet =  myboiler.TdhwSet;
            myboiler.TroomTarget_toSet = myboiler.TroomTarget;
            myboiler.ToSet_start = 0;
        }

    }

    //MCMD_GET_CAP
    int GetContollerCapabilities() {
        int rc, itmp, b_flags4;
        byte tmp[] = new byte[4];
        byte tmp2[] = new byte[2];
        byte tmpmac[] = new byte[6];
        short b_flags, itmp2;

        ByteBuffer bb = ByteBuffer.allocate(6);
        Msg1 ucmd = new Msg1();
        Msg1 outcmd = new Msg1();
        ucmd.cmd = MCMD_GET_CAP;

        rc = controller_server.SendAndConfirm(ucmd, 0, outcmd, 16);
        if (rc == 0) {
            float x;
            System.arraycopy(outcmd.Buf, 0, tmpmac, 0, 6);
            System.arraycopy(tmpmac, 0, myboiler.MacAddr, 0, 6);
            System.arraycopy(outcmd.Buf, 6, tmp2, 0, 2);
            b_flags = ByteBuffer.wrap(tmp2).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();

            System.arraycopy(outcmd.Buf, 8, tmp2, 0, 2);
            itmp2 = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            myboiler.stsOT = itmp2;
            System.arraycopy(outcmd.Buf, 10, tmp, 0, 4);
            b_flags4 = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();

            if (b_flags < 1)
                return 1;

            if ((b_flags & 0x01) != 0) myboiler.enable_CentralHeating = true;
            myboiler.CH2_present = false;
            myboiler.Toutside_present = false;
            myboiler.Pressure_present = false;
            myboiler.RetT_present = false;

            System.arraycopy(outcmd.Buf, 10, tmp, 0, 4);

            if ((b_flags4 & 0x01) != 0) myboiler.HotWater_present = true;
            else myboiler.HotWater_present = false;
            if ((b_flags4 & 0x02) != 0) myboiler.CH2_present = true;
            else myboiler.CH2_present = false;
            if ((b_flags4 & 0x04) != 0) myboiler.RetT_present = true;
            else myboiler.RetT_present = false;
            if ((b_flags4 & 0x08) != 0) myboiler.Toutside_present = true;
            else myboiler.Toutside_present = false;
            if ((b_flags4 & 0x10) != 0) myboiler.Pressure_present = true;
            else myboiler.Pressure_present = false;

            if ((b_flags4 & 0x100) != 0) myboiler.MQTT_present = true;
            else myboiler.MQTT_present = false;
            if ((b_flags4 & 0x200) != 0) myboiler.MQTT_used = true;
            else myboiler.MQTT_used = false;
            if ((b_flags4 & 0x400) != 0) myboiler.PID_present = true;
            else myboiler.PID_present = false;
            if ((b_flags4 & 0x800) != 0) myboiler.PID_used = true;
            else myboiler.PID_used = false;

            System.arraycopy(outcmd.Buf, 14, tmp2, 0, 2);
            b_flags = ByteBuffer.wrap(tmp2).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            boolean tmps;
            if(b_flags != 0)
                tmps = true;
            else
                tmps = false;
            if(myboiler.Use_remoteTCPserver != tmps) {
                myboiler.Use_remoteTCPserver = tmps;
                needSavesetup = 1;
            }
        } else {
            System.out.printf("SendAndConfirm rc = %x\n", rc);
        }
//todo
        return rc;
    }

    //MCMD_SET_TCPSERVER
    int SetContoller_Tcpserver() {
        int l, rc, use;
        short b_flags, itmp2;
        ByteBuffer bb = ByteBuffer.allocate(20);
        Msg1 ucmd = new Msg1();
        Msg1 outcmd = new Msg1();
        ucmd.cmd = MCMD_SET_TCPSERVER;
        String ServerAddress;

        if(ServerIpAddress.equals("default"))
            ServerAddress = Server_default_IpAddress;
        else
            ServerAddress = ServerIpAddress;

        use = 1;
        try {
            InetAddress ipAddress = InetAddress.getByName(ServerAddress); // создаем объект который отображает вышеописанный IP-адрес.
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace();
//            errmsg = uhe.toString();
//            ierr = 1;
//            return ierr;
            use = 0;
        }


//статус 4 int
//IP 20 char
//report time 4 int
//port 4 int

        //formal TCPserver_report_period
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(use);
        System.arraycopy(bb.array(), 0, ucmd.Buf, 0, 4);
        bb.clear();
        byte[] b = ServerAddress.getBytes();
        l = b.length;
        if (l > 20) l = 20;
        System.arraycopy(b, 0, ucmd.Buf, 4, l);

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(ServerReportTime * 1000);
        System.arraycopy(bb.array(), 0, ucmd.Buf, 24, 4);
        bb.clear();

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(ServerPort);
        System.arraycopy(bb.array(), 0, ucmd.Buf, 28, 4);
        bb.clear();

        rc = controller_server.SendAndConfirm(ucmd, 32, outcmd, 0);
        if (rc == 0) {
            float x;
        }
        return rc;
    }

    //MCMD_GETTIME
    //return 0 Ok
    int GetTime(int sts) {
        int rc, itmp;
        byte tmp[] = new byte[4];
        byte tmp_t[] = new byte[8];
        short t_l;
        long t_sec;
        long imp1;
        int usec;
        Msg1 ucmd = new Msg1();
        Msg1 outcmd = new Msg1();
        ucmd.cmd = MCMD_GETTIME;

        rc = controller_server.SendAndConfirm(ucmd, 0, outcmd, 14);
        if (rc == 0) {
            System.arraycopy(outcmd.Buf, 0, tmp, 0, 2);
            t_l = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
            if (t_l == 4) //esp32
            {
                TimeZone tz;
                long diffInMillies;

                System.arraycopy(outcmd.Buf, 2, tmp, 0, 4);
                imp1 = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
                System.arraycopy(outcmd.Buf, 6, tmp, 0, 4);
                usec = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();

                Date date = new Timestamp(imp1 * 1000 + usec/1000);
                Date dateNow = new Date();
                System.out.println(date);
                tz = TimeZone.getDefault();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                df.setTimeZone(tz);
                System.out.println("Date and time " + df.format(date));
                diffInMillies  = dateNow.getTime() - date.getTime();
                System.out.printf("diffInMillies = %d\n", diffInMillies );
                if(diffInMillies < -50 ||diffInMillies > 50) {
                    SetTime(0);
                }

            } else if (t_l == 8) { //esp8266
                System.arraycopy(outcmd.Buf, 2, tmp_t, 0, 8);
                t_sec = ByteBuffer.wrap(tmp_t).order(java.nio.ByteOrder.LITTLE_ENDIAN).getLong();
                Date date = new Timestamp(t_sec * 1000);
//               Instant instant = ofEpochSecond(t_sec);

                System.out.println(date);
            } else {
                return -1;
            }
        } else {
            System.out.printf("SendAndConfirm rc = %x\n", rc);
        }

        return rc;
    }

    //MCMD_SETTIME
    int SetTime(int sts) {
        int rc, t, ms;
        long lms;
        ByteBuffer bb = ByteBuffer.allocate(4);
        Instant it;
        Date now = new Date();
        lms = now.getTime();
        t = (int) (lms/1000);
        ms = (int) (lms%1000);

        Msg1 ucmd = new Msg1();
        Msg1 outcmd = new Msg1();
        ucmd.cmd = MCMD_SETTIME;
        System.out.printf("SetTime t %d ms %d\n", t, ms);

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(t);
        System.arraycopy (bb.array(), 0, ucmd.Buf, 0,4);
        bb.clear();

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(ms*1000);
        System.arraycopy (bb.array(), 0, ucmd.Buf, 4,4);
        bb.clear();

        System.out.printf("controller_server.SendAndConfirm()\n");

        rc = controller_server.SendAndConfirm(ucmd, 8, outcmd, 0);
        if (rc == 0) {

        }
        System.out.printf("SetTime rc %d\n", rc);
        return rc;
    }
    /***************************************************/
//MCMD_IDENTIFY
    int  Identify_server()
    {   int rc, itmp, lname, ldate ;
        short lp;
        byte tmp[] = new byte[4];
        byte tmpmac[] = new byte[6];
        byte tmpdate[] = new byte[20];

        ByteBuffer bb = ByteBuffer.allocate(4);

        short b_flags, itmp2;
        Msg1 ucmd = new Msg1();
        Msg1 outcmd = new Msg1();
        ucmd.cmd = MCMD_IDENTIFY;
        rc =  remote_server.SendAndConfirm2(ucmd, 6, outcmd,126, 6, 1,0x12);
   System.out.printf("SendAndConfirm2  rc = %x\n", rc);
        if (rc == 0)
        {    int idcode, idtype, idNumber;
            short sl;
            sl = (short) ((outcmd.Buf[0]& 0xff) | (outcmd.Buf[1]&0xff) << 8);

            System.arraycopy(outcmd.Buf, 2,tmp,0,4);
            remote_server.servertype = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            System.arraycopy(outcmd.Buf, 6,tmp,0,4);
            remote_server.servercode = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
//   servertype  DS_SMARTSERVER
//               DS_OPENTHERM
            System.arraycopy(outcmd.Buf, 10,tmp,0,4);
            remote_server.server_id = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            System.arraycopy(outcmd.Buf, 14,tmp,0,4);
            remote_server.server_Vers = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            System.arraycopy(outcmd.Buf, 18,tmp,0,4);
            remote_server.server_SubVers = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            System.arraycopy(outcmd.Buf, 22,tmp,0,4);
            remote_server.server_SubVers1 = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            System.arraycopy(outcmd.Buf, 26,tmpdate,0,12);
            tmpdate[12] = 0; tmpdate[13] = 0;
            remote_server.server_date = new String(tmpdate,0, 12);

            System.arraycopy( outcmd.Buf, 38, tmpmac,0,6);
            System.arraycopy(tmpmac,0, remote_server.Mac_Addr, 0,6);

            remote_server.serverIdentify = new String(outcmd.Buf,44, sl-44+2);
            np_server++;
        }
        return rc;
    }
// MCMD_INTRODUCESELF
    int Send_to_server_IdentifySelf()
    {
        int rc, lname, ldate ;
        short lp;
        byte tmp[] = new byte[4];
        byte tmpmac[] = new byte[6];
        ByteBuffer bb = ByteBuffer.allocate(4);

        Msg1 ucmd = new Msg1();
        Msg1 outcmd = new Msg1();
        ucmd.cmd = MCMD_INTRODUCESELF;
        byte[] bname = IdentifyStr.getBytes(Charset.defaultCharset());
        lname = bname.length;
        if(lname > ucmd.Buf.length-16)
            lname = ucmd.Buf.length - 16;

        lp = (short) (2+4*6 +6 +12 + lname);

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort(lp);
        System.arraycopy (bb.array(), 0, ucmd.Buf, 0,2);
        bb.clear();

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(IDENTIFY_TYPE);
        System.arraycopy (bb.array(), 0, ucmd.Buf, 2,4);
        bb.clear();

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(IDENTIFY_CODE);
        System.arraycopy (bb.array(), 0, ucmd.Buf, 6,4);
        bb.clear();

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(versionCode); //=IdNumber
        System.arraycopy (bb.array(), 0, ucmd.Buf, 10,4);
        bb.clear();

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(Vers);
        System.arraycopy (bb.array(), 0, ucmd.Buf, 14,4);
        bb.clear();

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(SubVers);
        System.arraycopy (bb.array(), 0, ucmd.Buf, 18,4);
        bb.clear();

        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(SubVers1);
        System.arraycopy (bb.array(), 0, ucmd.Buf, 22,4);
        bb.clear();

        byte[] bdate = BuildDate.getBytes(Charset.defaultCharset());
        ldate = bdate.length;
        if(ldate > 12)
            ldate = 12;
        System.arraycopy (bdate,0,  ucmd.Buf, 26,ldate); //BiosDate
        System.arraycopy(myboiler.MacAddr, 0, ucmd.Buf, 38, 6);
        System.arraycopy (bname,0,   ucmd.Buf, 44,lname); //IDENTIFY_TEXT
        rc = remote_server.SendAndConfirm( ucmd, lp,  outcmd, 12);
        if(rc == 0)
        {   np_server++;
            System.arraycopy(outcmd.Buf, 0,tmp,0,4);
            myboiler.remote_server_ClientId = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            System.arraycopy(outcmd.Buf, 4,tmp,0,4);
            myboiler.remote_server_ClientId_k = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            System.arraycopy(outcmd.Buf, 8,tmp,0,4);
            myboiler.have_remote_server_ClientId = true;
            server_user_online_report_period = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
        }

        return rc;
    }

    //ACMD_ASK_STS
    int Ask_RemoteServer_MyContoller() {
        int rc, itmp, b_flags4;
        byte tmp[] = new byte[4];
        byte tmpmac[] = new byte[6];

        Msg1 ucmd = new Msg1();
        Msg1 outcmd = new Msg1();
        ucmd.cmd = ACMD_ASK_STS_S;

        System.arraycopy(myboiler.MacAddr, 0, ucmd.Buf, 0, 6);
        rc = remote_server.SendAndConfirm(ucmd, 6, outcmd, 4);
        if (rc == 0) {
            np_server++;
            System.arraycopy(outcmd.Buf, 0, tmp, 0, 4);
            b_flags4 = ByteBuffer.wrap(tmp).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            if(b_flags4 == 0x01) {
                rc = Send_to_server_IdentifySelf();
                System.out.printf("Send_to_server_IdentifySelf rc=%d\n", rc);
            } else {
                rc = 0x10;
            }
        }
        return rc;
    }

/***************************************************/
    static  int raz = 0;
    static  int color = 0;
    static  int bcolor = 0;
    static long told_redraw =0;
    int info_sts_server = 0;
    public  void RedrawInfoButton(Context context,  MaterialButton bt_connect_sts)
    {
        int rid=0;
        String str="";
        long diffInSec, t_mills;
        Date now = new Date();

        t_mills = System.currentTimeMillis();
        if(t_mills - told_redraw > 2000)
        {   told_redraw = t_mills;
        } else if(need_update_connect_info_event == 0) {
            return;
        }

        raz++;

        bt_connect_sts.setBackgroundTintMode(SRC);
//        ConectStatus_txt.setBackgroundTintMode(SRC);
        if(sts_controller > 0) {
            if(sts_controller == 1) {
//              bcolor =  ContextCompat.getColor(context,R.color.myBlueConroller );
                bcolor =   ResourcesCompat.getColor(context.getResources(),R.color.myBlueConroller, null);
                diffInSec = Math.abs(now.getTime() - MainActivity.st.controller_server.Last_work.getTime())/1000;
                if(MainActivity.st.controller_server.work && diffInSec > 10)
                    str = String.format(Locale.ROOT, "Нет связи с контроллером %d сек %d (%d %d)", diffInSec, raz, sts_controller, sts_server);
                else
                    str = String.format(Locale.ROOT, "Связываемся с контроллером %d (%d %d)", raz, sts_controller, sts_server);
            } else if(sts_controller == 2) {
                bcolor =   ResourcesCompat.getColor(context.getResources(),R.color.myGreenConroller, null);
//                bt_connect_sts.setBackgroundTintList(ColorStateList.valueOf(R.color.myGreenConroller));
                str = String.format(Locale.ROOT, "Связываемся с контроллером %d (%d %d)", raz, sts_controller, sts_server);
//                   str = String.format(Locale.ROOT, "Связь с контроллером OK %d (%d %d)", raz, sts_controller, sts_server);
            } else if(sts_controller == 3) {
                bcolor =   ResourcesCompat.getColor(context.getResources(),R.color.myGreenConroller1, null);
                str = String.format(Locale.ROOT, "Связь с контроллером работает %d (%d %d)", raz, sts_controller, sts_server);
            } else if(sts_controller == 10) {
                bcolor =   ResourcesCompat.getColor(context.getResources(),R.color.myWaitConroller, null);
                str = String.format(Locale.ROOT, "Пауза %d (%d %d)", raz, sts_controller, sts_server);
            } else {
                bcolor =   ResourcesCompat.getColor(context.getResources(),R.color.myRedConroller, null);
                str = String.format(Locale.ROOT, "Ошибка связи с контроллером %d (%d %d)", raz, sts_controller, sts_server);
            }
            //ConectStatus_txt.setBackgroundTintList(ColorStateList.valueOf(ResourcesCompat.getColor(getResources(), R.color.myGreenConroller, null)));
        } else if(sts_server > 0) {
            if(info_sts_server == 3)
            {   diffInSec = Math.abs(now.getTime() - MainActivity.st.remote_server.Last_work.getTime());
                if(diffInSec > 15000)
                    info_sts_server = sts_server;
            } else {
                info_sts_server = sts_server;
            }

            if(info_sts_server == 1) {
                bcolor =   ResourcesCompat.getColor(context.getResources(),R.color.myBlueServer, null);
                str = String.format(Locale.ROOT, "Связываемся с сервером %d (%d %d)", raz, sts_controller, sts_server);
            } else if(info_sts_server == 2) {
                bcolor =   ResourcesCompat.getColor(context.getResources(),R.color.myGreenServer, null);
                str = String.format(Locale.ROOT, "Связь с сервером ОК %d (%d %d)", raz, sts_controller, sts_server);
            } else if(info_sts_server == 3) {
                bcolor = ResourcesCompat.getColor(context.getResources(), R.color.myGreenServer1, null);
                str = String.format(Locale.ROOT, "Связь с сервером работает %d (%d %d) %d", raz, sts_controller, sts_server, np_server);
            } else if(info_sts_server == 10) {
                bcolor =   ResourcesCompat.getColor(context.getResources(),R.color.myWaitConroller, null);
                str = String.format(Locale.ROOT, "Пауза %d (%d %d)", raz, sts_controller, sts_server);
            } else if(info_sts_server == 11) {
                bcolor =   ResourcesCompat.getColor(context.getResources(),R.color.myWaitConroller, null);
                str = String.format(Locale.ROOT, "Host Unknown %s (%d %d)", ServerIpAddress, sts_controller, sts_server);
            }  else if(info_sts_server == 0x40) {
                bcolor = ResourcesCompat.getColor(context.getResources(), R.color.myRedServer1, null);
                str = String.format(Locale.ROOT, "Серевер не знает контроллера %d (%d %d)", raz, sts_controller, sts_server);
            }  else if(info_sts_server == 0x41) {
                bcolor = ResourcesCompat.getColor(context.getResources(), R.color.myRedServer1, null);
                str = String.format(Locale.ROOT, "Connection failed %d (%d %d)", raz, sts_controller, sts_server);
            }  else if(info_sts_server == 0x42) {
                bcolor = ResourcesCompat.getColor(context.getResources(), R.color.myRedServer1, null);
                str = String.format(Locale.ROOT, "Connection ok, response wrong %d (%d %d)", raz, sts_controller, sts_server);
            }  else if(info_sts_server == 0x43) {
                bcolor = ResourcesCompat.getColor(context.getResources(), R.color.myRedServer1, null);
                str = String.format(Locale.ROOT, "Connection ok, handshake wrong %d (%d %d)", raz, sts_controller, sts_server);
            }  else if(info_sts_server == 0x44) {
                bcolor = ResourcesCompat.getColor(context.getResources(), R.color.myRedServer1, null);
                str = String.format(Locale.ROOT, "Failed to connect to %s port %d, %d (%d %d)", ServerIpAddress, ServerPort, raz, sts_controller, sts_server);
            } else {
                bcolor =   ResourcesCompat.getColor(context.getResources(),R.color.myRedServer, null);
                str = String.format(Locale.ROOT, "Ошибка связи с сервером %d (%d %d)", raz, sts_controller, sts_server);
            }
        } else {
            bcolor =   ResourcesCompat.getColor(context.getResources(),R.color.myGreyOffline, null);
            str = String.format(Locale.ROOT, "Нет связи  %d (%d %d)", raz, sts_controller, sts_server);
        }
//        ConectStatus_txt.setTextColor(color);
//        ConectStatus_txt.setBackgroundColor(ResourcesCompat.getColor(getResources(), rid, null));
//        ConectStatus_txt.setText(str);

//        bt_connect_sts.setBackgroundTintList(ColorStateList.valueOf(rid));
        color = sutils.InvertColor(bcolor); // 0xff405040;
        bt_connect_sts.setBackgroundTintList(ColorStateList.valueOf(bcolor));
        bt_connect_sts.setText(str);
        bt_connect_sts.setTextColor(color);
        need_update_connect_info_event = 0;
    }

//    public int ActivityesFlag; // флаги активностей. Каждая при onResume() устанавливет флаг, в OnStop сбрасывает
 //   boolean Theads_Sleep = false;
   public void SleepDispatcher(boolean flag, int id)
    {   if(flag)
            ActivityesFlag |= (1<<id);
        else
            ActivityesFlag &= ~(1<<id);

        if(ActivityesFlag != 0)
            Theads_Sleep = false;
        else
            Theads_Sleep = true;
    }

//    int LocalHomeNetsts =-1; //-1 - неизвестно, 0 Не локальная или не домашняя сеть 1 - локальная домашняя сеть -

    void CheckLocalHomeNet()
    {   int rc;
        sutils.GetMyIPandMask();
        rc = sutils.CheckIPis_inRage(myboiler.ControllerIpAddress);
        if(rc == 1)
            LocalHomeNetsts = 1;
        else
            LocalHomeNetsts = 0;

    }

}
