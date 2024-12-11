package com.smartthermclient;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;

import java.util.Date;
import java.util.Locale;

public class Smart_Activity extends AppCompatActivity  implements SetTemp_DialogFragment.SetTemp_DialogListener {

    private static final int TAB_CONNECT = 0;
    private static final int TAB_STATEOT = 1;
    private static final int TAB_STATEBOILER = 2;
    private static final int TAB_STATEBOILERMASTER = 3;
    private static final int TAB_BOILERT = 4;
    private static final int TAB_TSET = 5;
    private static final int TAB_TRET = 6;
    private static final int TAB_HOTWATER = 7;
    private static final int TAB_FLAMEMODULATION = 8;
    private static final int TAB_PRESSURE = 9;
    private static final int TAB_T1_T2 = 10;
    private static final int TAB_TOUTSIDE = 11;
    private static final int TAB_USE_PID_MQTT = 12;
    private static final int TAB_SET_CH = 13;
    private static final int TAB_SET_ROOM_SETPOINT = 14;
    private static final int TAB_SET_DHW_SETPOINT = 15;


    MaterialButton bt_connect_sts;
    Context SA_context;
    TableLayout tl;
    float wPar, wState, wEdit;
    private final int MaxPar = 64;
    int[] NparamsIds = new int[MaxPar];
    int[] Nparams_0_Ids = new int[MaxPar];
    int[] idParams   = new int[MaxPar];
    volatile int Nparams; //число строк в таблице
    volatile int UpdateFlag =0; //
    static int SA_ThreadRun = 0;
    private  static Handler hdlr; // = new Handler();
    int old_stsOT;
    int old_status_D18b20;
    int OldCapabilitiesFlag = 0;

    static int iswork = 0;
    boolean needExitInfoTherad = false;

    @Override
    public void onFinishSetTempDialog(int par, float val, String inputText) {
        System.out.printf("%s\n", inputText);
        switch (par)
        {
            case 1:
                MainActivity.st.myboiler.Tset_toSet = val;
                if(MainActivity.st.sts_controller > 0)    MainActivity.st.NeedSendControllerCmd2 = 6;
                else if (MainActivity.st.sts_server > 0)  MainActivity.st.NeedSendServerCmd = 6;
                break;
            case 2:
                MainActivity.st.myboiler.TroomTarget_toSet = val;
                if(MainActivity.st.sts_controller > 0)    MainActivity.st.NeedSendControllerCmd2 = 6;
                else if (MainActivity.st.sts_server > 0)  MainActivity.st.NeedSendServerCmd = 6;
                break;
            case 3:
                MainActivity.st.myboiler.TdhwSet_toSet = val;
                if(MainActivity.st.sts_controller > 0)    MainActivity.st.NeedSendControllerCmd2 = 6;
                else if (MainActivity.st.sts_server > 0)  MainActivity.st.NeedSendServerCmd = 6;
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_smart);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        bt_connect_sts  = findViewById(R.id.SmartA_ConnectStatus_button);
        SA_context = getApplicationContext();
        tl = (TableLayout) findViewById(R.id.Table_SmartA);
        wPar = 0.5f;
        wEdit = 0.33f;
        wState = 1.f - wPar;
        for(int i=0; i<MaxPar ;i++)
            Nparams_0_Ids[i] = -1;
        Build_Table();
        MainActivity.st.need_update_connect_info_event++;
        StartInfoSmAThread();
    }

