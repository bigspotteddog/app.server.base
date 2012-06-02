package com.maintainer.data.provider.hibernate;

import java.util.Date;

public class DateUtil {
    public static Date getNewDate() {
        return new Date();
    }

    public static Date copyDate(Date date) {
        if(date == null) return null;
        return new Date(date.getTime());
    }
}
