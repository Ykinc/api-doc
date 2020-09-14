package com.power.doc.utils;

import org.apache.commons.lang3.StringEscapeUtils;
import org.beetl.core.Context;
import org.beetl.core.Function;

/**
 * @author : KINC
 */
public class EscapeXml implements Function {
    @Override
    public String call(Object[] objects, Context context) {
        if(objects==null){
            return "";
        }
        Object o = objects[0];
        return o==null?"":StringEscapeUtils.escapeXml(o.toString());
    }
}
