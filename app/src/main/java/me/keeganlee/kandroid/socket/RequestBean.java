package me.keeganlee.kandroid.socket;

import org.json.JSONObject;

import java.nio.ByteBuffer;

import me.keeganlee.kandroid.tools.ConvertUtil;

public class RequestBean {
    private int requestCode;
    private int type;
    private ByteBuffer data;
    private int expand;//扩展字段
    private JSONObject jsonObject;

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public ByteBuffer getData() {
        return data;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }

    public int getExpand() {
        return expand;
    }

    public void setExpand(int expand) {
        this.expand = expand;
    }

    public JSONObject getJsonData() {
        try {
            if (jsonObject != null) {
                return jsonObject;
            }
            String jsonStr = ConvertUtil.byteTOString(this.data.array());
            return new JSONObject(jsonStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
