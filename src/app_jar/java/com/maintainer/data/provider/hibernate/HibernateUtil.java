package com.maintainer.data.provider.hibernate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;

import com.maintainer.util.Utils;

public final class HibernateUtil {

    private static volatile SessionFactory sessionFactory;
    private static final Logger log = Logger.getLogger(HibernateUtil.class.getName());
    public static final ThreadLocal<Session> threadSession = new ThreadLocal<Session>();
    public static final ThreadLocal<Transaction> threadTransaction = new ThreadLocal<Transaction>();
    private static final Object FACTORY_LOCK = new Object();

    private HibernateUtil() {}

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (FACTORY_LOCK) {
                if (sessionFactory == null) {
                    createSessionFactory();
                }
            }
        }

        // Alternatively, you could look up in JNDI here
        return sessionFactory;
    }

    public static Configuration createConfiguration() {
        log.info("Creating new session factory");
        File file = Utils.getDatabaseConfigurationFile();
        Properties properties = Utils.getProperties(file);

        try {
            Configuration config = new AnnotationConfiguration();

            String packageName = (String) properties.get("hibernate.model.package");
            Class<?>[] classes = Utils.getClassesInPackage(packageName, null);

            for (Class<?> clazz : classes) {
                config.addAnnotatedClass(clazz);
            }

            config
                    .mergeProperties(properties)
                    .configure()
                    .setNamingStrategy(new CapitalNamingStrategy())
                    .setInterceptor(new AuditInterceptor());

            return config;
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static void createSessionFactory() {
        sessionFactory = createConfiguration().buildSessionFactory();
    }

    public static Session getCurrentSession() throws HibernateException {
        return getCurrentSession(false);
    }

    public static Session getCurrentSession(boolean forceNewConnection) throws HibernateException {
        Session s = null;
        if (!forceNewConnection) {
            s = threadSession.get();
        }

        // Open a new Session, if this Thread has none yet
        if (s == null) {
            s = getSessionFactory().openSession();
            s.setFlushMode(FlushMode.COMMIT);
            threadSession.set(s);
        }
        return s;
    }

    public static void closeSession() throws HibernateException {
        Session s = threadSession.get();
        threadSession.set(null);
        if (s != null && (s.isOpen() || s.isConnected())) {
            try { s.flush(); } catch (Exception e) {}
            try { s.close(); } catch (Exception e) {}
            try { s.disconnect(); } catch (Exception e) {}
        }
    }

    public static Transaction getTransaction() {
        return threadTransaction.get();
    }

    public static void beginTransaction() {
        Transaction tx = threadTransaction.get();
        if (tx == null) {
            tx = getCurrentSession().beginTransaction();
            threadTransaction.set(tx);
        }
    }

    public static boolean isInTransaction() {
        return threadTransaction.get() != null;
    }

    public static void commitTransaction(boolean closeSession) throws Exception {
        Transaction tx = threadTransaction.get();
        try {
            if (tx != null && !tx.wasCommitted() && !tx.wasRolledBack()) {
                tx.commit();
            } else {
                log.warn("Trying to commit transaction that was already committed");
            }
            threadTransaction.set(null);
        } catch (Exception ex) {
            log.error("Could not commit transaction trying to rollback. " + ex);
            rollbackTransaction();
            throw ex;
        } finally {
            if (closeSession) {
                closeSession();
            }
        }
    }

    public static void rollbackTransaction() {
        log.info("Rolling back current transaction.");
        Transaction tx = threadTransaction.get();
        threadTransaction.set(null);
        if (tx != null && !tx.wasCommitted() && !tx.wasRolledBack()) {
            log.info("Rolling back transaction.");
            tx.rollback();
        }
    }

    public static void initializeAppServerLogging() {
        //PropertyConfigurator.configure("etc/log4j.properties");
        log.info("Logging re-initialized.");
    }

    public static void shutdown() {
        // Close caches and connection pools
        getSessionFactory().close();
    }

    /**
     * Wrap a jpa or hibernate result List with generics for compile-time happiness.
     *
     * @param list
     * @param t
     * @return Given untyped list as list of specified type.
     */
    public static <T> List<T> asListOfType (List<T> list, Class<T> t) {
        return new ArrayList<T>(list);
    }
}

