package org.study.java.java_optimization.oom;

public class StackOverflowErrorTest {
    public static void main(String[] args) {
        javaKeeper();
    }
    private static void javaKeeper() {
        javaKeeper();
    }
}
