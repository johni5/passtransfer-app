package com.del.pst.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;
import java.util.Map;

@Dao
public abstract class ItemValuesDao implements ItemDao {

    @Transaction
    public void delete(DBItem item) {
        deleteValues(item.getId());
        deleteItem(item);
    }

    @Insert
    public abstract void addValues(List<DBItemValue> values);

    @Transaction
    public void add(DBItem item, List<DBItemValue> values) {
        long id = addItem(item);
        for (DBItemValue value : values) {
            value.setItemId(id);
        }
        addValues(values);
    }

    @Transaction
    public void importData(Map<DBItem, List<DBItemValue>> importData) {
        for (DBItem item : importData.keySet()) {
            List<DBItem> items = findByName(item.getName());
            for (DBItem dbItem : items) {
                delete(dbItem);
            }
            add(item, importData.get(item));
        }
    }

    @Query("delete from pass_val where pass=:itemId")
    public abstract void deleteValues(Long itemId);

}
