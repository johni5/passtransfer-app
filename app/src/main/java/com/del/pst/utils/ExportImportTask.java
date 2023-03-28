package com.del.pst.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.del.pst.AppContext;
import com.del.pst.dao.AppDatabase;
import com.del.pst.dao.DBItem;
import com.del.pst.dao.DBItemValue;
import com.del.pst.json.Item;
import com.del.pst.json.ItemValue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportImportTask extends AsyncTask<String, Void, String> {

    private final byte[] digestOfPassword;
    private final Mode mode;
    private Callback<String> callback;

    public ExportImportTask(byte[] digestOfPassword, Mode mode, Callback<String> callback) {
        super();
        this.digestOfPassword = digestOfPassword;
        this.mode = mode;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(String... p) {
        String password = p[0];
        switch (mode) {
            case EXPORT:
                return exportData(password);
            case IMPORT:
                String json = p[1];
                return importData(password, json);
        }
        return null;
    }

    private String importData(String password, String json) {
        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Item>>() {
            }.getType();
            List<Item> items = gson.fromJson(json, listType);
            Map<DBItem, List<DBItemValue>> importData = new HashMap<>();
            for (Item item : items) {
                List<DBItemValue> vList = new ArrayList<>();
                for (ItemValue value : item.values) {
                    vList.add(new DBItemValue(
                            null,
                            Utils.encodeValue(value.name, digestOfPassword),
                            Utils.encodeValue(value.val, digestOfPassword)
                    ));
                }
                DBItem i = new DBItem(
                        null,
                        Utils.encodeValue(item.name, digestOfPassword)
                );
                i.setCheck(Utils.encodeValue(DBItem.CHECK_ORIG, digestOfPassword));
                importData.put(i, vList);
            }

            AppDatabase db = AppContext.getInstance().getDatabase();
            db.itemValuesDao().importData(importData);
            return "Успешно: " + importData.size();
        } catch (Exception e) {
            Log.e("IMPORT", e.getMessage(), e);
        }
        return null;
    }

    private String exportData(String password) {
        AppDatabase db = AppContext.getInstance().getDatabase();
        List<Item> list = new ArrayList<>();
        Map<Long, List<DBItemValue>> values = new HashMap<>();
        for (DBItemValue itemValue : db.itemValueDao().getAll()) {
            if (!values.containsKey(itemValue.getItemId()))
                values.put(itemValue.getItemId(), new ArrayList<>());
            values.get(itemValue.getItemId()).add(itemValue);
        }
        for (DBItem dbItem : db.itemDao().getAll()) {
            if (Utils.valid(dbItem, digestOfPassword)) {
                try {
                    Item item = new Item(Utils.decodeValue(dbItem.getName(), digestOfPassword));
                    list.add(item);
                    item.values = new ArrayList<>();
                    for (DBItemValue itemValue : values.get(dbItem.getId())) {
                        item.values.add(new ItemValue(
                                Utils.decodeValue(itemValue.getName(), digestOfPassword),
                                Utils.decodeValue(itemValue.getValue(), digestOfPassword)
                        ));
                    }
                } catch (Exception e) {
                    //
                }
            }
        }
        if (!list.isEmpty()) {
            Gson gson = new Gson();
            String json = gson.toJson(list);
            try {
                byte[] export = Utils.encodeData(json, password, true);
                String path = Utils.writeToFile(export);
                return String.format("%s записей сохранено в '%s'", list.size(), path);
            } catch (Exception e) {
                Log.e("EXPORT", e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String data) {
        if (callback != null) callback.call(data);
    }

    public enum Mode {
        EXPORT,
        IMPORT
    }

}