    void StartInfoSmAThread() {
        if(SA_ThreadRun == 0)
        {
            SA_ThreadRun = 1;
            needExitInfoTherad = false;

            new Thread(new Runnable() {
                public void run()
                {
                    long t0, t1, t;
                    int sleep_step = 100;
                    int sleep_time = 500; //2000;
                    old_stsOT = MainActivity.st.myboiler.stsOT;
                    old_status_D18b20 = MainActivity.st.myboiler.status_D18b20;
                    OldCapabilitiesFlag  = MainActivity.st.myboiler.CapabilitiesFlags;
                    hdlr =  new Handler(Looper.getMainLooper());
                    while (true) {
                        // Update the progress bar and display the current value in text view
                        hdlr.post(new Runnable() {
                            public void run() {
                                MainActivity.st.RedrawInfoButton(SA_context, bt_connect_sts);
                                if(MainActivity.st.need_update_event > 0) {
                                    UpdateSmartThermTable();
                                    MainActivity.st.need_update_event = 0;
                                } else {
                                    if(old_stsOT != MainActivity.st.myboiler.stsOT ||
                                            old_status_D18b20 != MainActivity.st.myboiler.status_D18b20 ||
                                            OldCapabilitiesFlag != MainActivity.st.myboiler.CapabilitiesFlags)
                                    {   DelSmartThermTable();
                                        Build_Table();
                                        old_stsOT = MainActivity.st.myboiler.stsOT;
                                        old_status_D18b20 = MainActivity.st.myboiler.status_D18b20;
                                        OldCapabilitiesFlag = MainActivity.st.myboiler.CapabilitiesFlags;
                                        UpdateSmartThermTable();
                                    } else {
                                        try {
                                            Thread.sleep(200);
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                        Update_ConnectStatus(0);
                                    }
                                }

                            } //endof run()
                        });  //endof hdlr.post()

                        t0 = System.currentTimeMillis();
                        do {
                            try {
                                Thread.sleep(sleep_step);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            if(needExitInfoTherad) {
                                break;
                            }
                            t1 = System.currentTimeMillis();
                        } while(t1-t0 < sleep_time);

                        if(needExitInfoTherad)
                            break;
                    } // endof while (true)

                    SA_ThreadRun = 0;
                } // endof run()
            }).start();  //Thread()
        } //endof if(ThreadRun == 0)


    }

    void Build_Table()
    {
        MainActivity.st.need_update_event = 1;
        AddTableHeadRow("Параметр", wPar, "Состояние", wState, "Настройка",  wEdit, tl);
        Nparams = 0;
        AddTableRow(Nparams+1, "Связь",wPar, "0", wState, tl,0);  idParams[Nparams++] = TAB_CONNECT;
        AddTableRow(Nparams+1, "OpenTherm",wPar, "0", wState, tl,0);  idParams[Nparams++] = TAB_STATEOT;
        AddTableRow(Nparams+1, "Статус котла"    ,wPar, "0",wState, tl,0);   idParams[Nparams++] = TAB_STATEBOILER;
        AddTableRow(Nparams+1, "Уставка\nот контроллера"    ,wPar, "0",wState, tl,0);   idParams[Nparams++] = TAB_STATEBOILERMASTER;
        AddTableRow(Nparams+1, "Выход,°C"    ,wPar, "0",wState, tl,0);   idParams[Nparams++] = TAB_BOILERT;
        AddTableRow(Nparams+1, "Уставка выхода,°C"    ,wPar, "0",wState, tl,0);   idParams[Nparams++] = TAB_TSET;
        if(MainActivity.st.myboiler.RetT_present)
        {   AddTableRow(Nparams+1, "Обратка,°C"    ,wPar, "0",wState, tl,0);   idParams[Nparams++] = TAB_TRET;
        }
        if(MainActivity.st.myboiler.HotWater_present)
        {   AddTableRow(Nparams+1, "Горячая вода,°C"    ,wPar, "0",wState, tl,0);   idParams[Nparams++] = TAB_HOTWATER;
        }
        AddTableRow(Nparams+1, "Модуляция пламени, %"    ,wPar, "0",wState, tl,0);   idParams[Nparams++] = TAB_FLAMEMODULATION;
        if(MainActivity.st.myboiler.Pressure_present)
        {   AddTableRow(Nparams+1, "Давление"    ,wPar, "0",wState, tl,0);   idParams[Nparams++] = TAB_PRESSURE;
        }
        if((MainActivity.st.myboiler.status_D18b20 & 0x0303) > 0)
        {   AddTableRow(Nparams+1, "T1 | T2,°C"    ,wPar, "0",wState, tl,0);   idParams[Nparams++] = TAB_T1_T2;
        }
        if(MainActivity.st.myboiler.Toutside_present)
        {   AddTableRow(Nparams+1, "Text,°C"    ,wPar, "0",wState, tl,0);   idParams[Nparams++] = TAB_TOUTSIDE;
        }
        if(MainActivity.st.myboiler.MQTT_present || MainActivity.st.myboiler.PID_present)
        {   String str0="";
            if(MainActivity.st.myboiler.MQTT_present && MainActivity.st.myboiler.PID_present)
                str0 = "MQTT & PID";
            else if (MainActivity.st.myboiler.MQTT_present)
                str0 = "MQTT";
            else
                str0 = "PID";
            AddTableRow(Nparams+1, str0    ,wPar, "0",wState, tl,0);   idParams[Nparams++] = TAB_USE_PID_MQTT;
        }

        if(MainActivity.st.myboiler.PID_used)
        {   AddTableRow(Nparams+1, "Уставка температуры\nпомещения,°C"    ,wPar, String.format(Locale.ROOT, "%.1f",MainActivity.st.myboiler.TroomTarget_toSet), wState, tl,2);
            idParams[Nparams++] = TAB_SET_ROOM_SETPOINT;
        } else {
           AddTableRow(Nparams+1, "Уставка температуры\nтеплоносителя,°C",wPar, String.format(Locale.ROOT, "%.1f",MainActivity.st.myboiler.Tset_toSet),wState, tl,1);
//            idSet_Tset_ef = NparamsIds[Nparams];
            idParams[Nparams++] = TAB_SET_CH;
        }
        if(MainActivity.st.myboiler.HotWater_present)
        {   AddTableRow(Nparams+1, "Уставка температуры\nгорячей воды,°C",wPar, String.format(Locale.ROOT, "%.1f",MainActivity.st.myboiler.TdhwSet_toSet),wState, tl,3);
//            idSet_Tset_ef = NparamsIds[Nparams];
            idParams[Nparams++] = TAB_SET_DHW_SETPOINT;
        }

    }

    void AddTableHeadRow(String sPar, float wNumber, String sState, float wName, String sEdit, float wEdit, TableLayout _tl) {
        TableRow.LayoutParams lp;
        TableRow tr = new TableRow(this);
        tr.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));

        TextView tv = new TextView(this);
        tv.setText(sPar);
        lp = new TableRow.LayoutParams(0,TableRow.LayoutParams.WRAP_CONTENT,wNumber);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tr.addView(tv,0);

        tv = new TextView(this);
        tv.setText(sState);
        lp = new TableRow.LayoutParams(0,TableRow.LayoutParams.WRAP_CONTENT,wName);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tr.addView(tv,1);
/*
        tv = new TextView(this);
        tv.setText(sEdit);
        lp = new TableRow.LayoutParams(0,TableRow.LayoutParams.WRAP_CONTENT,wEdit);

        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tr.addView(tv,2);
*/
        tr.setBackgroundColor( 0xffe8e4e4);
        _tl.addView(tr,0);
    }

