package com.del.pst;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.del.pst.dao.AppDatabase;
import com.del.pst.dao.DBItem;
import com.del.pst.dao.DBItemValue;
import com.del.pst.databinding.FrAddBinding;
import com.del.pst.session.Session;
import com.del.pst.utils.PasswordGenerator;
import com.del.pst.utils.Utils;
import com.google.android.gms.common.util.Strings;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddFragment extends Fragment implements FieldValueView.RemoveListener {

    private FrAddBinding binding;
    private List<FieldValueView> fields;
    private PasswordGenerator pg;
    private SharedPreferences sharedPreferences;
    private byte[] digestOfPassword;
    private Session session;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FrAddBinding.inflate(inflater, container, false);
        session = new ViewModelProvider(requireActivity()).get(Session.class);
        fields = new ArrayList<>();
        sharedPreferences = getActivity().getSharedPreferences(SettingsActivity.S_NAME, MODE_PRIVATE);
        this.digestOfPassword = session.getDigestOfPasswordValue();
        return binding.getRoot();
    }


    private void init() {
        if (!Arrays.equals(session.getDigestOfPasswordValue(), digestOfPassword)) {
            this.digestOfPassword = session.getDigestOfPasswordValue();
            NavHostFragment.findNavController(AddFragment.this).
                    navigate(R.id.action_add_to_list);
        }
        if (pg == null) {
            pg = new PasswordGenerator();
        }
        pg.setLength(sharedPreferences.getInt(SettingsActivity.S_PSD_SIZE, 20));
        pg.setUseDigits(sharedPreferences.getBoolean(SettingsActivity.S_PSD_DIGITS, true));
        pg.setUseLower(sharedPreferences.getBoolean(SettingsActivity.S_PSD_LOWER, true));
        pg.setUseUpper(sharedPreferences.getBoolean(SettingsActivity.S_PSD_UPPER, true));
        pg.setUsePunctuation(sharedPreferences.getBoolean(SettingsActivity.S_PSD_PUNCTUATION, true));
    }

    @Override
    public void onResume() {
        init();
        super.onResume();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppDatabase db = AppContext.getInstance().getDatabase();

        binding.btnCreate.setOnClickListener(view1 -> {
            FieldValueView fView = new FieldValueView(requireContext(), pg, this);
            fields.add(fView);
            binding.fieldsLayout.addView(fView);
        });

        binding.btnSave.setOnClickListener(view1 -> {
            Map<String, String> form = new HashMap<>();
            Editable name = binding.fName.getText();
            if (name == null || Strings.isEmptyOrWhitespace(name.toString())) {
                binding.fName.setError(getString(R.string.required));
                return;
            }
            try {
                String encodeName = Utils.encodeValue(name.toString(), digestOfPassword);
                if (validate(form) && unique(encodeName, db)) {
                    DBItem item = new DBItem();
                    item.setName(encodeName);
                    item.setCheck(Utils.encodeValue(DBItem.CHECK_ORIG, digestOfPassword));
                    List<DBItemValue> values = new ArrayList<>();
                    for (String fName : form.keySet()) {
                        DBItemValue value = new DBItemValue(
                                null,
                                Utils.encodeValue(fName, digestOfPassword),
                                Utils.encodeValue(form.get(fName), digestOfPassword)
                        );
                        values.add(value);
                    }
                    db.itemValuesDao().add(item, values);
                    NavHostFragment.findNavController(AddFragment.this)
                            .navigate(R.id.action_add_to_list);
                }
            } catch (Exception e) {
                Log.e("ADD", "Create error", e);
                Snackbar.make(requireView(),
                        "Ошибка при сохранении данных",
                        5000).show();
            }
        });

    }

    private boolean unique(String name, AppDatabase db) {
        List<DBItem> list = db.itemDao().findByName(name);
        if (list != null && !list.isEmpty()) {
            binding.fName.setError("Это название уже используется");
            return false;
        }
        return true;
    }

    private boolean validate(Map<String, String> form) {
        for (FieldValueView field : fields) {
            if (form.containsKey(field.getName())) {
                field.setError(getString(R.string.douplicate));
                form.clear();
                return false;
            }
            if (Strings.isEmptyOrWhitespace(field.getValue())) {
                field.setError(getString(R.string.required));
                form.clear();
                return false;
            }
            form.put(field.getName(), field.getValue());
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void remove(FieldValueView view) {
        fields.remove(view);
        binding.fieldsLayout.removeView(view);
    }
}