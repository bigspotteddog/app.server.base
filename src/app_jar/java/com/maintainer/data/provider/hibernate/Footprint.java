package com.maintainer.data.provider.hibernate;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;


@Embeddable
public class Footprint implements Serializable {

    private static final long serialVersionUID = 1285931837710276984L;

    @Temporal(value = TemporalType.TIMESTAMP)
    Date dateCreated;

    @Column(nullable = false, length = 50)
    String user;

    @Column(nullable = false, length = 50)
    String server;

    @Column(nullable = false, length = 50)
    String client;

    public Footprint(Date date, String user, String server, String client) {
        super();
        this.dateCreated = DateUtil.copyDate(date);
        this.user = user;
        this.server = server;
        this.client = client;
    }

    protected Footprint() { }

    public Date getDate() {
        if(dateCreated == null) dateCreated = DateUtil.getNewDate();
        return DateUtil.copyDate(dateCreated);
    }

    public void setDate(Date date) {
        this.dateCreated = DateUtil.copyDate(date);
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

    @Override
    public String toString() {
        return "User: " + getUser() + "\t Date: " + getDate();
    }
}