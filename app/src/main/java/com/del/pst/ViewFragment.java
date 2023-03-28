package com.del.pst;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.del.pst.dao.AppDatabase;
import com.del.pst.dao.DBItem;
import com.del.pst.dao.DBItemValue;
import com.del.pst.databinding.FrViewBinding;
import com.del.pst.session.ConnectionManager;
import com.del.pst.session.Session;
import com.del.pst.utils.ListItemValue;
import com.del.pst.utils.Utils;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ViewFragment extends Fragment {

    private FrViewBinding binding;
    private Long id;
    private DBItem item;
    private ConnectionManager connectionManager;
    private byte[] digestOfPassword;
    private Session session;
    private Map<Long, ListItemValue> values;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        this.id = getArguments().getLong("id");
        session = new ViewModelProvider(requireActivity()).get(Session.class);
        binding = FrViewBinding.inflate(inflater, container, false);
        connectionManager = new ConnectionManager(requireContext());
        this.digestOfPassword = session.getDigestOfPasswordValue();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        if (!Arrays.equals(session.getDigestOfPasswordValue(), digestOfPassword)) {
            this.digestOfPassword = session.getDigestOfPasswordValue();
            NavHostFragment.findNavController(ViewFragment.this).
                    navigate(R.id.action_view_to_list);
        }

        super.onResume();
    }

    private void initValues() {
        values = new HashMap<>();
        AppDatabase db = AppContext.getInstance().getDatabase();
        List<DBItemValue> items = db.itemValueDao().findByItemId(id);
        for (DBItemValue item : items) {
            try {
                values.put(item.getId(), new ListItemValue(item, Utils.decodeValue(item.getName(), digestOfPassword)));
            } catch (Exception e) {
                //
            }
        }
        List<ListItemValue> listItems = new ArrayList<>(values.values());
        Collections.sort(listItems);

        ArrayAdapter<ListItemValue> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, listItems);
        binding.listValues.setAdapter(adapter);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDatabase db = AppContext.getInstance().getDatabase();
        item = db.itemDao().findById(id);
        binding.textView.setText(Utils.decodeValueORNull(item.getName(), digestOfPassword));
        initValues();

        binding.listValues.setOnItemLongClickListener((parent, view13, position, _id) -> {
            ListItemValue valItem = (ListItemValue) binding.listValues.getAdapter().getItem(position);
            PopupMenu popup = new PopupMenu(getContext(), view13);
            popup.setOnMenuItemClickListener(menuItem -> {
                try {
                    DBItemValue dbItemValue = valItem.getItem();
                    dbItemValue.setName(Utils.encodeValue(menuItem.getTitle().toString(), digestOfPassword));
                    db.itemValueDao().updateItem(dbItemValue);
                    initValues();
                } catch (Exception e) {
                    Log.e("UPDATE", "Update error", e);
                    Snackbar.make(requireView(),
                            "Ошибка при сохранении данных",
                            5000).show();
                }
                return true;
            });
            List<String> names = new ArrayList<>();
            for (Long id : values.keySet()) {
                names.add(values.get(id).getName());
            }
            String[] namesArray = getResources().getStringArray(R.array.value_names);
            for (String n : namesArray) {
                if (!names.contains(n)) {
                    popup.getMenu().add(n);
                }
            }
            popup.show();
            return true;
        });
        binding.listValues.setOnItemClickListener((adapterView, view1, i, l) -> {

            ListItemValue val = (ListItemValue) binding.listValues.getAdapter().getItem(i);
            try {
                String text = Utils.decodeValue(val.getItem().getValue(), digestOfPassword);
                if (session.isActive()) {
                    connectionManager.send(session, text,
                            r -> Snackbar.make(requireView(), r, 5000).show(),
                            e -> Snackbar.make(requireView(), Objects.requireNonNull(e.getMessage()), 5000).show()
                    );
                } else {
                    ClipboardManager clipboard = (ClipboardManager) getActivity().
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("QRTransfer", text);
                    clipboard.setPrimaryClip(clip);
                    Snackbar.make(requireView(), "Скопировано в буфер обмена", 5000).show();
                }
            } catch (Exception e) {
                Log.e("View", "Copy value error", e);
            }
        });

        binding.bViewDel.setOnClickListener(view12 -> {
            AlertDialog ad = new AlertDialog.Builder(getContext()).
                    setTitle("Удалить запись").
                    setMessage("Вы уверены что хотете удалить запись?").
                    setNegativeButton("Нет", null).
                    setPositiveButton("Да", (dialogInterface, i) -> {
                        db.itemValuesDao().delete(item);
                        NavHostFragment.findNavController(ViewFragment.this).
                                navigate(R.id.action_view_to_list);
                    }).
                    create();
            ad.show();
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onPause() {
        clearBuffer();
        super.onPause();
    }

    private void clearBuffer() {
        ClipboardManager clipboard = (ClipboardManager) getActivity().
                getSystemService(Context.CLIPBOARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip();
        } else {
            ClipData clip = ClipData.newPlainText("PassTransfer", "");
            clipboard.setPrimaryClip(clip);
        }
    }

}