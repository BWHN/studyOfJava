package org.study.java.java_base.copy;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

public class JsonDeepCloneTest {

    public static void main(String[] args) throws JsonProcessingException {
        Son son = new Son("son",20);
        Son clone;
        clone = GsonTest(son);
//        clone = JacksonTest(son);
//        clone = FastjsonTest(son);
        System.out.println("son equals: " + (son == clone));
        System.out.println("name equals: " + (son.getName() == clone.getName()));
        System.out.println("age equals: " + (son.getAge() == clone.getAge()));
    }

    private static Son GsonTest(Son son) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(son), Son.class);
    }

    // 成功调用该方法，需要为 Son 增加 @NoArgsConstructor 无参构造
    private static Son JacksonTest(Son son) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(objectMapper.writeValueAsString(son),Son.class);
    }

    // 成功调用该方法，需要为 Son 增加 @NoArgsConstructor （能创建）和 @Setter 注解（能赋值）
    private static Son FastjsonTest(Son son) {
        return JSONObject.parseObject(JSONObject.toJSONString(son)).toJavaObject(Son.class);
    }

}
