package com.timzaak.cloud.user.dsl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record Pair<K,V>(K key, V value) {
    public static <K,V> Map<K,V> toMap(List<Pair<K, V>> list)  {
        return list.stream().collect(Collectors.toMap(Pair::key, Pair::value));
    }
}
