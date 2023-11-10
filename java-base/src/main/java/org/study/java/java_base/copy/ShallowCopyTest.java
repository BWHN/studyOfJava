package org.study.java.java_base.copy;

import lombok.Getter;

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
//  @Override
//  protected Object clone() throws CloneNotSupportedException {
//    Father father = (Father) super.clone();
//    father.name = new String(name);
//    return father;
//  }
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
