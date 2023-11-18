package org.study.java.java_optimization.oom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

// VM 参数：-Xms10M -Xmx10M
public class GCOverheadLimitTest {
    static class Key {
        Integer id;
        Key(Integer id) {
            this.id = id;
        }
    }
    public static void main(String[] args) {
        Map m = new HashMap();
        while (true) {
            int i = 0;
            m.put(new Key(++i), i);
            System.out.println("m.size()=" + m.size());
        }
    }
}
