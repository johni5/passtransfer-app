package com.del.pst.dao;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {DBItem.class, DBItemValue.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ItemDao itemDao();

    public abstract ItemValueDao itemValueDao();

    public abstract ItemValuesDao itemValuesDao();

}