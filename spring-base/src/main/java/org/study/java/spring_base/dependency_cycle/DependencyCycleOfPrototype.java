package org.study.java.spring_base.dependency_cycle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

//@Component  // 同时放开该注解和下面2个注解，会出现循环依赖
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DependencyCycleOfPrototype2 {
  @Autowired
  private DependencyCycleOfPrototype1 dependency1;
}
//@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DependencyCycleOfPrototype1 {
  @Autowired
  private DependencyCycleOfPrototype2 dependency2;
}
//@Component
public class DependencyCycleOfPrototype {
  @Autowired
  private DependencyCycleOfPrototype1 dependency1;
}