package org.study.java.java_base.copy;

import lombok.Getter;

import java.io.*;

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
