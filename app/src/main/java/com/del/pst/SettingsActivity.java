package com.del.pst;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;

import com.del.pst.databinding.ActivitySettingsBinding;
import com.del.pst.utils.Utils;
import com.google.android.gms.common.util.Strings;

public class SettingsActivity extends AppCompatActivity {

    public static final String S_NAME = "SETTINGS";
    public static final String S_APP_NAME = "S_APP_NAME";
    public static final String S_SESSION_TIMEOUT = "S_SESSION_TIMEOUT";
    public static final String S_PSD_SIZE = "S_PSD_SIZE";
    public static final String S_PSD_UPPER = "S_PSD_UPPER";
    public static final String S_PSD_LOWER = "S_PSD_LOWER";
    public static final String S_PSD_PUNCTUATION = "S_PSD_PUNCTUATION";
    public static final String S_PSD_DIGITS = "S_PSD_DIGITS";

    private ActivitySettingsBinding binding;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        settings = getSharedPreferences(S_NAME, MODE_PRIVATE);
        editor = settings.edit();

        binding.appName.setText(settings.getString(S_APP_NAME, getDeviceName()));
        binding.sessionTimeout.setText(String.valueOf(settings.getLong(S_SESSION_TIMEOUT, 10)));
        binding.passSize.setText(String.valueOf(settings.getInt(S_PSD_SIZE, 20)));
        binding.passUseDigits.setChecked(settings.getBoolean(S_PSD_DIGITS, true));
        binding.passUsePunctuation.setChecked(settings.getBoolean(S_PSD_PUNCTUATION, false));
        binding.passUseLower.setChecked(settings.getBoolean(S_PSD_LOWER, true));
        binding.passUseUpper.setChecked(settings.getBoolean(S_PSD_UPPER, true));

        binding.passUseDigits.setOnCheckedChangeListener((compoundButton, b) -> editor.putBoolean(S_PSD_DIGITS, b));
        binding.passUsePunctuation.setOnCheckedChangeListener((compoundButton, b) -> editor.putBoolean(S_PSD_PUNCTUATION, b));
        binding.passUseLower.setOnCheckedChangeListener((compoundButton, b) -> editor.putBoolean(S_PSD_LOWER, b));
        binding.passUseUpper.setOnCheckedChangeListener((compoundButton, b) -> editor.putBoolean(S_PSD_UPPER, b));
        binding.passSize.addTextChangedListener(new SettingsTextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0)
                    editor.putInt(S_PSD_SIZE, Integer.parseInt(editable.toString()));
                else binding.passSize.setError(getString(R.string.required));
            }
        });
        binding.sessionTimeout.addTextChangedListener(new SettingsTextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0)
                    editor.putLong(S_SESSION_TIMEOUT, Long.parseLong(editable.toString()));
                else binding.sessionTimeout.setError(getString(R.string.required));
            }
        });
        binding.appName.addTextChangedListener(new SettingsTextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                editor.putString(S_APP_NAME, editable.toString());

            }
        });
        binding.btnBack.setOnClickListener((l) -> {
            Editable appName = binding.appName.getText();
            if (appName == null || Strings.isEmptyOrWhitespace(appName.toString()))
                binding.appName.setError(getString(R.string.required));
            editor.apply();
            finish();
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        editor.apply();
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return Utils.capitalize(model);
        } else {
            return Utils.capitalize(manufacturer) + " " + model;
        }
    }

    private abstract static class SettingsTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

    }

}
