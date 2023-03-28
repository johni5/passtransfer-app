package com.del.pst.json;

public class ItemValue {

    public String name;
    public String val;

    public ItemValue() {
    }

    public ItemValue(String name, String val) {
        this.name = name;
        this.val = val;
    }

    @Override
    public String toString() {
        return "ItemValue{" +
                "name='" + name + '\'' +
                ", val='" + val + '\'' +
                '}';
    }
}
