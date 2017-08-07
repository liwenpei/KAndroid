package me.keeganlee.kandroid.exception;

import me.keeganlee.kandroid.tools.LogUtil;

/**
 * Created by Administrator on 2017/7/10 0010.
 */

public class KAndroidException extends RuntimeException{
    private static final long serialVersionUID = 533854535537735838L;

    public KAndroidException(String message) {
        super(message);
        LogUtil.error(message);
    }
}
