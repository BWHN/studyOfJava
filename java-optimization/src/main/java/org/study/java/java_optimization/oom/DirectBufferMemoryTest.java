package org.study.java.java_optimization.oom;

import java.nio.ByteBuffer;

// -Xms10m -Xmx10m -XX:+PrintGCDetails -XX:MaxDirectMemorySize=5m
public class DirectBufferMemoryTest {
    public static void main(String[] args) {
        System.out.println("maxDirectMemory is: "+ sun.misc.VM.maxDirectMemory() / 1024 / 1024 + "MB");
//        ByteBuffer buffer = ByteBuffer.allocate(8*1024*1024);  // Java heap space
        ByteBuffer buffer = ByteBuffer.allocateDirect(6*1024*1024);
    }
}
