package com.maintainer.data.router;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.maintainer.data.model.ThreadLocalInfo;
import com.maintainer.data.model.User;
import com.maintainer.util.Utils;

@SuppressWarnings("serial")
public class MyHttpServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(MyHttpServlet.class.getName());

    public static final int BAD_REQUEST = 400;
    public static final String APPLICATION_JSON = "application/json; charset=UTF-8";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ThreadLocalInfo.setInfo(req);
        super.service(req, resp);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            get(req, resp);
        } catch (final Exception e) {
            sendError(resp, e);
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            post(req, resp);
        } catch (final Exception e) {
            sendError(resp, e);
        }
    }

    @Override
    protected void doPut(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            put(req, resp);
        } catch (final Exception e) {
            sendError(resp, e);
        }
    }

    @Override
    protected void doDelete(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            delete(req, resp);
        } catch (final Exception e) {
            sendError(resp, e);
        }
    }

    protected void get(final HttpServletRequest req, final HttpServletResponse resp) throws Exception {}
    protected void post(final HttpServletRequest req, final HttpServletResponse resp) throws Exception {}
    protected void put(final HttpServletRequest req, final HttpServletResponse resp) throws Exception {}
    protected void delete(final HttpServletRequest req, final HttpServletResponse resp) throws Exception {}

    public static String getBody(final HttpServletRequest req) {
        final StringBuilder buf = new StringBuilder();
        String line = null;
        try {
            final BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null) {
                buf.append(line);
            }
        } catch (final Exception e) {}

        return buf.toString();
    }

    protected String getId(final HttpServletRequest req) throws UnsupportedEncodingException {
        final String path = req.getRequestURI();
        final Map<String, String> map = com.maintainer.util.Utils.getParts(path, "/{{resource}}/{{id}}");
        String id = map.get("id");
        id = URLDecoder.decode(id, "UTF-8");
        return id;
    }

    public static void sendJsonResponse(final HttpServletResponse resp, final String json) throws IOException {
        sendJsonResponse(resp, json.getBytes());
    }

    public static void sendJsonResponse(final HttpServletResponse resp, final byte[] bytes) throws IOException {
        resp.setContentType(APPLICATION_JSON);
        resp.setContentLength(bytes.length);
        resp.getOutputStream().write(bytes);
    }

    public static void sendError(final HttpServletResponse resp, final Exception e) throws IOException {
        Utils.severe(log, e);
        resp.setStatus(BAD_REQUEST);
        resp.getWriter().write(e.getMessage());
    }

    public static void recordError(final Exception e) {

    }

    protected static void setUser(final User user) {
        ThreadLocalInfo.setInfo(user);
    }

    protected static User getUser() {
        return ThreadLocalInfo.getInfo().getUser();
    }
}
