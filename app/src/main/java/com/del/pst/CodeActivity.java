package com.del.pst;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import com.del.pst.databinding.ActivityCodeBinding;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.util.Strings;

public class CodeActivity extends AppCompatActivity {

    public static String CodeObject = "CodeObject";
    private ActivityCodeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        binding.bCode.setOnClickListener((e) -> {
            String code = binding.textCode.getText().toString();
            imm.hideSoftInputFromWindow(binding.bCode.getWindowToken(), 0);
            Intent iData = new Intent();
            if (!Strings.isEmptyOrWhitespace(code)) {
                iData.putExtra(CodeObject, code);
                setResult(CommonStatusCodes.SUCCESS, iData);
            }
            finish();
        });
        binding.textCode.requestFocus();
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

}
