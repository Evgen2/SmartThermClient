package com.smartthermclient;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static com.smartthermclient.MainActivity.st;
import static com.smartthermclient.SmartUtils.validateStringIP;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SetController_Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SetController_Fragment extends DialogFragment {
    TextView Info_title;
    TextView Info_Code_txt;
    TextView Info_MAC_txt;
    TextView Info_Version;
    TextView Info_RC_txt;
    EditText edit_IP_ef;
    EditText edit_Name_ef;
    EditText edit_Model_ef;
    int type;
    int indBoiler=0;

    public SetController_Fragment() {
        // Required empty public constructor
    }

    public static SetController_Fragment newInstance(String title) {
        SetController_Fragment frag = new SetController_Fragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SetController_Fragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SetController_Fragment newInstance(String param1, String param2) {
//todo
        SetController_Fragment fragment = new SetController_Fragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    // 1. Defines the listener interface with a method passing back data result.

    public interface SetController_DialogListener {
//todo

        void onFinishSetControllerDialog(int par);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//todo
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
    }

    // onCreate --> (onCreateDialog) --> onCreateView --> onActivityCreated
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String strtext;
        View dialogView = inflater.inflate(R.layout.fragment_set_controller, container, false);
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_set_controller, container, false);

//
        Info_title = dialogView.findViewById(R.id.fr_sc_title);
        if (getArguments() != null) {
            strtext = getArguments().getString("title");
            if(strtext != null)
                Info_title.setText(strtext);
        }
        assert getArguments() != null;
        type = getArguments().getInt("type", 0);


        assert getArguments() != null;
        indBoiler = getArguments().getInt("indBoiler");

        strtext = "Code " + st.boiler[indBoiler].GetOTmemberCodeName();
        Info_Code_txt = dialogView.findViewById(R.id.fr_sc_Code);
        Info_Code_txt.setText(strtext);

        strtext = String.format("MAC %02x:%02x:%02x:%02x:%02x:%02x",
                st.boiler[indBoiler].MacAddr[0], st.boiler[indBoiler].MacAddr[1], st.boiler[indBoiler].MacAddr[2],
                st.boiler[indBoiler].MacAddr[3], st.boiler[indBoiler].MacAddr[4], st.boiler[indBoiler].MacAddr[5]);

        Info_MAC_txt = dialogView.findViewById(R.id.fr_sc_MAC);
        Info_MAC_txt.setText(strtext);

        Info_Version = dialogView.findViewById(R.id.fr_sc_Vers);
        if(st.boiler[indBoiler].Vers == 0 && st.boiler[indBoiler].SubVers == 0 && st.boiler[indBoiler].SubVers1 == 0 && st.boiler[indBoiler].Revision == 0)
            strtext = "";
        else
            strtext = String.format("vers %d.%d.%d.%d",
                st.boiler[indBoiler].Vers, st.boiler[indBoiler].SubVers, st.boiler[indBoiler].SubVers1, st.boiler[indBoiler].Revision);
        Info_Version.setText(strtext);


        Info_RC_txt = dialogView.findViewById(R.id.fr_sc_RC);
        if(st.boiler[indBoiler].IknowMycontroller == 1)
        {   if(st.boiler[indBoiler].Use_remoteTCPserver)
                    strtext = "Удаленное управление разрешено";
            else
                    strtext = "Удаленное управление запрещено";
        } else {
            strtext = "Контроллер не известен";
        }
        Info_RC_txt.setText(strtext);

        edit_IP_ef = dialogView.findViewById(R.id.fr_sc_IP_ef);
        edit_IP_ef.setText(st.boiler[indBoiler].ControllerIpAddress);

        edit_Name_ef = dialogView.findViewById(R.id.fr_sc_Name_ef);
        edit_Name_ef.setText(st.boiler[indBoiler].Name);

        edit_Model_ef = dialogView.findViewById(R.id.fr_sc_Model_ef);
        edit_Model_ef.setText(st.boiler[indBoiler].Model);


        // "Got it" button
        Button buttonPos = (Button) dialogView.findViewById(R.id.fr_sc_pos_button);
        buttonPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int change = 0;
                String str;
//                showToast("getString(R.string.fr_sc_pos_button)");

                // Return input text back to activity through the implemented listener
                str = edit_IP_ef.getText().toString();
                if(validateStringIP(str)) {
                    if (!st.boiler[indBoiler].ControllerIpAddress.equals(str)) {
                        st.boiler[indBoiler].ControllerIpAddress = str;
                        change = 3;
                    }
                }
                str = edit_Name_ef.getText().toString();
                if(!st.boiler[indBoiler].Name.equals(str)) {
                    st.boiler[indBoiler].Name = str;
                    change |= 0x01;
                }

                str = edit_Model_ef.getText().toString();
                if(!st.boiler[indBoiler].Model.equals(str)) {
                    st.boiler[indBoiler].Model = str;
                    change |= 0x01;
                }

                SetController_DialogListener listener = (SetController_DialogListener) getActivity();
                listener.onFinishSetControllerDialog(change);
                // Close the dialog and return back to the parent activity
                // Dismiss the DialogFragment (remove it from view)
                dismiss();
            }
        });

        // "Cancel" button
        Button buttonNeg = (Button) dialogView.findViewById(R.id.fr_sc_neg_button);
        buttonNeg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                showToast("getString(R.string.fr_sc_neg_button)");
                // If shown as dialog, cancel the dialog (cancel --> dismiss)
                if (getShowsDialog())
                    getDialog().cancel();
                    // If shown as Fragment, dismiss the DialogFragment (remove it from view)
                else
                    dismiss();
            }
        });

        // "Del" button
        Button buttonDel = (Button) dialogView.findViewById(R.id.fr_sc_del_button);
        if(st.Nboilers >1) {
            buttonDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int change = 1;
                    int old = st.indCurrentBoiler;
                    st.DelBoiler(indBoiler);
                    if(old != st.indCurrentBoiler)
                        change |= 0x02;
//                    showToast("getString(R.string.fr_sc_del_button)");
                    SetController_DialogListener listener = (SetController_DialogListener) getActivity();
                    listener.onFinishSetControllerDialog(change);

                    // If shown as dialog, cancel the dialog (cancel --> dismiss)
                    if (getShowsDialog())
                        getDialog().cancel();
                        // If shown as Fragment, dismiss the DialogFragment (remove it from view)
                    else
                        dismiss();
//todo
                }
            });
        } else {
            buttonDel.setEnabled(false);
        }

        return dialogView;
    }

    // If shown as dialog, set the width of the dialog window
    // onCreateView --> onActivityCreated -->  onViewStateRestored --> onStart --> onResume
    @Override
    public void onResume() {
        super.onResume();
//        Log.v(LOG_TAG, "onResume");
        if (getShowsDialog()) {
            // Set the width of the dialog to the width of the screen in portrait mode
            DisplayMetrics metrics = getActivity().getResources().getDisplayMetrics();
            int dialogWidth = Math.min(metrics.widthPixels, metrics.heightPixels);
            getDialog().getWindow().setLayout(dialogWidth, WRAP_CONTENT);
        }
    }

//    private void showToast(String buttonName) {
//        Toast.makeText(getActivity(), "Clicked on \"" + buttonName + "\"", Toast.LENGTH_SHORT).show();
//    }

    // If dialog is cancelled: onCancel --> onDismiss
    @Override
    public void onCancel(DialogInterface dialog) {
//        Log.v(LOG_TAG, "onCancel");
    }

    // If dialog is cancelled: onCancel --> onDismiss
    // If dialog is dismissed: onDismiss
    @Override
    public void onDismiss(DialogInterface dialog) {
//        Log.v(LOG_TAG,"onDismiss");
    }

}