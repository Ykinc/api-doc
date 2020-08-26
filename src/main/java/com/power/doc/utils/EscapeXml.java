package com.power.doc.utils;

import org.apache.commons.lang3.StringEscapeUtils;
import org.beetl.core.Context;
import org.beetl.core.Function;

/**
 * @author : KINC
 * @date : 2020/8/26 16:46
 */
public class EscapeXml implements Function {
    @Override
    public String call(Object[] objects, Context context) {
        Object o = objects[0];
        return StringEscapeUtils.escapeXml(o.toString());
    }
}
