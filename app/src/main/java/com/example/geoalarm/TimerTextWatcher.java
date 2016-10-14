package com.example.geoalarm;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Created by Иван on 11.10.2016.
 */
public class TimerTextWatcher implements TextWatcher {
    public EditText editText, nextEditText;

    public TimerTextWatcher(EditText editText, EditText nextEditText) {
        super();
        this.editText = editText;
        this.nextEditText = nextEditText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if(editText.length() == 2 && editText.hasFocus()){
            editText.clearFocus();
            if(null != nextEditText){
                nextEditText.requestFocus();
            }
        }
    }
}
