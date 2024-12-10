package com.smartthermclient;

import static com.smartthermclient.MainActivity.st;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.FragmentManager;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

public class SetupActivity extends AppCompatActivity  implements SetController_Fragment.SetController_DialogListener {
    EditText editSetupControllerPort_ef;
    EditText editSetupControllerTimeout_ef;
//   CheckBox chSetupUseDebugLog;
//    EditText editSetupDebugLogFname_ef;
    EditText editSetupServerIP_ef;
//    EditText editSetupServerUserName_ef;
//    EditText editSetupServerUserPwd_ef;
    EditText editSetupServerPort_ef;
    EditText editSetupServerTimeout_ef;
    Context SetA_context;
    MaterialButton bt_addController;
    TableLayout tl;
    int[] NeditControllerIds = new int[SmartTherm.MaxBoilers];
    int[] NtestCheckBoxes = new int[SmartTherm.MaxBoilers];

    static int SetA_ThreadRun = 0;
    private  static Handler hdlr; // = new Handler();
    boolean needExitInfoTherad = false;
    boolean need_update_Controller_table = true;
    int isChange = 0; // & 0x01 need write setup  & 0x02  needRestartSetup

    @Override
    public void onFinishSetControllerDialog(int par) {
        need_update_Controller_table = true;
        isChange |= par;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        SetA_context = getApplicationContext();
        editSetupControllerPort_ef = findViewById(R.id.editSetupControllerPort);
        editSetupControllerTimeout_ef= findViewById(R.id.editSetupControllerTimeout);
//        chSetupUseDebugLog = findViewById(R.id.checkBox_Setup_useDebug);
//        editSetupDebugLogFname_ef = findViewById(R.id.editSetupDebugLogFname);

        editSetupServerIP_ef =     findViewById(R.id.editSetupServerIP);
        editSetupServerPort_ef =     findViewById(R.id.editSetupServerPort);
        editSetupServerTimeout_ef = findViewById(R.id.editSetupServerTimeout);
//        editSetupServerUserName_ef = findViewById(R.id.editSetupServerUserName);
//        editSetupServerUserPwd_ef = findViewById(R.id.editSetupServerPassword);

        editSetupControllerPort_ef.setText(String.valueOf(st.ControllerPort));
        editSetupServerIP_ef.setText(String.valueOf(st.ServerIpAddress));
        editSetupServerPort_ef.setText(String.valueOf(st.ServerPort));
//        editSetupDebugLogFname_ef.setText(String.valueOf(st.DebugLogFname));
//        editSetupServerUserName_ef.setText(String.valueOf(st.ServerUserName));
//        editSetupServerUserPwd_ef.setText(String.valueOf(st.ServerUserPwd));

        editSetupServerTimeout_ef.setText(String.valueOf(st.ServerTimeout));
        editSetupControllerTimeout_ef.setText(String.valueOf(st.ControllerTimeout));
//        if(st.UseDebugLog)
//            chSetupUseDebugLog.setChecked(true);
//        else
//            chSetupUseDebugLog.setChecked(false);
        tl = (TableLayout) findViewById(R.id.Table_setup_controller);
//        BuildControllersTable();

        bt_addController = findViewById(R.id.SA_AddController);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bt_addController.setTooltipText("Добавить контроллер");
        }

