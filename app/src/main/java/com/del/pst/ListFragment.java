package com.del.pst;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.del.pst.dao.AppDatabase;
import com.del.pst.dao.DBItem;
import com.del.pst.databinding.FrListBinding;
import com.del.pst.session.Session;
import com.del.pst.utils.DBItemAdapter;
import com.del.pst.utils.ListItemName;
import com.del.pst.utils.Utils;
import com.google.android.gms.common.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ListFragment extends Fragment {

    private FrListBinding binding;
    private Session session;
    private List<ListItemName> names;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FrListBinding.inflate(inflater, container, false);
        names = new ArrayList<>();
        session = new ViewModelProvider(requireActivity()).get(Session.class);
        return binding.getRoot();
    }

    private void updateList(byte[] code) {
        binding.list.setVisibility(View.GONE);
        binding.listLoader.setVisibility(View.GONE);
        if (code == null) {
            binding.bListAdd.setVisibility(View.GONE);
        } else {
            binding.bListAdd.setVisibility(View.VISIBLE);
            binding.listLoader.setVisibility(View.VISIBLE);
            new LoadListTask().execute(code);
        }
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.list.setOnItemClickListener((adapterView, view1, i, l) -> {
            Bundle a = new Bundle();
            ListItemName si = (ListItemName) binding.list.getAdapter().getItem(i);
            a.putLong("id", si.getItem().getId());
            binding.fFilter.setText("");
            NavHostFragment.findNavController(ListFragment.this).
                    navigate(R.id.action_list_to_view, a);
        });

        session.getDigestOfPassword().observe(getViewLifecycleOwner(), this::updateList);
        binding.bListAdd.setOnClickListener(view12 ->
                NavHostFragment.findNavController(ListFragment.this)
                        .navigate(R.id.action_list_to_add));

        binding.fFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                filter(editable.toString());
            }
        });
        updateList(null);
    }

    private void filter(String filter) {
        DBItemAdapter adapter = (DBItemAdapter) binding.list.getAdapter();
        if (adapter != null) adapter.getFilter().filter(filter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadListTask extends AsyncTask<byte[], Void, DBItemAdapter> {

        public LoadListTask() {
            super();
        }

        @Override
        protected DBItemAdapter doInBackground(byte[]... s) {
            byte[] digestOfPassword = s[0];
            names.clear();
            AppDatabase db = AppContext.getInstance().getDatabase();
            DBItemAdapter adapter = null;
            try {
                List<DBItem> list = db.itemDao().getAll();
                for (DBItem dbItem : list) {
                    if (Utils.valid(dbItem, digestOfPassword)) {
                        try {
                            names.add(new ListItemName(
                                    dbItem,
                                    Utils.decodeValue(dbItem.getName(), digestOfPassword))
                            );
                        } catch (Exception e) {
                            //
                        }
                    }
                }
                adapter = new DBItemAdapter(getContext(),
                        android.R.layout.simple_list_item_1, names);
                adapter.sort(ListItemName::compareTo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return adapter;
        }

        @Override
        protected void onPostExecute(DBItemAdapter data) {
            if (data != null) {
                binding.list.setAdapter(data);
                binding.list.setVisibility(View.VISIBLE);
                binding.listLoader.setVisibility(View.GONE);
            }
        }
    }

}