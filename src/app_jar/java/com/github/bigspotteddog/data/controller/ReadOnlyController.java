package com.github.bigspotteddog.data.controller;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.restlet.Client;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public abstract class ReadOnlyController extends ServerResource {
    private static final Logger log = Logger.getLogger(ReadOnlyController.class.getName());

    protected abstract Properties getProperties();

    protected abstract void setProperties(Properties properties);

    protected abstract String getServer();

    protected abstract void setServer(String server);

    protected abstract Integer getPort();

    protected abstract void setPort(Integer port);

    protected abstract String getPortPropertyName();

    protected abstract String getServerPropertyName();

    protected abstract String getCacheSecondsPropertyName();

    protected abstract String getIsCachingPropertyName();

    protected abstract boolean isCaching();

    protected abstract void setCaching(boolean caching);

    protected abstract Cache<String, String> getCache();

    protected abstract void setCache(Cache<String, String> cache);

    protected abstract long getCacheSeconds();

    protected abstract void setCacheSeconds(long seconds);

    private static final String PROPERTIES_PATH = "etc/appserver.properties";
    private static final String SYSPROP_PATH = "app.configuration";
    private static final String ORG_RESTLET_HTTP_HEADERS = "org.restlet.http.headers";
    private static Client client;
    private final Object lock = new Object();

    public ReadOnlyController() {
        initialize();
    }

    @Override
    public Representation handle() {
        Request request = getRequest();
        Response response = getResponse();

        String path = request.getResourceRef().toString();
        if (!path.startsWith("http")) {
            path = request.getRootRef().toString() + path;
        }

        String text = getFromCache(path);

        Status status = Status.SUCCESS_OK;

        if (text == null) {
            log.debug("not cached: " + path);
            Reference resourceRef = new Reference(path);
            resourceRef.setHostDomain(getServer());
            resourceRef.setHostPort(getPort());
            resourceRef.setScheme("http");
            Request request2 = new Request(request.getMethod(), resourceRef);
            request2.getAttributes().put(ORG_RESTLET_HTTP_HEADERS,
                    request.getAttributes().get(ORG_RESTLET_HTTP_HEADERS));
            request2.setEntity(request.getEntity());

            log.debug("read-only: " + request2.toString());
            log.debug("header: " + request2.getAttributes().get(ORG_RESTLET_HTTP_HEADERS));
            Response response2 = getClient().handle(request2);

            status = response2.getStatus();
            Object headers = response2.getAttributes().get(ORG_RESTLET_HTTP_HEADERS);
            if (headers != null) {
                response.getAttributes().put(ORG_RESTLET_HTTP_HEADERS, headers);
            }

            Representation entity = response2.getEntity();

            if (isCaching() && entity != null && entity.getSize() != 0
                    && entity.getMediaType().equals(MediaType.APPLICATION_JSON)) {
                try {
                    text = entity.getText();
                    response.setEntity(new JsonRepresentation(text));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (response2.getStatus().getCode() == Status.SUCCESS_OK.getCode()) {
                    log.debug("caching with " + response2.getStatus().getCode() + ", " + path);
                    putInCache(path, text);
                }
            } else {
                response.setEntity(entity);
            }
        } else {
            log.debug("cached: " + path);

            Representation entity = new JsonRepresentation(text);
            response.setEntity(entity);
        }

        if (status.getCode() >= 1000) {
            status = new Status(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, status.getReasonPhrase());
        }
        response.setStatus(status);

        return response.getEntity();
    }

    protected void putInCache(String path, String entity) {
        if (isCaching()) {
            getCache().put(path, entity);
        }
    }

    protected String getFromCache(String path) {
        if (isCaching() && path != null) {
            return getCache().getIfPresent(path);
        }
        return null;
    }

    private Client getClient() {
        if (client == null) {
            client = new Client(Protocol.HTTP);
            try {
                client.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return client;
    }

    private void initialize() {
        String server = getServer();
        if (server == null) {
            synchronized (lock) {
                if (server == null) {
                    setProperties(getApplicationServerProperties());
                    setServer((String) getProperties().get(getServerPropertyName()));

                    String portString = (String) getProperties().get(getPortPropertyName());
                    if (portString != null) {
                        setPort(Integer.parseInt(portString));
                    }

                    String isCachingString = (String) getProperties().get(getIsCachingPropertyName());
                    if (isCachingString != null) {
                        setCaching(Boolean.parseBoolean(isCachingString));
                    }

                    if (isCaching()) {
                        String cacheSecondsString = (String) getProperties().get(getCacheSecondsPropertyName());
                        if (cacheSecondsString != null) {
                            setCacheSeconds(Long.parseLong(cacheSecondsString));
                        } else {
                            setCacheSeconds(60);
                        }

                        Cache<String, String> cache = CacheBuilder.newBuilder()
                                .expireAfterWrite(getCacheSeconds(), TimeUnit.SECONDS).build();
                        setCache(cache);
                    }
                }
            }
        }
    }

    public static Properties getApplicationServerProperties() {
        return getProperties(getApplicationConfigurationFile());
    }

    private static File getApplicationConfigurationFile() {
        String path = System.getProperty(SYSPROP_PATH, PROPERTIES_PATH);
        return new File(path);
    }

    public static Properties getProperties(File file) throws ExceptionInInitializerError {
        Properties properties = new Properties();
        Reader reader = null;
        try {
            reader = new FileReader(file);
            properties.load(reader);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load " + file.getAbsolutePath() + ": " + e);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to decrypt " + file.getAbsolutePath() + ": " + e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return properties;
    }
}
