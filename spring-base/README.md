# 循环依赖
循环依赖指一个或多个对象之间存在直接或间接的依赖关系。这种依赖关系构成一个环形调用，具体有以下 3 种形式：

![](/src/main/resources/dependency_cycle/dependency-cycle-1.png)

以情况 2 为例，测试代码如下：

```java
@Component
class Dependency1 { 
    @Autowired 
    private Dependency2 dependency2;
}
@Component
class Dependency2 { 
    @Autowired 
    private Dependency1 dependency1;
}
```

启动 Spring 项目上述代码可以正常运行。但是如果我们将依赖方式由 `@Autowired` 属性注入改为构造方法注入，那么代码就会报错循环依赖。

```java
@Component
class DependencyCycleOfConstructor2 {
    private DependencyCycleOfConstructor1 dependency1;
    public DependencyCycleOfConstructor2(DependencyCycleOfConstructor1 dependency1) {
        this.dependency1 = dependency1;
    }
}
@Component
public class DependencyCycleOfConstructor1 {
    private DependencyCycleOfConstructor2 dependency2;
    public DependencyCycleOfConstructor1(DependencyCycleOfConstructor2 dependency2) {
        this.dependency2 = dependency2;
    }
}
```

![](/src/main/resources/dependency_cycle/dependency-cycle-2.png)

此外，如果我们将 Bean 的作用域修改为 Prototype，那么代码运行时也会出现循环依赖的报错。

```java
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DependencyCycleOfPrototype2 {
  @Autowired
  private DependencyCycleOfPrototype1 dependency1;
}
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DependencyCycleOfPrototype1 {
  @Autowired
  private DependencyCycleOfPrototype2 dependency2;
}
@Component
public class DependencyCycleOfPrototype {
  @Autowired
  private DependencyCycleOfPrototype1 dependency1;
}
```

![](/src/main/resources/dependency_cycle/dependency-cycle-3.png)

由此可见，Spring 只解决了通过属性注入和通过 setter 方式注入且引用的 Bean 的作用域是 Singleton 级别的循环依赖问题。

## DI 流程

测试代码（通过 setter 注入）：

```java
@Component
class Dependency2 {
    private Dependency1 d1;
    @Autowired
    public void setD1(Dependency1 d1) {
        this.d1 = d1;
    }
}
@Component
class Dependency1 {
    private Dependency2 d2;
    @Autowired
    public void setD2(Dependency2 d2) {
        this.d2 = d2;
    }
//    @Transactional    // 放开该注解可测试 wrapIfNecessary 创建代理类分支
//    public void testProxy() {}
}
public class DependencyInjectTest {}
```

我们将断点打在下图中的位置，就可以从堆栈信息看到 Spring 进行依赖注入的过程：

![](/src/main/resources/dependency_cycle/dependency-cycle-4.png)

当 Spring 获取到所有需要创建的 `BeanDefinition` 后，开始循环创建 Bean。堆栈信息中最先创建的是 `Dependency1` 对象，我们看到 `doGetBean` 方法。该方法中首先会尝试通过 `getSingleton` 方法获取 `Dependency1` 对象：

![](/src/main/resources/dependency_cycle/dependency-cycle-5.png)

由于此时 Spring 的对象池中还没有 `Dependency1` 对象，我们直接跟进到堆栈位置的 `getSingleton` 方法，该方法中的第二个参数是 `ObjectFactory`（对象工厂）。

![](/src/main/resources/dependency_cycle/dependency-cycle-6.png)

`getSingleton` 方法内部使用 `synchronized` 关键字加锁操作：先尝试从 <font color=red>singletonObjects</font> 中获取 `Dependency1` 对象（获取到锁时可能对象已经创建成功了），如果获取不到，就将 beanName 标记为创建中，然后调用传入的 `ObjectFactory` 创建对象：

![](/src/main/resources/dependency_cycle/dependency-cycle-7.png)

根据上图可知，传入的 `ObjectFactory` 实现会调用 `createBean` 方法。当 `createBean` 方法被执行时才正式进入创建对象过程。

