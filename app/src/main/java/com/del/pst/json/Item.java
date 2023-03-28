package com.del.pst.json;

import java.util.List;

public class Item {
    public String name;
    public List<ItemValue> values;

    public Item() {
    }

    public Item(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Item{" +
                "name='" + name + '\'' +
                ", values=" + values +
                '}';
    }
}
