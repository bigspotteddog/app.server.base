package com.maintainer.data.provider.hibernate;

import java.util.Date;
import java.util.Locale;


/**
 * Associate a logged-in user to the current thread, or look it up.
 */
public class ThreadLocalInfo {

    private static final ThreadLocal<ThreadLocalInfo> info = new ThreadLocal<ThreadLocalInfo>() {
        @Override
        protected ThreadLocalInfo initialValue() {
            return new ThreadLocalInfo();
        }
    };

    private String user;
    private String server;
    private String client;
    private Date date = new Date();
    private Locale locale = Locale.ENGLISH;

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public Date getDate() {
        return DateUtil.copyDate(date);
    }

    public void setDate(Date date) {
        this.date = DateUtil.copyDate(date);
    }

    public static ThreadLocalInfo getInfo() {
        ThreadLocalInfo callerInfo = info.get();
        if (callerInfo == null) {
            throw new IllegalStateException("Footprint caller information has not been set, and should have been already.  This is a bug and should be reported.");
        }
        return callerInfo;
    }

    public static ThreadLocalInfo setInfo(String user, String server, String client, Date date) {
        ThreadLocalInfo callerInfo = getThreadInfo();
        callerInfo.user = user;
        callerInfo.server = server;
        callerInfo.client = client;
        callerInfo.date = date;
        return callerInfo;
    }

    private static ThreadLocalInfo getThreadInfo() {
        ThreadLocalInfo callerInfo = info.get();
        if (callerInfo == null) {
            callerInfo = new ThreadLocalInfo();
            info.set(callerInfo);
        }
        return callerInfo;
    }

    public static void clearInfo() {
        info.set(null);
    }

    public static Footprint getFootprint() {
        ThreadLocalInfo callerInfo = getInfo();
        return new Footprint(callerInfo.date,
                callerInfo.user,
                callerInfo.server,
                callerInfo.client);
    }
}
