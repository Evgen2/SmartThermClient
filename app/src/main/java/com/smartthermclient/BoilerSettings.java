package com.smartthermclient;

import static com.smartthermclient.MainActivity.st;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class BoilerSettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boiler_settings);
    }
    public void BBS_SetupReturnToMainActivity(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity((intent));

    }

    public void BBS_SetupSaveAndReturnToMainActivity(View v) {
        double vv;
        int iv;
//         st.controllerIpAddress = (editSetupControllerIP_ef.getText().toString());
//        st.severIpAddress = (editSetupServerIP_ef.getText().toString());
        st.needSavesetup = 1;

        Intent intent = new Intent(this, MainActivity.class);
        startActivity((intent));
    }

}