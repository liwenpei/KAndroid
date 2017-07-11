package me.keeganlee.kandroid.bean;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/7/3 0003.
 */

public class TestBean extends RootBean {
    private String age;
    private String name;

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
