<!-- TOC -->
* [循环依赖](#循环依赖)
  * [DI 流程](#di-流程)
  * [三级缓存](#三级缓存)
  * [为什么是三级缓存？](#为什么是三级缓存)
<!-- TOC -->

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

我们把目光再次回到创建 `Dependency1` 对象时执行的 `doGetBean` 方法，此时 `Dependency1` 对象已经创建成功，继续往下执行 `addSingleton` 方法，该方法会将 `Dependency1` 对象放到 <font color=red>singletonObjects</font> 中，并从 <font color=red>earlySingletonObjects</font> 中移除。

![](/src/main/resources/dependency_cycle/dependency-cycle-19.png)

至此，`Dependency1` 和 `Dependency2` 对象都创建成功。

## 三级缓存
上面的 DI 流程一节中，有三个代码重点标红，分别是 <font color=red>singletonObjects</font>、<font color=red>earlySingletonObjects</font>、<font color=red>singletonFactories</font>。它们就是 Spring 的三级缓存，是 Spring 解决循环依赖的关键。它们的作用分别是：
- 一级缓存：singletonObjects，用于保存实例化、注入、初始化完成的 bean 实例； 
- 二级缓存：earlySingletonObjects，用于保存实例化完成的 bean 实例； 
- 三级缓存：singletonFactories，用于保存 bean 创建工厂，以便后面有机会创建代理对象。

三级缓存的具体使用方式如下：

![](/src/main/resources/dependency_cycle/dependency-cycle-20.png)

整个执行逻辑如下：
1. 在第一层中，先去获取 A 的 Bean，发现没有就准备去创建一个，然后将 A 的代理工厂放入“三级缓存”（这个 A 其实是一个半成品，还没有对里面的属性进行注入），但是 A 依赖 B 的创建，就必须先去创建 B； 
2. 在第二层中，准备创建 B，发现 B 又依赖 A，需要先去创建 A； 
3. 在第三层中，去创建 A，因为第一层已经创建了 A 的代理工厂，直接从“三级缓存”中拿到 A 的代理工厂，获取 A 的代理对象，放入“二级缓存”，并清除“三级缓存”； 
4. 回到第二层，现在有了 A 的代理对象，对 A 的依赖完美解决（这里的 A 仍然是个半成品），B 初始化成功； 
5. 回到第一层，现在 B 初始化成功，完成 A 对象的属性注入，然后再填充 A 的其它属性，以及 A 的其它步骤（包括 AOP），完成对 A 完整的初始化功能（这里的 A 才是完整的 Bean）。 
6. 将 A 放入“一级缓存”。

## 为什么是三级缓存？

伟大的毛主席曾说过：用发展的眼光看待问题。当问及为什么 Spring 要用三级缓存解决循环依赖问题时，我们不能只盯着三级缓存看，也要兼顾 Spring 的发展脉络。综合二者来看，<font color=yellow>Spring 是为了解决循环依赖中出现 aop 代理问题，同时兼顾代码的可拓展性才这么设计</font>。

如果只是为了解决循环依赖，只需要一级缓存就够了。对比 SpringFramework-1.0 的代码，早期的 Spring 在 AbstractBeanFactory 中只用了一个叫 singletonCache 的一级缓存（也就是单例池）来解决循环依赖，同时，在AbstractAutowireCapableBeanFactory 中，实例化 Bean 之后，就先把引用存放到一级缓存中，通过这个操作来解决循环依赖问题。

于此同时 SpringBean 生命周期的基调也已经定下来了：也就是实例化 Bean，对 Bean 进行属性注入，调用 aware 接口回调，调用 BeanPostProcessor 的前置处理方法，调用 Bean 初始化方法，调用 BeanPostProcessor 后置处理方法这一流程。并且还没有 getEarlyBeanReference 这个方法，同时 Spring 中的 aop 包也已经存在 springframework1.0 中。

那么大胆猜测一下，早期的 Spring 中已经有 aop 代理，但是还没有出现或者还没有修复循环依赖中出现 aop 代理的问题。那么要解决循环依赖中出现 aop 代理问题引起的依赖注入引用不一致问题，可以实例化后通过 getEarlyBeanReference 把 Bean 引用提前暴露，放到一级缓存当中，依然只需要一级缓存就可以了。<font color=yellow>但是从 Spring 设计者的角度出发，如果仅仅为了解决由于 aop 代理引起的循环依赖问题，就改变了所有 Bean 的生命周期流程，那肯定是不可以的。修复这个问题把影响限制在只有循环依赖出现的时候，而不是每个代理对象都要提前暴露，这样子对于框架代码的影响才是最小的。所以最少需要用二级缓存解决，实例化之后先存放一个对象工厂到二级缓存，只有出现循环依赖时才需要提前暴露引用</font>。

那么此时的一级缓存就同时存放完整 Bean 和提前暴露引用的不完整 Bean，如果在 Bean 的生命周期中想要对提前暴露引用的 Bean 进行一些处理，二级缓存就会无法支持这种场景。因此需要三级缓存来处理：一级缓存保存完整 Bean，二级缓存保存提前暴露的不完整 Bean，三级缓存存放能够提前暴露 Bean 引用的对象工厂。那么要对提前暴露引用的 Bean 进行特殊处理的场景就能够实现了，只需要在 Bean 后置处理器之后，在放入一级缓存之前，调用 getSingleton 方法，allowEarlyReference 为 false，查询二级缓存中是否存在对应的 Bean，如果不为 null，说明 Bean 提前暴露了，从三级缓存移动到了二级缓存，就需要进行处理。
