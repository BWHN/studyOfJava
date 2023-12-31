<!-- TOC -->
* [动态代理](#动态代理)
  * [JDK 动态代理](#jdk-动态代理)
    * [原理](#原理)
    * [为什么必须是接口？](#为什么必须是接口)
  * [CgLib 动态代理](#cglib-动态代理)
    * [原理](#原理-1)
  * [JDK、Cglib 对比](#jdkcglib-对比)
* [深/浅拷贝](#深浅拷贝)
  * [浅拷贝](#浅拷贝)
  * [深拷贝](#深拷贝)
    * [重写 clone() 方法](#重写-clone-方法)
    * [Serializable 序列化](#serializable-序列化)
    * [JSON 序列化](#json-序列化)
<!-- TOC -->

# 动态代理

动态代理是代理模式的一种实现，旨在在内存中动态构建代理对象，实现对目标对象的代理。

静态代理与动态代理的区别主要在： 
* 静态代理在编译时就已经实现，编译完成后代理类是一个实际的 class 文件。 
* 动态代理是在运行时动态生成，编译完成后没有生成 class 文件，而是在运行时动态生成 class 文件，并加载到 JVM 中。

## JDK 动态代理

JDK 动态代理要求目标对象必须实现接口，否则不能使用动态代理。 使用 JDK 动态代理有以下 4 个步骤：

1. 定义一个接口； 
2. 基于这个接口创建一个实现类； 
3. 自定义一个 Handler 实现 `InvocationHandler` 接口，通过重写内部的 `invoke` 方法实现逻辑增强； 
4. 使用 `Proxy` 类的静态方法 `newProxyInstance` 生成一个代理对象并调用方法。

代码实现：

```java
// 目标接口
interface Worker {
      void work();
}
// 目标类
class Programmer implements Worker {
      @Override
      public void work() {
            System.out.println("coding");
      }
}
// 目标对象功能增强
class WorkHandler implements InvocationHandler {
      private Object target;
      public WorkHandler(Object target) {
            this.target = target;
      }
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("before work...");
            Object invoke = method.invoke(target, args);   // 注意这里是 target 而非 proxy
            System.out.println("after work...");
            return invoke;
      }
}
// 测试类
// -Dsun.misc.ProxyGenerator.saveGeneratedFiles=true
public class JDKProxyTest {
      public static void main(String[] args) {
            Programmer programmer = new Programmer();
            Worker proxy = (Worker) Proxy.newProxyInstance(
                                        programmer.getClass().getClassLoader(),
                                        programmer.getClass().getInterfaces(),
                                        new WorkHandler(programmer));
            proxy.work();
      }
}
```

打印结果：

![](src/main/resources/dynamic_proxy/JDKProxy-1.png)

### 原理
Proxy 的 `newProxyInstance()` 方法大体上将动态代理分为以下 4 个步骤：

1. `checkProxyAccess`：进行参数验证； 
2. `getProxyClass0`：生成一个代理类 class 文件或者寻找已生成过的代理类的缓存； 
3. `getConstructor`：获取生成的代理类的构造方法； 
4. `newInstance`：生成实例对象，也就是最终的代理对象。

在这 4 个步骤中，第 2 步是关键。其核心是调用了 `ProxyClassFactory` 的 `apply()` 方法生成 class 文件。
在该方法中，首先会生成代理类的文件名（默认文件名是 com.sun.proxy.$Proxy0.class）：

![](src/main/resources/dynamic_proxy/JDKProxy-2.png)

获取到文件名之后，会通过 `ProxyGenerator.generateProxyClass()` 方法生成字节码文件，然后通过 `defineClass0()` 方法生成 Class 对象：

![](src/main/resources/dynamic_proxy/JDKProxy-3.png)

默认情况下生成的字节码文件不保存到本地，如果想要保留有两种方式：

第一种是在启动 VM 参数中加入：`-Dsun.misc.ProxyGenerator.saveGeneratedFiles=true`。

第二种是在代码中加入下面这一句，注意要加在生成动态代理对象之前：
`System.getProperties().put("sun.misc.ProxyGenerator.saveGeneratedFiles", "true");`。

另外需要注意这个文件生成的位置，并不是在 target 目录下，而是在项目目录下的 `org\study\java\java_base\dynamic_proxy` 中。这是因为测试代码中我们定义的 `Worker` 接口是非 `public` 的，如果我们将 `Worker` 接口定义为 `public`，那么代理类的 class 文件将会出现在 `com\sun\proxy` 目录下。

![](src/main/resources/dynamic_proxy/JDKProxy-4.png)

将代理类反编译，我们可以看到代理类继承了 `Proxy` 类并实现了 `Worker` 接口。

![](src/main/resources/dynamic_proxy/JDKProxy-5.png)

代理类主要做了下面 3 件事情：
1. 在这个类的静态代码块中，通过反射初始化了多个静态方法 `Method` 变量，除了接口中的方法还有 `equals`、`toString`、`hashCode` 这三个方法； 
2. 继承父类 `Proxy`，实例化的过程中会调用父类的构造方法，构造方法中传入的 `InvocationHandler` 对象实际上就是我们自定义的 `WorkHandler` 的实例； 
3. 实现了自定义的接口 `Worker`，并重写了 `work` 方法，方法内调用了 `InvocationHandler` 的 `invoke` 方法，也就是实际上调用了 `WorkHandler` 的 `invoke` 方法。

至此，我们可以归纳出 JDK 动态代理的执行流程：

![](src/main/resources/dynamic_proxy/JDKProxy-6.png)

### 为什么必须是接口？

我们知道在 java 中想要扩展一个类有两种方式：继承父类和实现接口。查看反编译后的代理类信息可知，代理类需要继承 `Proxy` 类，而 java 是单继承的。那么代理类就只能通过实现接口来扩展目标类。

![](src/main/resources/dynamic_proxy/JDKProxy-7.png)

在明白这一点之后，我们可以继续往下思考：代理是什么？代理是对目标的逻辑增强，而这段增强的逻辑肯定是用户自定义的。如此一来，代理类的职责就很清晰了：组合目标类和目标类的逻辑增强。

在 JDK 动态代理实现中，逻辑增强就是 `InvocationHandler` 接口。但 `InvocationHandler` 的实现类也不仅仅是逻辑增强，它的实现类已经把目标类和对目标类的逻辑增强做到了一起！

![](src/main/resources/dynamic_proxy/JDKProxy-8.png)

那么问题来了：既然 `InvocationHandler` 已经将目标类和逻辑增强耦合到了一起，代理类又干了什么呢？其实 JDK 动态代理生成的代理类并没有像我们预期的那样绑定目标类和逻辑增强，而是进行了方法转发。

![](src/main/resources/dynamic_proxy/JDKProxy-9.png)

现在我们继续思考：既然在 JDK 动态代理实现中，代理类只负责方法转发，那么代理类还有实现父类的必要吗？如果代理类可以继承父类，那么继承来的那些属性只是白白占用空间，绝对不会使用到。
因此从 JDK 动态代理的实现方式反推，我们就可以知道为什么 JDK 动态代理只能基于接口实现。

## CgLib 动态代理

Cglib（Code Generation Library）是一个高性能的代码生成包，它广泛被 AOP 框架使用，为他们提供方法的拦截。下图是 Cglib 与一些框架和语言的关系：

![](src/main/resources/dynamic_proxy/CGLib-1.png)

对于此图：

* 最底层的是字节码 Bytecode，字节码是 Java 为了保证“一次编译、到处运行”而产生的一种虚拟指令格式。例如 `iload_0`、`iconst_1`、`if_icmpne`、`dup` 等； 
* 位于字节码之上的是 ASM，这是一种直接操作字节码的框架，应用 ASM 需要对 Java 字节码、Class 结构比较熟悉； 
* 位于 ASM 之上的是 CGLIB、Groovy、BeanShell，后两种并不是 Java 体系中的内容而是脚本语言。它们通过 ASM 框架生成字节码变相执行 Java 代码，这说明在 JVM 中执行程序并不一定非要写 Java 代码 —— 只要你能生成Java字节码，JVM 并不关心字节码的来源。当然通过Java代码生成的JVM字节码是通过编译器直接生成的，算是最“正统”的 JVM 字节码； 
* 位于 CGLIB、Groovy、BeanShell 之上的就是 Hibernate、Spring AOP 这些框架了； 
* 最上层的是 Applications，即具体应用。一般都是一个 Web 项目或者本地跑一个程序。

下面使用 Cglib 编写一个通过实现父类进行代理的示例。

代码实现：

```java
// 目标类
class Dao {
    public void select() {
        System.out.println("select success");
    }
}
// 方法增强类
class DaoMethodInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        System.out.println("before invoke");
        // 这里和 InvocationHandler 不同，直接使用方法代理
        Object result = methodProxy.invokeSuper(o, objects);
        System.out.println("after invoke");
        return result;
    }
}
// 测试类
public class CGLibProxyTest {
    public static void main(String[] args) {
//        System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "D:/code/studyOfJava");
        DaoMethodInterceptor methodInterceptor = new DaoMethodInterceptor();
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Dao.class);
        enhancer.setCallback(methodInterceptor);
        // 相较于 JDK 动态代理，这里我们并不需要创建目标类对象
        Dao dao = (Dao) enhancer.create();
        dao.select();
    }
}
```

打印结果：

![](src/main/resources/dynamic_proxy/CGLib-2.png)

### 原理

同样的，想要探究 Cglib 动态代理的原理，需要将其生成的字节码文件保留到本地。在实现代码入口处增加如下代码：

`System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "D:/code/studyOfJava");`

重新执行代码后，我们就可以在对应目录下可以看到 3 个文件：

![](src/main/resources/dynamic_proxy/CGLib-3.png)

CgLib 动态代理采用了 `FastClass` 机制，分别为代理类和目标类各生成一个 `FastClass`，这个 `FastClass` 类会为代理类或目标类的方法分配一个 index（int类型）。这个 index 当做一个入参，`FastClass` 就可以直接定位要调用的方法直接进行调用，这样省去了反射调用，所以调用效率比 JDK 动态代理通过反射调用更高。

反编译代理类，我们看最关键的 `select()` 方法是如何代理的：

![](src/main/resources/dynamic_proxy/CGLib-4.png)

CgLib 生成的代理类为 `select()` 方法生了一个 `MethodProxy`。`select()` 方法被执行时，会将 `MethodProxy` 作为参数传递给用户实现的 `MethodInterceptor`，然后实现方法增强。

那么 `MethodProxy` 是如何实现方法代理的呢？在我们的测试代码中是这样使用 `MethodProxy` 的：

`Object result = methodProxy.invokeSuper(o, objects);`

查看 `invokeSuper()` 方法：

![](src/main/resources/dynamic_proxy/CGLib-5.png)

可以看到 `invokeSuper()` 方法内部就是调用 `FastClass` 的 `invoke()` 方法。而也就是说 `FastClass` 才是 CgLib 动态代理的核心。而 `FastClass` 是抽象类，`invoke()` 方法是抽象方法。那么 `FastClass` 的实现在哪儿呢？CgLib 生成的代理类文件共有 3 个，其中 1 个就是代理类的 `FastClass` 文件。反编译 CgLib 生成的代理类的 `FastClass` 文件：

![](src/main/resources/dynamic_proxy/CGLib-6.png)

可以看到 `invoke()` 方法内部就是靠入参的 int 值确定执行代理类的 `select()` 方法。那么这 int 值又是怎么来的呢？可以预见这就是 `FastClass` 的核心机制。
把注意力再次回到 CgLib 创建的代理类中为 `select()` 方法创建 `MethodProxy` 的时候：

`CGLIB$select$0$Proxy = MethodProxy.create(var1, var0, "()V", "select", "CGLIB$select$0");`

进入 `create()` 方法，可以看到该方法生成了 2 个方法签名：

![](src/main/resources/dynamic_proxy/CGLib-7.png)

接下来在 `init()` 方法中，代理类的 `FastClass` 根据方法签名找到对应的 int 值并储存起来：

![](src/main/resources/dynamic_proxy/CGLib-8.png)

而 `CGLIB$select$0()` 最终执行的又是什么呢？答案就是父类的 `select()` 方法。

```java
final void CGLIB$select$0() {
    super.select();
}
```

至此，CgLib 动态代理实现的全过程结束。

## JDK、Cglib 对比
JDK 动态代理是实现了被代理对象所实现的接口；

CgLib 是继承了被代理对象，但是不能对声明为 final 的方法进行代理。
JDK 和 CgLib 都是在运行期生成字节码，JDK 是直接写 class 字节码，CGLib 使用 ASM 框架写 class 字节码，CgLib 代理实现更复杂，生成代理类的效率比 JDK 代理低。

JDK 调用代理方法，是通过反射机制调用；CGLib 是通过 `FastClass` 机制直接调用方法，CGLib 执行效率更高。

JDK、CgLib 执行性能对比：

![](src/main/resources/dynamic_proxy/CGLib-9.png)

# 深/浅拷贝
java 中拷贝共有 3 种：引用拷贝、浅拷贝和深拷贝。

引用拷贝就是简单的赋值操作，不同的引用依然指向堆区的同一对象，这里不再赘述。如：

```java
Person person = new Person();
Person clone = person;
```

## 浅拷贝
不同于引用拷贝的是，浅拷贝会创建一个全新的对象，新对象和原对象本身没有任何关系，但是新对象的属性和原对象相同。具体区别如下：

* 如果属性是基本类型（`int`，`double`，`long`，`boolean` 等），拷贝的就是基本类型的值； 
* 如果属性是引用类型，拷贝的就是内存地址，即复制引用但不复制引用的对象。因此如果其中一个对象改变了引用的对象，就会影响到另一个对象。
* 
实现浅拷贝需要在拷贝的类上实现 `Cloneable` 接口并重写其 `clone()` 方法。

测试代码：
```java
@Getter
class Father implements Cloneable {
    private String name;
    private int age;
    public Father(String name, int age) {
        this.name = name;
        this.age = age;
    }
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

public class ShallowCopyTest {
    public static void main(String[] args) throws CloneNotSupportedException {
        Father father = new Father("fa", 40);
        Father clone = (Father) father.clone();
        System.out.println("father equals: " + (father == clone));
        System.out.println("name equals: " + (father.getName() == clone.getName()));
        System.out.println("age equals: " + (father.getAge() == clone.getAge()));
    }
}
```

打印结果：

![](src/main/resources/copy/copy-1.png)

可以看到，通过浅拷贝创建的对象，其内部持有的其他对象依然是相同的。

## 深拷贝
相较于浅拷贝，深拷贝更进一步：在对引用数据类型进行拷贝的时候，也会创建一个新的对象，并且拷贝其内部的成员变量。

在深拷贝的具体实现上，有两种方式：
1. 重写 `clone()` 方法； 
2. 序列化：有两种实现：实现 `Serializable` 接口、`JSON` 序列化。

### 重写 clone() 方法
在浅拷贝测试代码的基础上，我们在 clone() 方法内将引用数据类型赋值为新的对象，修改为如下代码：

```java
@Override
protected Object clone() throws CloneNotSupportedException {
  Father father = (Father) super.clone();
  father.name = new String(name);
  return father;
}
```

打印结果：

![](src/main/resources/copy/copy-2.png)

不难发现，通过重写 `clone()` 方法实现深拷贝的方式存在一个隐患：若引用数量/引用层级过多，会导致需要重写的 `clone()` 方法呈几何级数增长。因此我们一般通过序列化来实现深拷贝。

### Serializable 序列化
实现 `Serializable` 接口就可以让对象序列化。通过序列化，对象可以脱离 java 程序存在于别的媒介之中，例如文本、数据库。我们通过反序列化将对象载入 java 程序中，就可以得到一个全新的对象。

测试代码：

```java
@Getter
class Son implements Serializable {
    private String name;
    private int age;
    public Son(String name, int age) {
        this.name = name;
        this.age = age;
    }
    public Object deepClone() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bOS = new ByteArrayOutputStream();
        ObjectOutputStream oOS = new ObjectOutputStream(bOS);
        oOS.writeObject(this);

        ByteArrayInputStream bIS = new ByteArrayInputStream(bOS.toByteArray());
        ObjectInputStream oIS = new ObjectInputStream(bIS);
        return oIS.readObject();
    }
}

public class SerializableTest {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Son son = new Son("son", 20);
        Son clone = (Son) son.deepClone();
        System.out.println("son equals: " + (son == clone));
        System.out.println("name equals: " + (son.getName() == clone.getName()));
        System.out.println("age equals: " + (son.getAge() == clone.getAge()));
    }
}
```

打印结果：

![](src/main/resources/copy/copy-3.png)

需要注意，通过实现 `Serializable` 接口实现深拷贝，其引用数据类型的成员变量也需要实现 `Serializable` 接口。否则会报错。

不过像 `deepClone()` 里的代码重复性很强，我们可以借助 Apache Commons Lang 提供的 `SerializationUtils` 实现：

```java
Son son = new Son("son", 20);
Son clone = (Son) SerializationUtils.clone(son);
```

### JSON 序列化
JSON 序列化有多种实现，这里介绍 3 种。

Google Gson：
```java
Son son = new Son("son", 20);
Gson gson = new Gson();
Son clone = gson.fromJson(gson.toJson(son), Son.class);
```

Fasterxml Jackson（序列化类需要有无参构造）：
```java
Son son = new Son("son", 20);
ObjectMapper objectMapper = new ObjectMapper();
Son clone = objectMapper.readValue(objectMapper.writeValueAsString(son), Son.class);
```

Alibaba Fastjson（序列化类需要有无参构造、Setter 方法）：
```java
Son son = new Son("son", 20);
Son clone = JSONObject.parseObject(JSONObject.toJSONString(son)).toJavaObject(Son.class);
```