    //строка динамической таблицы
    void AddTableRow(int ind, String sPar, float wPar, String sState, float wState,TableLayout _tl, int typ)
    {   int id;
        int bcolor;

        TableRow.LayoutParams lp;
        TableRow tr = new TableRow(this);
        tr.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));

        TextView tv = new TextView(this);
        tv.setText(sPar);
        lp = new TableRow.LayoutParams(0,TableRow.LayoutParams.WRAP_CONTENT,wPar);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
        tv.setPadding(2,4,4,8);
        tr.addView(tv,0);
    if(typ == 0) {
        tv = new TextView(this);
        tv.setText(sState);
        lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, wState);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.LEFT);
        tv.setPadding(8, 2, 2, 2);
        id = View.generateViewId();
        tv.setId(id);
        NparamsIds[ind - 1] = id;
        tr.addView(tv, 1);
    } else {
        float ts;
        float factor = SA_context.getResources().getDisplayMetrics().density;
/*
        EditText ef = new EditText(this);
        lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT,wState/2);
        ef.setLayoutParams(lp);
        ef.setGravity(Gravity.CENTER);
        ef.setPadding(2, 2, 2, 2);
        ts = ef.getTextSize();
        ef.setTextSize(14);
        ef.setLeft(10);
        id = View.generateViewId();
        ef.setLayoutParams(lp);

        ef.setId(id);
        NparamsIds[ind - 1] = id;
        ef.setText(sState);
        ef.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ef.setKeyListener(DigitsKeyListener.getInstance(Locale.ROOT, false,true));

        tr.addView(ef, 1);
 */
        tv = new TextView(this);
        tv.setText(sState);
        lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT,wState/2);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setPadding(8, 2, 2, 2);
        id = View.generateViewId();
        tv.setId(id);
        NparamsIds[ind - 1] = id;
        tr.addView(tv, 1);

        lp = new TableRow.LayoutParams(0,TableRow.LayoutParams.WRAP_CONTENT,wState/2);
        MaterialButton bt = new MaterialButton(this);

