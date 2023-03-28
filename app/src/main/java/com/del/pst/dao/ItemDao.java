package com.del.pst.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ItemDao {

    @Query("select * from pass order by name")
    List<DBItem> getAll();

    @Query("select * from pass where _id=:id limit 1")
    DBItem findById(Long id);

    @Query("select * from pass where name like :name")
    List<DBItem> findByName(String name);

    @Insert
    long addItem(DBItem item);

    @Update
    void updateItem(DBItem item);

    @Delete
    void deleteItem(DBItem item);

}
