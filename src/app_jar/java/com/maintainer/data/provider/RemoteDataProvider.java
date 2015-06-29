package com.maintainer.data.provider;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.maintainer.data.commands.HttpRequestCommand;
import com.maintainer.data.commands.HttpRequestCommand.Method;
import com.maintainer.data.commands.HttpResponseModel;
import com.maintainer.data.model.EntityImpl;
import com.maintainer.data.model.Resource;
import com.maintainer.data.model.ThreadLocalInfo;
import com.maintainer.util.ParameterizedTypeImpl;
import com.maintainer.util.Utils;

public class RemoteDataProvider<T extends EntityImpl> extends AbstractDataProvider<T> {

    private final String host;

    public RemoteDataProvider(String host) {
        this.host = host;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(Key key) throws Exception {
        HttpServletRequest original = getHttpServletRequest();
        Map<String, List<String>> headers = HttpRequestCommand.getHeaders(original);

        Class<?> type = key.getKind();
        String root = getPath(type);
        root = root + '/' + key.toString();

        HttpRequestCommand command = getHttpRequestCommand(root, Method.GET, headers);
        HttpResponseModel resp = command.execute();

        int status = resp.getStatus();
        byte[] bytes = resp.getBytes();
        String json = new String(bytes);

        if (status != 200) {
            throw new Exception(json);
        }

        T object = (T) Utils.getGson().fromJson(json, type);
        return object;
    }

    protected HttpRequestCommand getHttpRequestCommand(String root, Method method, Map<String, List<String>> headers) {
        HttpRequestCommand command = new HttpRequestCommand(root, method, headers);
        return command;
    }

    @Override
    public List<T> getAll(Class<?> type) throws Exception {
        HttpServletRequest original = getHttpServletRequest();
        Map<String, List<String>> headers = HttpRequestCommand.getHeaders(original);

        String root = getPath(type);

        HttpRequestCommand command = getHttpRequestCommand(root, Method.GET, headers);
        HttpResponseModel resp = command.execute();
        int status = resp.getStatus();
        byte[] bytes = resp.getBytes();
        String json = new String(bytes);

        if (status != 200) {
            throw new Exception(json);
        }

        ParameterizedTypeImpl typeImpl = com.maintainer.util.Utils.getParameterizedListType(type);
        List<T> list = Utils.getGson().fromJson(json, typeImpl);
        return list;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T post(T obj) throws Exception {
        HttpServletRequest original = getHttpServletRequest();
        Map<String, List<String>> headers = HttpRequestCommand.getHeaders(original);

        Class<?> type = obj.getClass();
        String root = getPath(type);

        String body = com.maintainer.util.Utils.getGson().toJson(obj);

        headers.put("Content-Type", Arrays.asList("application/json; charset=UTF-8"));
        Method method = Method.POST;
        HttpRequestCommand command = getHttpRequestCommand(root, method, headers, body);
        HttpResponseModel resp = command.execute();
        int status = resp.getStatus();
        byte[] bytes = resp.getBytes();
        String json = new String(bytes);

        if (status != 200) {
            throw new Exception(json);
        }

        T obj2 = (T) Utils.getGson().fromJson(json, type);
        obj.setKey(obj2.getKey());
        return obj2;
    }

    protected HttpRequestCommand getHttpRequestCommand(String root, Method method, Map<String, List<String>> headers, String body) {
        HttpRequestCommand command = getHttpRequestCommand(root, method, headers);
        command.setBody(body);
        return command;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T put(T obj) throws Exception {
        Key key = obj.getKey();

        if (key == null) {
            throw new Exception("Cannot put an object that has not be previously saved.");
        }

        HttpServletRequest original = getHttpServletRequest();
        Map<String, List<String>> headers = HttpRequestCommand.getHeaders(original);

        Class<?> type = obj.getClass();
        String root = getPath(type);
        root = root + '/' + key.toString();

        String body = com.maintainer.util.Utils.getGson().toJson(obj);

        headers.put("Content-Type", Arrays.asList("application/json; charset=UTF-8"));
        HttpRequestCommand command = getHttpRequestCommand(root, Method.PUT, headers, body);
        HttpResponseModel resp = command.execute();
        int status = resp.getStatus();
        byte[] bytes = resp.getBytes();
        String json = new String(bytes);

        if (status != 200) {
            throw new Exception(json);
        }

        T obj2 = (T) Utils.getGson().fromJson(json, type);
        obj.setKey(obj2.getKey());
        return obj2;
    }

    @Override
    public Key delete(Key key) throws Exception {
        HttpServletRequest original = getHttpServletRequest();
        Map<String, List<String>> headers = HttpRequestCommand.getHeaders(original);

        Class<?> type = key.getKind();
        String root = getPath(type);
        root = root + '/' + key.toString();

        HttpRequestCommand command = getHttpRequestCommand(root, Method.DELETE, headers);
        HttpResponseModel resp = command.execute();
        int status = resp.getStatus();
        byte[] bytes = resp.getBytes();
        String json = new String(bytes);

        if (status != 200) {
            throw new Exception(json);
        }

        return key;
    }

    @Override
    public List<T> find(Query query) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<T> getAll(Collection<Key> keys) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    private HttpServletRequest getHttpServletRequest() {
        ThreadLocalInfo info = ThreadLocalInfo.getInfo();
        HttpServletRequest req = info.getReq();
        return req;
    }

    private String getPath(Class<?> type) {
        Resource resource = type.getAnnotation(Resource.class);
        String path = resource.name();
        String root = host + "/data/" + path;
        return root;
    }
}