        StartInfoSetAThread();
    }

    void StartInfoSetAThread()
    {
        if(SetA_ThreadRun == 0) {
            SetA_ThreadRun = 1;
            needExitInfoTherad = false;
            new Thread(new Runnable() {
                public void run()
                {
                    long t0, t1, t;
                    int sleep_step = 100;
                    int sleep_time = 500; //2000;
                    hdlr =  new Handler(Looper.getMainLooper());
                    while (true) {
                        hdlr.post(new Runnable() {
                            public void run() {
                                if(need_update_Controller_table) {
                                    UpdateControllers();
                                } else {
                                    try {
                                        Thread.sleep(200);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
//                                    Update_ConnectStatus(0);
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

                    SetA_ThreadRun = 0;
                } // endof run()
            }).start();  //Thread()
        } //endof if(ThreadRun == 0)

    }

    void UpdateControllers()
    {   Delete_Table();
        BuildControllersTable();
        need_update_Controller_table = false;
    }

    void Delete_Table()
    {   int i;
        int count = tl.getChildCount();
        for (i = 0; i < count; i++) {
            tl.removeViewAt(count - 1 - i);
        }
    }

    void BuildControllersTable()
    {
        float wNumber, wName, wEdit;
        float w0,w1,w2,w3;
        int i, isCheck;
        wNumber = 0.14f;
        wEdit = 0.33f;
        wName = 1.f - (wNumber + wEdit);
        w0 = 0.1f;
        w1 = 0.3f;
        w2 = 0.3f;
        w3 = 1.f - (w0+w1+w2);
        AddTableHeadRow("Исп.", w0, "Название", w1, "IP адрес", w2, "Настройка",  w3, tl);
        i = 0;
        for(i=0;i<st.Nboilers;i++) {
            isCheck = 0;
            if(i == st.indCurrentBoiler)
                isCheck = 1;
            AddTableRow(i + 1, isCheck, w0, st.boiler[i].Name, w1, st.boiler[i].ControllerIpAddress,w2, "Жми", w3, tl);
        }
    }
    //Первая строка динамической таблицы с названиями столбцов
    void AddTableHeadRow(String sText0, float _w0, String sText1, float _w1, String sText2, float _w2,  String sText3, float _w3, TableLayout tl)
    {   int col = 0;
        TextView tv;
        TableRow.LayoutParams lp;
        TableRow tr = new TableRow(this);
        tr.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));

        if(st.Nboilers > 1) {
            tv = new TextView(this);
            tv.setText(sText0);
            lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, _w0);
            tv.setLayoutParams(lp);
            tv.setGravity(Gravity.CENTER_HORIZONTAL);
            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
            tr.addView(tv, col++);
        }

        tv = new TextView(this);
        tv.setText(sText1);
        lp = new TableRow.LayoutParams(0,TableRow.LayoutParams.WRAP_CONTENT,_w1);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tr.addView(tv,col++);

        tv = new TextView(this);
        tv.setText(sText2);
        lp = new TableRow.LayoutParams(0,TableRow.LayoutParams.WRAP_CONTENT,_w2);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tr.addView(tv,col++);

        tv = new TextView(this);
        tv.setText(sText3);
        lp = new TableRow.LayoutParams(0,TableRow.LayoutParams.WRAP_CONTENT,_w3);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tr.addView(tv,col);

        tl.addView(tr,0);
    }

    //строка динамической таблицы
    void AddTableRow(int ind, int check, float _w0, String sName, float _w1, String Ipaddr, float _w2, String sEdit, float _w3, TableLayout tl)
    {   int id, col=0;
        int bcolor;
        TableRow.LayoutParams lp;
        TableRow tr = new TableRow(this);
        tr.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));
        if(st.Nboilers > 1) {
            if (check != 0) {
                bcolor = ResourcesCompat.getColor(SetA_context.getResources(), R.color.CurrentSelect, null);
                tr.setBackgroundColor(bcolor);
            }

            MaterialCheckBox cb = new MaterialCheckBox(this);
            lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, _w0);
            cb.setLayoutParams(lp);
            cb.setGravity(Gravity.CENTER);
            if (check != 0)
                cb.setChecked(true);
            else
                cb.setChecked(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                cb.setTooltipText("Задать используемый котёл");
            }

            id = View.generateViewId();
            NtestCheckBoxes[ind - 1] = id;
            cb.setId(id);
            cb.setOnCheckedChangeListener(this::SelectCheckBox_one);

            tr.addView(cb, col++);
        }

        TextView tv = new TextView(this);


        tv = new TextView(this);
        tv.setText(sName);
        lp = new TableRow.LayoutParams(0,TableRow.LayoutParams.WRAP_CONTENT,_w1);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tr.addView(tv,col++);

        tv = new TextView(this);
        tv.setText(Ipaddr);
        lp = new TableRow.LayoutParams(0,TableRow.LayoutParams.WRAP_CONTENT,_w2);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tr.addView(tv,col++);

        lp = new TableRow.LayoutParams(0,TableRow.LayoutParams.WRAP_CONTENT,_w3);
        MaterialButton bt = new MaterialButton(this);
        float factor = SetA_context.getResources().getDisplayMetrics().density;
        lp.height = (int)(30.f * factor);
        bt.setCornerRadius(30);
        bt.setText(sEdit);
        bt.setPadding(2, 2, 2, 2);
        bt.setLayoutParams(lp);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bt.setTooltipText("Жми кнопку для настройки");
        }
        id = View.generateViewId();
        NeditControllerIds[ind-1] = id;
        bt.setId(id);
        bt.setOnClickListener(this::StartSetController_fragment);
        tr.addView(bt,col);

        tl.addView(tr,ind);

    }

    public  void StartSetController_fragment(@Nullable View v)
    {   int id, id0, i, is;
        assert v != null;
        id = v.getId();
        is = id0 = 0;
        for (i = 0; i < st.Nboilers; i++)
        {   if (id == NeditControllerIds[i])
            {   is = 1;
                id0 = i;
                break;
            }
        }
        if(is == 0)
            return;

        Bundle bundle = new Bundle();
        bundle.putInt("type",1);
        bundle.putString("title", "Параметры контроллера");
        bundle.putInt("indBoiler", id0);

// set Fragmentclass Arguments
/****************/
        FragmentManager fm = getSupportFragmentManager();
        SetController_Fragment editNameDialogFragment = SetController_Fragment.newInstance("");
        editNameDialogFragment.setArguments(bundle);
        editNameDialogFragment.show(fm, "Set Controller");
    }

    static int iswork = 0;
    public  void SelectCheckBox_one(CompoundButton buttonView, boolean isChecked)
    {
        int id, n, is, i, rc,  id0, oldcurrent;

        oldcurrent = SmartTherm.indCurrentBoiler;

        if(iswork == 0) {
            iswork = 1;
            id = buttonView.getId();
            is = id0 = 0;
            for (i = 0; i < st.Nboilers; i++) {
                if (id == NtestCheckBoxes[i]) {
                    is = 1;
                    id0 = i;
                    SmartTherm.indCurrentBoiler = i;
                    break;
                }
            }
            /* только один контроллер может быть выбран */
            if (isChecked)
            {   MaterialCheckBox cb;
                for (i = 0; i < st.Nboilers; i++) {
                    cb = (MaterialCheckBox) findViewById(NtestCheckBoxes[i]);
                    if (i == id0) {
                        cb.setChecked(true);
                    } else {
                        if (cb.isChecked())
                            cb.setChecked(false);
                    }
                }
            } else {
                MaterialCheckBox cb;
                id0 = (id0 +1) % st.Nboilers;
                SmartTherm.indCurrentBoiler = id0;
                for (i = 0; i < st.Nboilers; i++) {
                    cb = (MaterialCheckBox) findViewById(NtestCheckBoxes[i]);
                    if (i == id0) {
                        cb.setChecked(true);
                    } else {
                        if (cb.isChecked())
                            cb.setChecked(false);
                    }
                }
            }
        }
        iswork = 0;
        need_update_Controller_table = true;
        if(SmartTherm.indCurrentBoiler != oldcurrent)
            isChange |= 0x02;
    }


    //    static final int MaxBoilers = 4;
    //static public SmartBoiler boiler[] = new SmartBoiler [MaxBoilers];
    //int Nboilers;

    public void SA_AddController(View v) {
        st.AddBoiler();
        isChange |= 0x01;

        need_update_Controller_table = true;
    }


    public void BS_SetupReturnToMainActivity(View v) {
        needExitInfoTherad = true;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        if((isChange & 0x02) == 0x02) {
            SmartTherm.needRestartSetup = 1;
        }
        Intent intent = new Intent(this, MainActivity.class);
        startActivity((intent));

    }

    public void BS_SetupSaveAndReturnToMainActivity(View v) {
        double vv;
        int iv;
        String str;

        st.myboiler = st.boiler[SmartTherm.indCurrentBoiler];

        iv = Integer.parseInt(editSetupControllerPort_ef.getText().toString());
        st.ControllerPort = iv;
        str = (editSetupServerIP_ef.getText().toString());
        if(!st.ServerIpAddress.equals(str) )
        {   st.ServerIpAddress = str;
            isChange |= 0x03;
        }
        iv = Integer.parseInt(editSetupServerPort_ef.getText().toString());
        st.ServerPort = iv;
        iv = Integer.parseInt(editSetupServerTimeout_ef.getText().toString());
        st.ServerTimeout = iv;
        iv = Integer.parseInt(editSetupControllerTimeout_ef.getText().toString());
        st.ControllerTimeout = iv;
//        if(chSetupUseDebugLog.isChecked())  st.UseDebugLog = true;
//        else st.UseDebugLog = false;
//        st.DebugLogFname = (editSetupDebugLogFname_ef.getText().toString());

        if((isChange & 0x01) == 0x01)
            SmartTherm.needSavesetup = 1;
        if((isChange & 0x02) == 0x02) {
            SmartTherm.needRestartSetup = 1;
            SmartTherm.needSavesetup = 1;
        }

        Intent intent = new Intent(this, MainActivity.class);
        startActivity((intent));
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        needExitInfoTherad = true;
        MainActivity.st.SleepDispatcher(false, 2);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        MainActivity.st.SleepDispatcher(true, 2);

    }

}