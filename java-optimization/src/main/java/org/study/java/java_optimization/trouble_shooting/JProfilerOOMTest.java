package org.study.java.java_optimization.trouble_shooting;

import java.util.ArrayList;
import java.util.List;

// VM 参数：-Xms10M -Xmx10M -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=D:\temp
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
