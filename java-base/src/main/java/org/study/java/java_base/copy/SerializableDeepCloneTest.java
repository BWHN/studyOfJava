package org.study.java.java_base.copy;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.SerializationUtils;

import java.io.*;

@Getter
//@NoArgsConstructor
//@Setter
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

public class SerializableDeepCloneTest {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Son son = new Son("son", 20);
        Son clone = (Son) son.deepClone();
//        clone = ApacheCommonTest(son);
        System.out.println("son equals: " + (son == clone));
        System.out.println("name equals: " + (son.getName() == clone.getName()));
        System.out.println("age equals: " + (son.getAge() == clone.getAge()));
    }

    private static Son ApacheCommonTest (Son son) {
        return (Son) SerializationUtils.clone(son);
    }

}
