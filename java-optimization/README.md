
# 故障排查
## jstack
jstack 是 jdk 自带的一款工具，用于生成 jvm 当前时刻的线程快照。其指令为：`jstack [option] pid`，执行后展示信息如下所示：

![图片](D:\code\studyOfJava\java-optimization\src\main\resources\jstack.png)

也可以通过该的指令将栈信息保存：`jstack pid > stack[pid].log`。

## jmap
jmap 也是 jdk 自带的一款工具，用于监控 jvm 中的 java 对象。其指令为：`jmap [option] pid`，常用的 option 有以下两个：

### heap
该参数作用是打印堆摘要信息，指令为：`jmap -heap pid`。执行后展示信息如下所示：

![](D:\code\studyOfJava\java-optimization\src\main\resources\jmap-heap.png)

### dump
该参数作用是生成当前 jvm 进程的堆快照（将 jvm 堆信息以 hprof 二进制格式转储到 filename 文件中），指令为：
`jmap -dump:[live,]format=b,file=<filename> pid`（其中 live 是可选参数，如果指定，则只转储堆中的活动对象）。

指令执行结果如下所示：

![](D:\code\studyOfJava\java-optimization\src\main\resources\jmap-dump-1.png)

然后我们就可以将生成好 dump 文件下载到本地，然后使用分析工具来分析该文件。最简单的，我们可以使用 jvm 自带 jvisualvm 来打开 dump 文件。

![](D:\code\studyOfJava\java-optimization\src\main\resources\jmap-dump-2.png)

除了这种主动抓取的方式，还有一种被动获取的方式，可以在 java 程序发生 OOM 时为我们生成 dump 文件。在启动 java 程序的指令中增加参数：`-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=D:\temp`，需要注意的是，通过该方式生成的 dump 文件是 hprof 类型。

## JProfiler
从 jdk9 开始，Visual VM 不包括在 Oracle JDK 和 Open JDK 发行版中。因此，如果我们使用的是 Java 9 或更高版本，可以从 Visual VM 开源项目站点获得 jvisualvm（https://visualvm.github.io/） 。
当然，我们也可以选择地三方工具，JProfiler 就是一个不错的选择。

JProfiler 可以直接监控 java 程序，也可以分析 dump 文件，但是支持的文件类型是 .hprof。

### 堆溢出排查示例
测试代码：
```
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
```

启动增加如下 VM 参数：`-Xms10M -Xmx10M -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=D:\temp`。

运行程序即可看到堆溢出错误：

![](D:\code\studyOfJava\java-optimization\src\main\resources\JProfiler-1.png)

使用 JProfiler 打开 dump 文件：

![](D:\code\studyOfJava\java-optimization\src\main\resources\JProfiler-2.png)

可以看到 OOMObject 对象存在大量实例，查看该对象的持有者（incoming references）, 点击 show more，即可看到出错代码位置：

![](D:\code\studyOfJava\java-optimization\src\main\resources\JProfiler-3.png)




