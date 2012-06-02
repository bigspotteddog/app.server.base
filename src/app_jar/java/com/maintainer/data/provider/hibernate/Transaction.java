package com.maintainer.data.provider.hibernate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.JDBCConnectionException;

public final class Transaction {

    private static final Logger log = Logger.getLogger(Transaction.class.getName());

    private Transaction() {
    }

    public static Object execute (String user, Callable<?> c) throws Exception {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            host = "localhost";
        }
        ThreadLocalInfo.setInfo(user, host, host, DateUtil.getNewDate());
        return execute(c);
    }

    public static Object execute (String user, String server, String client, Date date, Callable<?> c) throws Exception {
        ThreadLocalInfo.setInfo(user, server, client, date);
        return execute(c);
    }

    private static Object execute(Callable<?> c) throws Exception {
        Object result;
        try {
            log.fine("Starting transaction from Callable " + c.getClass().getName() + " on Thread " + Thread.currentThread().getId());
            HibernateUtil.beginTransaction();
            result = c.call();
            log.fine("Committing the database transaction from Callable " + c.getClass().getName() + " on Thread " + Thread.currentThread().getId());
            HibernateUtil.commitTransaction(true);
        } catch(GenericJDBCException e) {
            log.severe("Could not commit transaction. JDBC Exception" + e);
            try {
                HibernateUtil.rollbackTransaction();
            } catch (Exception ex) {
                log.severe ("Rollback failed! " + ex);
            }
            throw e;
        } catch(JDBCConnectionException e) {
            log.severe("Could not commit transaction. JDBC Connection Exception" + e);
            try {
                HibernateUtil.rollbackTransaction();
            } catch (Exception ex) {
                log.severe ("Rollback failed! " + ex);
            }
            throw e;
        } catch (Exception ex) {
            log.severe("Could not commit transaction. " + ex);
            try {
                HibernateUtil.rollbackTransaction();
            } catch (Exception e) {
                log.severe ("Rollback failed! " + e);
            }
            throw ex;
        }
        return result;
    }

}
