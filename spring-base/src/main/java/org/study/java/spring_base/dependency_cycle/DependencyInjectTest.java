package org.study.java.spring_base.dependency_cycle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
//    @Transactional
//    public void testProxy() {}
}
public class DependencyInjectTest {}
