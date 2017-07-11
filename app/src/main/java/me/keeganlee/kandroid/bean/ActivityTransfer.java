package me.keeganlee.kandroid.bean;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/7/3 0003.
 */

public class ActivityTransfer implements Serializable{
    private static final long serialVersionUID = 2L;
    private String  id;
    private String from;
    private String to;
    private String finish;
    private String checkRunEnd;
    private String method;
    private String requestCode;

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getCheckRunEnd() {
        return checkRunEnd;
    }

    public void setCheckRunEnd(String checkRunEnd) {
        this.checkRunEnd = checkRunEnd;
    }

    public String getFinish() {
        return finish;
    }

    public void setFinish(String finish) {
        this.finish = finish;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
