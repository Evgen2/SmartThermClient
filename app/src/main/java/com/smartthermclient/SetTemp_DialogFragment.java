package com.smartthermclient;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import java.util.Locale;

public class SetTemp_DialogFragment extends DialogFragment {

    private final String LOG_TAG = SetTemp_DialogFragment.class.getSimpleName();
    TextView Info_title;
    TextView Info_txt;
    EditText edit_ef;
    int type;

    public SetTemp_DialogFragment() {
        // Empty constructor required for DialogFragment
    }
    public static SetTemp_DialogFragment newInstance(String title) {
        SetTemp_DialogFragment frag = new SetTemp_DialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }
    // 1. Defines the listener interface with a method passing back data result.

    public interface SetTemp_DialogListener {
        void onFinishSetTempDialog(int par, float val, String inputText);
    }

    // onCreate --> (onCreateDialog) --> onCreateView --> onActivityCreated
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String strtext;
        Log.v(LOG_TAG, "onCreateView");
        View dialogView = inflater.inflate(R.layout.set_temp_dialog, container, false);
//
        Info_title = dialogView.findViewById(R.id.set_temp_title);
        if (getArguments() != null) {
            strtext = getArguments().getString("title");
            if(strtext != null)
                Info_title.setText(strtext);
        }
        assert getArguments() != null;
        type = getArguments().getInt("type", 0);
        strtext = getArguments().getString("edttext");
        Info_txt = dialogView.findViewById(R.id.fr_message);
        Info_txt.setText(strtext);

        edit_ef = dialogView.findViewById(R.id.fr_ef);
        float v = getArguments().getFloat("Float");
        strtext = String.format(Locale.ROOT, "%.2f", v);

        edit_ef.setText(strtext);
        // "Got it" button
        Button buttonPos = (Button) dialogView.findViewById(R.id.pos_button);
        buttonPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Return input text back to activity through the implemented listener
                edit_ef.getText();
                float vv = Float.parseFloat(edit_ef.getText().toString());

                SetTemp_DialogListener listener = (SetTemp_DialogListener) getActivity();
                listener.onFinishSetTempDialog(type, vv, "ляля тополя");
                // Close the dialog and return back to the parent activity
                // Dismiss the DialogFragment (remove it from view)
                dismiss();
            }
        });

        // "Cancel" button
        Button buttonNeg = (Button) dialogView.findViewById(R.id.neg_button);
        buttonNeg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If shown as dialog, cancel the dialog (cancel --> dismiss)
                if (getShowsDialog())
                    getDialog().cancel();
                    // If shown as Fragment, dismiss the DialogFragment (remove it from view)
                else
                    dismiss();
            }
        });

        return dialogView;
    }

    // If shown as dialog, set the width of the dialog window
    // onCreateView --> onActivityCreated -->  onViewStateRestored --> onStart --> onResume
    @Override
    public void onResume() {
        super.onResume();
        Log.v(LOG_TAG, "onResume");
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
        Log.v(LOG_TAG, "onCancel");
    }

    // If dialog is cancelled: onCancel --> onDismiss
    // If dialog is dismissed: onDismiss
    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.v(LOG_TAG,"onDismiss");
    }
}