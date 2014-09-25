package com.maintainer.data.commands;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpResponseModel {
    private int status;
    private final Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void addHeader(String name, String value) {
        List<String> list = headers.get(name);
        if (list == null) {
            list = new ArrayList<String>();
            headers.put(name, list);
        }
        list.add(value);
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public byte[] getBytes() {
        return out.toByteArray();
    }
}
