package org.study.java.spring_base.dependency_cycle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//@Component
class DependencyCycleOfConstructor2 {
    private DependencyCycleOfConstructor1 dependency1;
    public DependencyCycleOfConstructor2(DependencyCycleOfConstructor1 dependency1) {
        this.dependency1 = dependency1;
    }
}
//@Component
public class DependencyCycleOfConstructor1 {
    private DependencyCycleOfConstructor2 dependency2;
    public DependencyCycleOfConstructor1(DependencyCycleOfConstructor2 dependency2) {
        this.dependency2 = dependency2;
    }
}
