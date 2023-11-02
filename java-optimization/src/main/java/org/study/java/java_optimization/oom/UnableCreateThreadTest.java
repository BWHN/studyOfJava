package org.study.java.java_optimization.oom;

public class UnableCreateThreadTest {
    public static void main(String[] args) {
        while(true){
            new Thread(() -> {
                try {
                    Thread.sleep(Integer.MAX_VALUE);
                } catch(InterruptedException e) { }
            }).start();
        }
    }
}
