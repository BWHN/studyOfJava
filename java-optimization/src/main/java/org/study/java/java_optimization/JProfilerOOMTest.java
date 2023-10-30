package org.study.java.java_optimization;

import java.util.ArrayList;
import java.util.List;

public class JProfilerOOMTest {
    static class OOMObject {}
    public static void main(String[] args) {
        List list = new ArrayList<>();
        //在堆中无限创建对象
        while (true) {
            list.add(new OOMObject());
        }
    }
}
