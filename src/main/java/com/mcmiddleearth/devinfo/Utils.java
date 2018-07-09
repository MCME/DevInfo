package com.mcmiddleearth.devinfo;

import java.util.Map;

public class Utils {
    static <K, V> V mapFindOrCreate(Map<K, V> map, K key, V value) {
        V v = map.get(key);
        if(v != null) {
            return v;
        }
        map.put(key, value);
        return value;
    }
}