//        android:minHeight="30dip"
//        android:text="inf"
//        android:textSize="12sp"

        bt.setText("Изменить");
        lp.height = (int)(30.f * factor);
        bt.setLayoutParams(lp);
        bt.setCornerRadius(20);
        //bt.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        bt.setGravity(Gravity.CENTER);
        ts = bt.getTextSize();
        bt.setTextSize(12.f);
        //    bt.setMinHeight(12);
//        bt.setHeight(14);
        bt.setPadding(2, 2, 2, 2);
        id = View.generateViewId();
        Nparams_0_Ids[ind - 1] = id;
        bt.setId(id);

        if(typ == 1)
            bt.setOnClickListener(this::SA_Set_Tset);
        else if(typ == 2)
            bt.setOnClickListener(this::SA_Set_TroomTarget);
        else if(typ == 3)
            bt.setOnClickListener(this::SA_Set_TdhwSet_toSet);

        tr.addView(bt,2);

    }
        if((ind &0x01) == 0x0) {
            bcolor = ResourcesCompat.getColor(SA_context.getResources(), R.color.CurrentSelect, null);
            tr.setBackgroundColor(bcolor);
        }
//??        else
//??            bcolor = 0xfffefdfd;


//        int[] NparamsIds = new int[MaxPar];
//        int[] idParams   = new int[MaxPar];

