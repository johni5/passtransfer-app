package com.del.pst;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;

import com.del.pst.databinding.ValueFieldBinding;
import com.del.pst.utils.PasswordGenerator;

public class FieldValueView extends RelativeLayout {

    private ValueFieldBinding binding;
    private PasswordGenerator passwordGenerator;
    private RemoveListener removeListener;

    public FieldValueView(Context context, PasswordGenerator passwordGenerator, RemoveListener removeListener) {
        super(context);
        this.passwordGenerator = passwordGenerator;
        this.removeListener = removeListener;
        initComponent(context);
    }

    private void initComponent(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.value_field, this);

        binding = ValueFieldBinding.inflate(inflater, this, true);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.value_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.valueNames.setAdapter(adapter);

        binding.valueGenerate.setOnClickListener((l) -> {
            PopupMenu popup = new PopupMenu(getContext(), l);
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.menu_generate:
                        binding.valuePasswd.setText(passwordGenerator.generate());
                        return true;
                    case R.id.menu_clear:
                        binding.valuePasswd.setText("");
                        return true;
                    case R.id.menu_remove:
                        if (removeListener != null) removeListener.remove(this);
                        return true;
                    default:
                        return false;
                }

            });
            MenuInflater menuInflater = popup.getMenuInflater();
            menuInflater.inflate(R.menu.f_value_menu, popup.getMenu());
            popup.show();
        });
    }

    public String getValue() {
        return binding.valuePasswd.getText().toString();
    }

    public String getName() {
        return binding.valueNames.getSelectedItem().toString();
    }

    public void setError(String error) {
        binding.valuePasswd.setError(error);
    }

    public interface RemoveListener {

        void remove(FieldValueView view);

    }
}
