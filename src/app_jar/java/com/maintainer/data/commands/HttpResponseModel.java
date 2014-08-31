package com.maintainer.data.commands;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponseModel {
    private int status;
    private Map<String, String> headers = new LinkedHashMap<String, String>();
    private ByteArrayOutputStream out = new ByteArrayOutputStream();

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public String setHeader(String key, String value) {
        return headers.put(key, value);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public byte[] getBytes() {
        return out.toByteArray();
    }
}
