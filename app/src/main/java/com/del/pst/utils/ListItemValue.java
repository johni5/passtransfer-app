package com.del.pst.utils;

import com.del.pst.dao.DBItemValue;

public class ListItemValue implements Comparable<ListItemValue> {

    private DBItemValue item;
    private String name;

    public ListItemValue(DBItemValue item, String name) {
        this.item = item;
        this.name = name;
    }

    public DBItemValue getItem() {
        return item;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(ListItemValue o) {
        if (o == null) return 1;
        if (o == this) return 0;
        return toString().compareTo(o.toString());
    }
}
