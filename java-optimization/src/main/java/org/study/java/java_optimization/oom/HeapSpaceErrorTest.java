package org.study.java.java_optimization.oom;

// -Xms10M -Xmx10M
public class HeapSpaceErrorTest {
    static final int SIZE = 2 * 1024 * 1024;
    public static void main(String[] args) {
        int[] i = new int[SIZE];
    }
}
