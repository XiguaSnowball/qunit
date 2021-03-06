package com.qunar.base.qunit.util;

import org.apache.commons.lang.StringUtils;

/**
 * User: zhaohuiyu
 * Date: 5/10/13
 * Time: 3:08 PM
 */
public class Util {
    public static Boolean isEmpty(Object value) {
        if (value == null) return true;
        if (StringUtils.isBlank(value.toString())) return true;
        return false;
    }

    public static boolean isJson(Object value) {
        if (!(value instanceof String)) return false;
        String json = value.toString();
        return json.startsWith("{") && json.endsWith("}");
    }
}
