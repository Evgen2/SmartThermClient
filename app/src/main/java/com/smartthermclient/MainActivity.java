package com.smartthermclient;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.google.android.material.button.MaterialButton;

import org.eclipse.paho.android.service.BuildConfig;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {
    static int onstart = 1;
    static int iswork = 0;
//    TextView ConectStatus_txt;
    MaterialButton bt_connect_sts;
    Context SA_context;

    static public SmartTherm st = new SmartTherm();
//    final String SetupFile = "SmartThermClient_settings";
//    final String className = "SmartThermMain";
    static int ThreadRun = 0;
    boolean needExitInfoTherad = false;
    private  static Handler hdlr; // = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SA_context = getApplicationContext();
        GetMyVersion();

        if(onstart == 1) {
            setup(1);
            onstart = 0;
        }
        if(SmartTherm.needSavesetup != 0) {
            WriteSetup();
        }

        bt_connect_sts  =  this.findViewById(R.id.ConnectStatus_button);


        StartInfoMAThread();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        iswork = 0;
        st.SleepDispatcher(false, 0);
        needExitInfoTherad = true; //обязательно иначе после onResume не будет работать обновление st.RedrawInfoButton()
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Context SA_context1 = getApplicationContext();
        iswork = 1;
        st.SleepDispatcher(true, 0);
        StartInfoMAThread();

    }

    public void StartSettingsActivity(View v) {
        Intent intent = new Intent(this, SetupActivity.class);
        startActivity((intent));
    }
    public void ExitSmartApp(View v) {

        finish();
        finishAffinity();
        // on below line we are exiting our activity
        System.exit(0);
    }

    public void StartSmart_Activity(View v) {
        if(SmartTherm.myboiler.IknowMycontroller == 0 || (!st.controller_server.work && !st.remote_server.work))
            MyWarning();
        else
        {   Intent intent = new Intent(this, Smart_Activity.class);
            startActivity((intent));
        }
    }

    void setup(int mode)
    {   int rc;

        if(mode == 1) {
            rc = ReadSetup();
            System.out.printf("Read rc = %d\n", rc);
        }
        // apply now() method of LocalTime class
        LocalTime time = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            time = LocalTime.now();
        }

        // print time
        System.out.println("Time: " + time);
        st.Init();
        st.ControllerConnection();
        SmartTherm.needRestartSetup = 0;
    }


    public int ReadSetup() {
        FileInputStream fin = null;
        try {
            fin = openFileInput(SmartTherm.SetupFile);
            st.read(fin);
            fin.close();
        } catch (FileNotFoundException e) {
            System.out.printf("file %s not found\n", SmartTherm.SetupFile);
            e.printStackTrace();
            return 1;
        } catch (IOException e) {
            System.out.print("io error\n");
            e.printStackTrace();
            return 2;
        }

        return 0;
    }

    public  int WriteSetup()
    { int rc;
        FileOutputStream fout = null;

        try {
            // отрываем поток для записи
            fout = openFileOutput(SmartTherm.SetupFile, MODE_PRIVATE);
            // пишем данные
            rc = st.Write(fout);
            //           fout.write("Содержимое файла".getBytes());
            // закрываем поток
            fout.close();
            System.out.printf("Write %s file rc=%d\n", SmartTherm.SetupFile, rc);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public void MyWarning() {
        String str="";
        // Create the object of AlertDialog Builder class
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        if(!st.controller_server.work)
        {   if(SmartTherm.myboiler.IknowMycontroller == 0)
            str = "С контроллером нет связи\nнастройте параметры";
            else if(!st.remote_server.work)
            str = "Нет связи\nни с контроллером, ни с сервером";
        } else if(SmartTherm.myboiler.IknowMycontroller == 0) {
            str = "Связь с контроллером есть,\nно или нет связи с котлом, или надо подождать минуту";
        }

        if(SmartTherm.myboiler.IknowMycontroller == 0 || (!st.controller_server.work && !st.remote_server.work))

            // Set the message show for the Alert time
        builder.setMessage(str);

        // Set Alert Title
        builder.setTitle("Предупреждение");

        // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
        builder.setCancelable(false);
/*
                // Set the positive button with yes name Lambda OnClickListener method is use of DialogInterface interface.
        builder.setPositiveButton("Yes", (DialogInterface.OnClickListener) (dialog, which) -> {
            // When the user click yes button then app will close
            finish();
        });
*/
        // Set the Negative button with No name Lambda OnClickListener method is use of DialogInterface interface.
        builder.setNegativeButton("Понятно", (dialog, which) -> {
            // If user click no then dialog box is canceled.
            dialog.cancel();
        });

        // Create the Alert dialog
        AlertDialog alertDialog = builder.create();
        // Show the Alert Dialog box
        alertDialog.show();
    }

    void GetMyVersion()
    {
        String versionName="";
        int versionCode = 0;
        st.IdentifyStr = getString(R.string.app_name);
        String s = BuildConfig.APPLICATION_ID;
// Get Build time from https://stackoverflow.com/questions/71428412/how-can-i-display-the-build-time-in-my-android-application
// ...Another alternative is to replace
        String s2 = getResources().getString(R.string.build_time);
        Scanner sca = new Scanner(s2);
        st.BuildDate = sca.next();
        sca.close();

//        long int buildDate = Date(BuildConfig.BUILD_TIME.toLong());

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = (int) packageInfo.getLongVersionCode();
            }
            versionName = packageInfo.versionName;
//            ts = packageInfo.firstInstallTime;
//            ts = packageInfo.lastUpdateTime;
            // Now, you can use versionCode and versionName as needed.
            // For example, you can display them in a TextView or log them.
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

//        Date installTime = new Date( ts );
        sca = new Scanner(versionName);
        sca.useDelimiter("\\.");

        st.Vers = sca.nextInt();
        if(sca.hasNext()) {
            st.SubVers = sca.nextInt();
            if (sca.hasNext())
                st.SubVers1 = sca.nextInt();
        }
        sca.close();
        st.versionCode = versionCode;
    }

    static InfoMAThread Info_Thread;

    void StartInfoMAThread() {
        if (ThreadRun == 0) {
            Info_Thread = new InfoMAThread();
            needExitInfoTherad = false;
            Thread threadInfo = new Thread(Info_Thread,  "i0");
            threadInfo.start();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    class InfoMAThread implements Runnable {
        public void run() {
            long t0, t1, t;
            int sleep_step = 100;
            int sleep_time = 500; //2000;

            hdlr =  new Handler(Looper.getMainLooper());
            while (true) {
                // Update the progress bar and display the current value in text view
                hdlr.post(new Runnable() {
                    public void run() {
                        if(iswork > 0)
                            st.RedrawInfoButton(SA_context, bt_connect_sts);

                        if(SmartTherm.needSavesetup > 0)
                            WriteSetup();
                        if( SmartTherm.needRestartSetup > 0)
                           setup(0);
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

            ThreadRun = 0;
        }
    }

}