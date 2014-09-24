package com.maintainer.data.commands;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class HttpResponseModel {
    private int status;
    private final List<HttpHeader> headers = new ArrayList<HttpHeader>();
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void addHeader(HttpHeader header) {
        headers.add(header);
    }

    public List<HttpHeader> getHeaders() {
        return headers;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public byte[] getBytes() {
        return out.toByteArray();
    }
}
