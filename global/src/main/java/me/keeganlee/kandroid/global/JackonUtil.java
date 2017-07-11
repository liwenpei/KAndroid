package me.keeganlee.kandroid.global;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

public class JackonUtil {
    private static ObjectMapper objectMapper = null;

    public JackonUtil() {
    }

    public static synchronized ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        return objectMapper;
    }

    public static String serialize(Object obj) {
        try {
            return getObjectMapper().writeValueAsString(obj);
        } catch (Exception var2) {
            return null;
        }
    }

    public static <T> T readValue(String content, Class<T> valueType) {
        try {
            return getObjectMapper().readValue(content, valueType);
        } catch (Exception var3) {
            return null;
        }
    }

    public static <T> List<T> readListValue(String content, Class<T> clazz) {
        try {
            if (content != null && !content.trim().equals("")) {
                JavaType javaType = getCollectionType(ArrayList.class, new Class[] {clazz});
                List e = (List) getObjectMapper().readValue(content, javaType);
                return (List) (e == null ? new ArrayList() : e);
            } else {
                return new ArrayList();
            }
        } catch (Exception var4) {
            return null;
        }
    }

    public static JavaType getCollectionType(Class<?> collectionClass, Class... elementClasses) {
        return getObjectMapper().getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }
}

