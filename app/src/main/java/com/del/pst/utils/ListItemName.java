package com.del.pst.utils;

import com.del.pst.dao.DBItem;

public class ListItemName implements Comparable<ListItemName> {

    private DBItem item;
    private String name;

    public ListItemName(DBItem item, String name) {
        this.item = item;
        this.name = name;
    }

    public DBItem getItem() {
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
    public int compareTo(ListItemName o) {
        if (o == null) return 1;
        if (o == this) return 0;
        return toString().compareTo(o.toString());
    }
}