接下来在 `doCreateBean` 方法中，首先会创建 `Dependency1` 对象的包装类 `BeanWrapper`，从 `BeanWrapper` 中可以取得 `Dependency1` 对象，但此时的 `Dependency1` 对象还是个半成品，没有进行依赖注入。

![](/src/main/resources/dependency_cycle/dependency-cycle-8.png)

然后 Spring 会在 <font color=red>singletonFactories</font> 中存入一个回调函数 `getEarlyBeanReference()`（<font color=yellow>前提是创建对象是单例，并且对象正在创建中</font>），该方法中持有半成品 `Dependency1` 对象的引用（`getEarlyBeanReference()` 是解决循环依赖的关键，其作用我们后面再说）。

![](/src/main/resources/dependency_cycle/dependency-cycle-9.png)

接着在 `populateBean` 方法中会对 `Dependency1` 对象应用 `BeanPostProcessor`（`InstantionAwareBeanPostProcessor` 类型），Spring 默认添加的 `BeanPostProcessor` 有以下这些：

![](/src/main/resources/dependency_cycle/dependency-cycle-10.png)

上图中可以看到 Spring 默认添加 `AutowiredAnnotationBeanProcessor` 处理 `@Autowired` 注解，跟着堆栈信息继续往下走，我们就可以在 `doResolveDependency` 方法中看到从 `BeanFactory` 中获取 `Dependency1` 依赖的 `Dependency2` 对象：

![](/src/main/resources/dependency_cycle/dependency-cycle-11.png)

但是此时 Spring 的对象池中内部并没有 `Dependency2` 对象，因此又会重复创建 `Dependency1` 对象的流程：

![](/src/main/resources/dependency_cycle/dependency-cycle-12.png)

在代码执行到为 `Dependency2` 对象注入 `Dependency1` 对象时，我们会第三次来到 `doGetBean` 方法，而此时也会再次调用 `getSingleton` 方法：

![](/src/main/resources/dependency_cycle/dependency-cycle-13.png)

此时我们跟进到 `getSingleton` 方法内部，代码最终会走到从 <font color=red>singletonFactories</font> 中获取可以得到 `Dependency1` 对象的 `ObjectFactory`，然后通过 <font color=red>singletonFactories</font> 的 `getObject()` 方法获取 `Dependency1` 对象：

![](/src/main/resources/dependency_cycle/dependency-cycle-14.png)

而我们在之前创建 `Dependency1` 对象的流程中，向 <font color=red>singletonFactories</font> 中存放了一个回调方法 `getEarlyBeanReference()`，该方法内部也是为 bean 执行 `BeanPostProcessor`（`SmartInstantiationAwareBeanProcessor` 类型）。

![](/src/main/resources/dependency_cycle/dependency-cycle-15.png)

继续查看该接口的实现类，来到 `AbstractAutoProxyCreator` 类的 `getEarlyBeanReference` 方法实现中：

![](/src/main/resources/dependency_cycle/dependency-cycle-16.png)

该方法内部会执行一个很重要的方法 `wrapIfNecessary()`：

![](/src/main/resources/dependency_cycle/dependency-cycle-17.png)

`wrapIfNecessary` 方法内部会判断对象是否需要代理，如果需要代理，就会创建代理对象返回；如果不需要，则返回原始对象。

![](/src/main/resources/dependency_cycle/dependency-cycle-18.png)

接着 `Dependency1` 对象会被放进 <font color=red>earlySingletonObjects</font>，而 <font color=red>singletonFactories</font> 中的回调方法则会被移除。而此时 `Dependency2` 对象也获取到了 `Dependency1` 对象并完成注入。

接着我们把目光再次回到创建 `Dependency1` 对象时执行的 `doGetBean` 方法，此时 `Dependency1` 对象已经创建成功，继续往下执行 `addSingleton` 方法，该方法会将 `Dependency1` 对象放到 <font color=red>singletonObjects</font> 中，并从 <font color=red>earlySingletonObjects</font> 中移除。

![](/src/main/resources/dependency_cycle/dependency-cycle-19.png)

至此，`Dependency1` 和 `Dependency2` 对象都创建成功。