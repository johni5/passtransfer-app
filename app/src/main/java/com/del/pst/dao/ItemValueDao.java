package com.del.pst.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ItemValueDao {

    @Query("select * from pass_val where pass=:id order by name")
    List<DBItemValue> findByItemId(Long id);

    @Query("select * from pass_val order by name")
    List<DBItemValue> getAll();

    @Update
    void updateItem(DBItemValue item);

    @Insert
    void addValues(List<DBItemValue> values);

}
