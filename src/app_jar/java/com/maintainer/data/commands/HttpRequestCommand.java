package com.maintainer.data.commands;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.maintainer.util.Utils;

public class HttpRequestCommand extends AbstractCommand<HttpResponseModel> {
    private static final Logger log = Logger.getLogger(HttpRequestCommand.class.getName());

    public enum Method {
        GET, POST, PUT, DELETE, HEAD
    }

    private HttpServletRequest req;

    private final String path;
    private final Method method;
    private Map<String, String> headers;
    private String body;

    public HttpRequestCommand(String path) {
        this.path = path;
        this.method = Method.GET;
    }

    public HttpRequestCommand(String path, Method method) {
        this.path = path;
        this.method = method;
    }

    public HttpRequestCommand(String path, Method method, Map<String, String> headers) {
        this.path = path;
        this.method = method;
        this.headers = headers;
    }

    public HttpRequestCommand(String path, Method method, HttpServletRequest req) throws IOException {
        this.path = path;
        this.method = method;
        this.headers = getHeaders(req);
    }

    public HttpRequestCommand(String path, Method method, Map<String, String> headers, String body) {
        this.path = path;
        this.method = method;
        this.headers = headers;
        this.body = body;
    }

    public HttpRequestCommand(String root, HttpServletRequest req) throws IOException {
        this.req = req;
        this.path = getPath(root, req);
        this.method = Method.valueOf(req.getMethod());
        this.headers = getHeaders(req);
        this.body = Utils.convertStreamToString(req.getInputStream());
    }

    public HttpRequestCommand(String root, HttpServletRequest req, String body) throws IOException {
        this.req = req;
        this.path = getPath(root, req);
        this.method = Method.valueOf(req.getMethod());
        this.headers = getHeaders(req);
        this.body = body;
    }

    @Override
    public HttpResponseModel execute() throws Exception {
        return request(path, method);
    }

    private HttpResponseModel request(String path, Method method) throws IOException {
        log.info("Processing path: " + path);

        HttpResponseModel response = new HttpResponseModel();
        OutputStream out = response.getOutputStream();

        URL url;
        HttpURLConnection connection = null;
        InputStream is = null;

        try {
            url = new URL(path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method.name());
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);

            if (headers != null) {
                for (Entry<String, String> e : headers.entrySet()) {
                    String headerName = e.getKey();
                    String headerValue = e.getValue();

                    if (headerValue != null) {
                        connection.setRequestProperty(headerName, headerValue);
                    }
                }
            }

            connection.setUseCaches (false);

            if (body != null && body.length() > 0) {
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
                wr.writeBytes(body);
                wr.flush();
                wr.close();
                connection.disconnect();
            }

            int responseCode = -1;

            try {
                responseCode = connection.getResponseCode();
            } catch (IOException e) {
                responseCode = connection.getResponseCode();
            }

            log.info("Response Code: " + responseCode);

            Map<String, List<String>> responseHeaders = connection.getHeaderFields();
            log.info(responseHeaders.toString());

            for (java.util.Map.Entry<String, List<String>> e : responseHeaders.entrySet()) {
                String key = e.getKey();
                if (key != null) {
                    List<String> value = e.getValue();
                    if (!value.isEmpty()) {
                        String v = value.iterator().next();
                        if (responseCode == 303 && key.toLowerCase().equals("location") && req != null) {
                            URL url2 = new URL(v);
                            String path2 = url2.getPath();
                            String query2 = url2.getQuery();

                            String scheme = req.getScheme();
                            String server = req.getServerName();
                            int port = req.getServerPort();

                            StringBuilder buf = new StringBuilder(scheme)
                            .append("://")
                            .append(server);

                            if (port != 80) {
                                buf
                                .append(':')
                                .append(port);
                            }

                            if (path2 != null) {
                                buf.append(path2);
                            }

                            if (query2 != null) {
                                buf.append('?').append(query2);
                            }

                            v = buf.toString();
                        }
                        response.setHeader(key, v);
                    }
                }
            }

            if (responseCode >= 400) {
                is = connection.getErrorStream();
            } else {
                is = connection.getInputStream();
            }

            int n = 0;
            byte[] b = new byte[1024];
            while ((n = is.read(b)) > 0) {
                out.write(b, 0, n);
            }

            if (responseCode == 303) {
                responseCode = 401;
            }
            log.info("Send Response Code: " + responseCode);
            response.setStatus(responseCode);

        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                is.close();
            }

            if (out != null) {
                out.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }

        return response;
    }

    public String getBody() {
        return body;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getHeaders(HttpServletRequest req) {
        Map<String, String> requestHeaders = new LinkedHashMap<String, String>();
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = req.getHeader(headerName);
            requestHeaders.put(headerName, headerValue);
        }
        return requestHeaders;
    }

    private String getPath(String root, HttpServletRequest req) {
        String path2 = root;
        String query = req.getQueryString();

        if (query != null) {
            path2 += '?' + query;
        }
        return path2;
    }
}