/*
        lp = new TableRow.LayoutParams(0,TableRow.LayoutParams.WRAP_CONTENT,wEdit);
        MaterialButton bt = new MaterialButton(this);
        bt.setCornerRadius(40);
        bt.setText(sEdit);
        bt.setLayoutParams(lp);
        id = View.generateViewId();
        NeditRoomsIds[ind-1] = id;
        //todo save id
        bt.setId(id);
        bt.setOnClickListener(this::StartEditRoomActivity);
        tr.addView(bt,2);
*/
        _tl.addView(tr,ind);

    }

    void UpdateSmartThermTable()
    {   int i;
        UpdateFlag++;
        for(i=0; i<Nparams; i++) {
            switch (idParams[i])
            {   case TAB_CONNECT: Update_ConnectStatus(i);
                    break;
                case TAB_STATEOT: Update_StateOT(i);
                    break;
                case TAB_STATEBOILER: Update_StateBoiler(i);
                    break;
                case TAB_STATEBOILERMASTER: Update_StateBoilerMaster(i);
                    break;
                case TAB_BOILERT:
                    Update_BoilerT(i);
                    break;
                case TAB_TSET:
                    Update_Tset(i);
                    break;
                case TAB_TRET:
                    Update_Tret(i);
                    break;
                case TAB_HOTWATER:
                    Update_HotWater(i);
                    break;
                case TAB_FLAMEMODULATION:
                    Update_FlameModulation(i);
                    break;
                case TAB_PRESSURE:
                    Udate_Pressure(i);
                    break;
                case TAB_T1_T2:
                    Update_T1_T2(i);
                    break;
                case TAB_TOUTSIDE:
                    Update_Toutside(i);
                    break;
                case TAB_USE_PID_MQTT:
                    Update_PID_MQTT(i);
                    break;
                case TAB_SET_CH:
                    Update_SetT_CH(i);
                    break;
                case TAB_SET_ROOM_SETPOINT:
                    Update_Set_Room_Setpoint(i);
                    break;
                case TAB_SET_DHW_SETPOINT:
                    Update_SetT_DHW(i);
                    break;
            }
        }
        UpdateFlag--;
    }

    void DelSmartThermTable()
    {   int i;
        int count = tl.getChildCount();
        if( UpdateFlag == 0) {
            if (Nparams > 0) {
                Nparams = 0;

                for (i = 0; i < count; i++) {
                    tl.removeViewAt(count - 1 - i);
                }
            }
        } else {
           System.out.printf("DelSmartThermTable UpdateFlag %d\n", UpdateFlag);

        }
    }

    //TAB_CONNECT
    void Update_ConnectStatus(int ind)
    {   int id;
        long diffInMillies, OT_work_time;
        long diffInMillies1;
        long diffInMillies2;
        String str ="todo";
        if(Nparams == 0)
            return;
        id = NparamsIds[ind];
        TextView tv = findViewById(id);
        if(tv == null)
            return;
        UpdateFlag++;

        Date now = new Date();
        OT_work_time = MainActivity.st.myboiler.Last_OT_work.getTime();
//Last_OT_work
//Last_server_ST_work
//controller_server.Last_work
//remote_server.Last_work
        if(MainActivity.st.LastMessage_isfrom == -1)
        {
            str = "Нет связи";

        } else if (MainActivity.st.LastMessage_isfrom == 0) { //0 - controller
            if(MainActivity.st.myboiler.stsOT <= -1 || OT_work_time == 0)
            {
                str = String.format(Locale.ROOT, "котёл: н/д");

            } else {
                diffInMillies = Math.abs(now.getTime() - OT_work_time);
                System.out.printf("diffInMillies ms %d\n", diffInMillies);

                diffInMillies1 = Math.abs(now.getTime() - MainActivity.st.controller_server.Last_work.getTime());
                str = String.format(Locale.ROOT, "котёл %s\nконтроллер %s", GetDayHourMinSecMc(diffInMillies), GetDayHourMinSecMc(diffInMillies1));
            }

        } else if (MainActivity.st.LastMessage_isfrom == 1) { //, 1 - remote server
            String str0 ="";
            str = "котёл: ";
            if(MainActivity.st.myboiler.stsOT == -2)
            {
                str += String.format(Locale.ROOT, "н/д\nконтроллер: н/д");
                diffInMillies2 = Math.abs(now.getTime() - MainActivity.st.remote_server.Last_work.getTime());
                str += "\nсервер " + GetDayHourMinSecMc(diffInMillies2);

            } else {
                if(MainActivity.st.myboiler.stsOT == -1 || OT_work_time == 0)
                {
                    str += String.format(Locale.ROOT, "н/д");

                } else {
                    diffInMillies = Math.abs(now.getTime() - OT_work_time);

//            System.out.printf("diffInMillies ms %d\n", diffInMillies);
                    diffInMillies1 = Math.abs(now.getTime() - MainActivity.st.Last_server_ST_work.getTime());
                    diffInMillies2 = Math.abs(now.getTime() - MainActivity.st.remote_server.Last_work.getTime());

                    str += GetDayHourMinSecMc(diffInMillies) +
                            "\nконтроллер " + GetDayHourMinSecMc(diffInMillies1) +
                            "\nсервер " + GetDayHourMinSecMc(diffInMillies2);
                }
            }

        }
/*
        if(MainActivity.st.stsOT == -1)
            str = "Не инициализирован";
        else if(MainActivity.st.stsOT == 0)
            str = "онлайн";
        else if(MainActivity.st.stsOT == 2) {
            Date now = new Date();
            long diffInMillies = Math.abs(now.getTime() - MainActivity.st.Last_OT_work.getTime());
            str = String.format(Locale.ROOT, "Потеря связи с котлом\n%d секунд назад", (int)(diffInMillies / 1000.));
        }
*/
        tv.setText(str);
        UpdateFlag--;
    }

    String GetDayHourMinSecMc(long diffInMillies)
    {   String str0 ="";
        if (diffInMillies > 60000 * 60 * 24) {
            str0 = String.format(Locale.ROOT, "%.1f суток", diffInMillies / (60000. * 60 * 24));
        } else if (diffInMillies > 60000 * 60 * 2) {
            str0 = String.format(Locale.ROOT, "%.1f часов", diffInMillies / (60000. * 60));
        } else if (diffInMillies > 60000 * 2) {
            str0 = String.format(Locale.ROOT, "%.1f минут", diffInMillies / (60000.));
        } else if (diffInMillies > 1000 *2) {
            str0 = String.format(Locale.ROOT, "%.1f сек", diffInMillies/1000.);
        } else {
            str0 = String.format(Locale.ROOT, "%d мс", diffInMillies);
        }
        return str0;
    }
    //TAB_STATEOT
    void Update_StateOT(int ind)
    {   int id;
        String str ="";
        id = NparamsIds[ind];
        TextView tv = findViewById(id);
        if(MainActivity.st.myboiler.stsOT == -1)
            str = "Не инициализирован";
        else if(MainActivity.st.myboiler.stsOT == -2)
            str = "нет данных";
        else if(MainActivity.st.myboiler.stsOT == 0)
            str = "онлайн";
        else if(MainActivity.st.myboiler.stsOT == 2) {
            Date now = new Date();
            long diffInMillies = Math.abs(now.getTime() - MainActivity.st.myboiler.Last_OT_work.getTime());
            str = String.format(Locale.ROOT, "Потеря связи с котлом\n%d секунд назад", (int)(diffInMillies / 1000.));
        }

        tv.setText(str);
    }
    //TAB_STATEBOILER
    void Update_StateBoiler(int ind)
    {   int id, sts;
        String str;
        sts = MainActivity.st.myboiler.BoilerStatus;
        id = NparamsIds[ind];
        TextView tv = findViewById(id);
        str = String.format(Locale.ROOT, "hex %x",  sts);
        if((sts & 0x01) == 0x01)
            str += "\nОшибка";
        if((sts & 0x02) == 0x02)
            str += "\nОтопление Вкл";
        else
            str += "\nОтопление вЫкл";
        if(MainActivity.st.myboiler.HotWater_present)
        { if((sts & 0x04) == 0x04)
            str += "\nГорячая вода Вкл";
        else
            str += "\nГорячая вода вЫкл";
        }
        if((sts & 0x08) == 0x08)
            str += "\nГорелка Вкл";
        else
            str += "\nГорелка вЫкл";

        if((sts & 0x10) == 0x10)
            str += "\nОхлаждение Вкл";
//                else
//            str += "\nОхлаждение вЫкл";
        if((sts & 0x20) == 0x20)
            str += "\nОтопление2  Вкл";
//        else
//            str += "\nОтопление2 вЫкл";
        if((sts & 0x40) == 0x40)
            str += "\nСервис/диагностика";

        if((sts & 0x80) == 0x80)
            str += "\nElectricity production (???)";

        tv.setText(str);
    }

    //TAB_STATEBOILERMASTER
    void Update_StateBoilerMaster(int ind)
    {   int id, sts;
        String str ="";
        sts = MainActivity.st.myboiler.BoilerStatus;
        id = NparamsIds[ind];
        TextView tv = findViewById(id);

        if((sts & 0x100) == 0x100)
            str += "Отопление Вкл";
        else
            str += "Отопление вЫкл";
        if((sts & 0x200) == 0x200)
            str += "\nГорячая вода вкл";
        if((sts & 0x400) == 0x400)
            str += "\nОхлаждение вкл";
        if((sts & 0x800) == 0x800)
            str += "\nOTC active";
        if((sts & 0x1000) == 0x1000)
            str += "\nОтопление2 вкл";
        if((sts & 0x2000) == 0x2000)
            str += "\nSummer/winter mode";
        if((sts & 0x4000) == 0x4000)
            str += "\nDHW blocking";

        tv.setText(str);

    }

    //TAB_BOILERT
    void Update_BoilerT(int ind)
    {   int id;
        String str;
        id = NparamsIds[ind];
        TextView tv = findViewById(id);
        str = String.format(Locale.ROOT, "%.2f",MainActivity.st.myboiler.BoilerT );
        tv.setText(str);
    }

    //TAB_TSET
    void Update_Tset(int ind)
    {   int id;
        String str;
        id = NparamsIds[ind];
        TextView tv = findViewById(id);
        str = String.format(Locale.ROOT, "%.2f | %.2f",MainActivity.st.myboiler.Tset, MainActivity.st.myboiler.Tset_r );
        tv.setText(str);
    }

    //TAB_TRET
    void Update_Tret(int ind)
    {   int id;
        String str;
        id = NparamsIds[ind];
        TextView tv = findViewById(id);
        str = String.format(Locale.ROOT, "%.2f",MainActivity.st.myboiler.RetT );
        tv.setText(str);
    }

    //TAB_HOTWATER
    void Update_HotWater(int ind)
    {   int id;
        String str;
        id = NparamsIds[ind];
        TextView tv = findViewById(id);
        str = String.format(Locale.ROOT, "%.2f",MainActivity.st.myboiler.Dhw_T );
        tv.setText(str);
    }

    //TAB_FLAMEMODULATION
    void Update_FlameModulation(int ind)
    {   int id;
        String str;
        id = NparamsIds[ind];
        TextView tv = findViewById(id);
        str = String.format(Locale.ROOT, "%.2f",MainActivity.st.myboiler.FlameModulation );
        tv.setText(str);
    }

    //TAB_PRESSURE
    void Udate_Pressure(int ind)
    {   int id;
        String str;
        id = NparamsIds[ind];
        TextView tv = findViewById(id);
        str = String.format(Locale.ROOT, "%.2f",MainActivity.st.myboiler.Pressure );
        tv.setText(str);
    }

    //TAB_T1_T2
    void Update_T1_T2(int ind)
    {   int id;
        String str ="";
        id = NparamsIds[ind];
        TextView tv = findViewById(id);
        if((MainActivity.st.myboiler.status_D18b20 & 0x0003) > 0)
            str += String.format(Locale.ROOT, "%.2f",MainActivity.st.myboiler.t1 );
        str += " | ";
        if((MainActivity.st.myboiler.status_D18b20 & 0x0300) > 0)
            str += String.format(Locale.ROOT, "%.2f",MainActivity.st.myboiler.t2 );
        tv.setText(str);
    }


    //TAB_TOUTSIDE
    void Update_Toutside(int ind)
    {   int id;
        String str;
        id = NparamsIds[ind];
        TextView tv = findViewById(id);
        str = String.format(Locale.ROOT, "%.2f",MainActivity.st.myboiler.Toutside );
        tv.setText(str);
    }

    //TAB_USE_PID_MQTT
    void Update_PID_MQTT(int ind)
    {   int id;
        String str="";
        id = NparamsIds[ind];
        TextView tv = findViewById(id);
        if(MainActivity.st.myboiler.MQTT_present) {
            if (MainActivity.st.myboiler.MQTT_used)
                str = "MQTT используется";
            else
                str = "MQTT не используется";
            if (MainActivity.st.myboiler.PID_present)
                str += "\n";
        }
        if(MainActivity.st.myboiler.PID_present)
        {   if (MainActivity.st.myboiler.PID_used)
                str += "PID управление";
            else
                str += "PID отключен";
        }
        tv.setText(str);
    }

    //TAB_SET_CH
    void Update_SetT_CH(int ind)
    {   int id;
        String str;

        id = NparamsIds[ind];
        TextView tv = findViewById(id);

        str = String.format(Locale.ROOT, "%.2f",MainActivity.st.myboiler.Tset_toSet );
        tv.setText(str);
    }

