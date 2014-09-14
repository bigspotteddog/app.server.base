package com.maintainer.data.provider;

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
        Map<String, String> headers = HttpRequestCommand.getHeaders(original);

        Class<?> type = key.getKind();
        String root = getPath(type);
        root = root + '/' + key.toString();

        HttpRequestCommand command = new HttpRequestCommand(root, Method.GET, headers);
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

    @Override
    public List<T> getAll(Class<?> type) throws Exception {
        HttpServletRequest original = getHttpServletRequest();
        Map<String, String> headers = HttpRequestCommand.getHeaders(original);

        String root = getPath(type);

        HttpRequestCommand command = new HttpRequestCommand(root, Method.GET, headers);
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
        Map<String, String> headers = HttpRequestCommand.getHeaders(original);

        Class<?> type = obj.getClass();
        String root = getPath(type);

        String body = com.maintainer.util.Utils.getGson().toJson(obj);

        headers.put("Content-Type", "application/json; charset=UTF-8");
        HttpRequestCommand command = new HttpRequestCommand(root, Method.POST, headers, body);
        HttpResponseModel resp = command.execute();
        int status = resp.getStatus();
        byte[] bytes = resp.getBytes();
        String json = new String(bytes);

        if (status != 200) {
            throw new Exception(json);
        }

        obj = (T) Utils.getGson().fromJson(json, type);
        return obj;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T put(T obj) throws Exception {
        Key key = obj.getKey();

        if (key == null) {
            throw new Exception("Cannot put an object that has not be previously saved.");
        }

        HttpServletRequest original = getHttpServletRequest();
        Map<String, String> headers = HttpRequestCommand.getHeaders(original);

        Class<?> type = obj.getClass();
        String root = getPath(type);
        root = root + '/' + key.toString();

        String body = com.maintainer.util.Utils.getGson().toJson(obj);

        headers.put("Content-Type", "application/json; charset=UTF-8");
        HttpRequestCommand command = new HttpRequestCommand(root, Method.PUT, headers, body);
        HttpResponseModel resp = command.execute();
        int status = resp.getStatus();
        byte[] bytes = resp.getBytes();
        String json = new String(bytes);

        if (status != 200) {
            throw new Exception(json);
        }

        obj = (T) Utils.getGson().fromJson(json, type);
        return obj;
    }

    @Override
    public Key delete(Key key) throws Exception {
        HttpServletRequest original = getHttpServletRequest();
        Map<String, String> headers = HttpRequestCommand.getHeaders(original);

        Class<?> type = key.getKind();
        String root = getPath(type);
        root = root + '/' + key.toString();

        HttpRequestCommand command = new HttpRequestCommand(root, Method.DELETE, headers);
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
