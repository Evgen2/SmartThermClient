package com.smartthermclient;

import java.util.Date;

//класс с параметрами/функциями только котла
public class SmartBoiler {
    int used;
    byte[] MacAddr = new byte[6];
    int SmartType; //0 - SmartTherm32, 1 - SmartTherm, 2 -  SmartTherm 2
    short OTmemberCode;
    int IknowMycontroller;
    int remote_server_ClientId = -1;     /* Client Id */
    int remote_server_ClientId_k = 0;   /* Client Id key (todo future use) */
    boolean have_remote_server_ClientId = false;
    int Vers = 0;
    int SubVers = 0;
    int SubVers1 = 0;
    int Revision = 0;

    String ControllerIpAddress;
    String Name;
    String FirmwareInfo;
    boolean Use_remoteTCPserver = false; //
    Date Last_OT_work;//время последнего принятого пакета OpenTherm (см также time_of_server_connect)
    Date Last_slaveOT_work;//время последнего принятого пакета slave OpenTherm (см также time_of_server_connect)

    int stsOT; // -1 not init, 0 - normal work, 2 - timeout
    int Slave_stsOT; // -1 not init, 0 - normal work, 2 - timeout
    int BoilerStatus;
    public int CapabilitiesFlags = 0; //Флаги возможностей
    boolean enable_CentralHeating;
    boolean enable_HotWater;
    boolean enable_Cooling;
    boolean enable_CentralHeating2;
    boolean HotWater_present;
    boolean CH2_present;
    boolean RetT_present;
    boolean DhwT_present;
    boolean Toutside_present;
    boolean Pressure_present;
    boolean MQTT_present;
    boolean MQTT_used;
    boolean PID_present;
    boolean PID_used;
    boolean Relay_present;
    boolean Relay_used;
    boolean Relay_sts;   //Relay state
    boolean Relay_sts_toSet;   //Relay state
    boolean OT_slave_present; //OT_slave  present and use

    /******************/

    /**** ****/
    float BoilerT;
    float RetT;
    float TdhwSet; // f8.8  DHW setpoint (°C)
    float Dhw_T;
    float Tset; // задаваемая температура теплоносителя (с контроллера)
    float Toutside; //температура улице, если есть (OT)
    float TempIndoor;   //температура в комнате, если есть
    float TempOutdoor;  //температура на улице, если есть
    float TroomTarget; //целевая температура в комнате, если есть
    float Tset_r;  // заданая температура теплоносителя
    float FlameModulation;
    float Pressure;
    int status_D18b20;
    float t1, t2; // D18b20

    int ToSet_start;
    float Tset_toSet; // задаваемая температура теплоносителя (контроллеру)
    float TroomTarget_toSet; // задаваемая температура помещения
    float TdhwSet_toSet;// задаваемая температура горячей воды
    boolean enable_CentralHeating_toSet;
    boolean enable_HotWater_toSet;

    /******************/

    SmartBoiler() {
        used = 0;
        Name = "Not used";
        SmartType = 0;
        IknowMycontroller = 0;
        MacAddr[0] = MacAddr[1] = MacAddr[2] = MacAddr[3] = MacAddr[4] = MacAddr[5] = 0;
        ControllerIpAddress = "127.0.0.1";
        stsOT = -2;
        Slave_stsOT = -2;
        OTmemberCode = 0;
        BoilerStatus = 0;

        enable_CentralHeating = enable_HotWater = enable_Cooling = enable_CentralHeating2 = false;
        /******************/
        HotWater_present = false;
        CH2_present = false;
        Toutside_present = false;
        Pressure_present = false;
        RetT_present = false;
        DhwT_present = false;
        MQTT_present = MQTT_used = PID_present = PID_used = false;
        BoilerT = 0.F;
        Dhw_T = RetT = 0.F;
        Tset_r = Tset = TdhwSet = 0.F;
        FlameModulation = 0.F;
        Pressure = 0.F;
        status_D18b20 = 0;
        t1 = t2 = 0.F;
        Toutside = 0.f;
        TempIndoor = TempOutdoor = TroomTarget = 0.f;
        FirmwareInfo = null;
        Last_OT_work = Last_slaveOT_work = new Date(0);
        ToSet_start = 1;
        Tset_toSet = TroomTarget_toSet = TdhwSet_toSet = 0;
        enable_CentralHeating_toSet = enable_HotWater_toSet = false;

        Relay_present = Relay_used = Relay_sts = false;
        Relay_sts_toSet = false;
        OT_slave_present = false;
    }
}