//    TAB_SET_DHW_SETPOINT
    void Update_SetT_DHW(int ind)
    {   int id;
        String str;

        id = NparamsIds[ind];
        TextView tv = findViewById(id);

        str = String.format(Locale.ROOT, "%.2f",MainActivity.st.myboiler.TdhwSet_toSet);
        tv.setText(str);
    }

//????? todo
    void Delete_SetT_CH(int ind)
    {   int id;
        id = NparamsIds[ind];
        EditText ef = findViewById(id);
        ((ViewGroup) ef.getParent()).removeView(ef);
        id = Nparams_0_Ids[ind];
        MaterialButton bt  = findViewById(id);
        ((ViewGroup) bt.getParent()).removeView(bt);
    }


    //TAB_SET_ROOM_SETPOINT
    void Update_Set_Room_Setpoint(int ind)
    {   int id;
        String str;

        id = NparamsIds[ind];
        TextView tv = findViewById(id);

        if(MainActivity.st.myboiler.TroomTarget != MainActivity.st.myboiler.TroomTarget_toSet)
        {   str = String.format(Locale.ROOT, "%.2f %.2f",
                MainActivity.st.myboiler.TroomTarget_toSet, MainActivity.st.myboiler.TroomTarget);

        } else {
            str = String.format(Locale.ROOT, "%.2f",
                    MainActivity.st.myboiler.TroomTarget_toSet );
        }

        tv.setText(str);

    }

    public void SA_Set_Tset(View v) {
/*  return ib onFinishSetTempDialog */

        Bundle bundle = new Bundle();
        bundle.putInt("type",1);
        bundle.putString("title", "Уставка температуры");
        bundle.putString("edttext", "теплоносителя");
        bundle.putFloat("Float", MainActivity.st.myboiler.Tset_toSet);
// set Fragmentclass Arguments
/****************/
        FragmentManager fm = getSupportFragmentManager();
        SetTemp_DialogFragment editNameDialogFragment = SetTemp_DialogFragment.newInstance("");
        editNameDialogFragment.setArguments(bundle);
        editNameDialogFragment.show(fm, "Set Tset");
    }

    public void SA_Set_TroomTarget(View v) {
        /*  return ib onFinishSetTempDialog */

        Bundle bundle = new Bundle();
        bundle.putInt("type",2);
        bundle.putString("title", "Уставка температуры");
        bundle.putString("edttext", "в помещении");
        bundle.putFloat("Float", MainActivity.st.myboiler.TroomTarget_toSet);
// set Fragmentclass Arguments
        FragmentManager fm = getSupportFragmentManager();
        SetTemp_DialogFragment editNameDialogFragment = SetTemp_DialogFragment.newInstance("");
        editNameDialogFragment.setArguments(bundle);
        editNameDialogFragment.show(fm, "Set TroomTarget");
    }



    public void SA_Set_TdhwSet_toSet(View v) {
   /*  return ib onFinishSetTempDialog */
        Bundle bundle = new Bundle();
        bundle.putInt("type",3);
        bundle.putString("title", "Уставка температуры");
        bundle.putString("edttext", "горячей воды");
        bundle.putFloat("Float", MainActivity.st.myboiler.TdhwSet_toSet);
    // set Fragmentclass Arguments
    FragmentManager fm = getSupportFragmentManager();
    SetTemp_DialogFragment editNameDialogFragment = SetTemp_DialogFragment.newInstance("");
        editNameDialogFragment.setArguments(bundle);
        editNameDialogFragment.show(fm, "Set DHWTarget");
}

public void SA_ReturnToMainActivity(View v) {
        needExitInfoTherad = true;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Intent intent = new Intent(this, MainActivity.class);
        startActivity((intent));
    }
    @Override
    protected void onStop()
    {
        super.onStop();
        needExitInfoTherad = true;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        MainActivity.st.SleepDispatcher(false, 1);
    }

    @Override
    protected void onResume()
    {
        super. onResume();
        MainActivity.st.SleepDispatcher(true, 1);
        StartInfoSmAThread();
    }


}