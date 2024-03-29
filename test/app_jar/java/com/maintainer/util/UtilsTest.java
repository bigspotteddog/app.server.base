package com.maintainer.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jasypt.util.text.BasicTextEncryptor;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.data.Method;
import org.restlet.data.Reference;

import com.google.gson.Gson;
import com.maintainer.data.model.MyField;
import com.maintainer.data.model.User;
import com.maintainer.data.provider.Key;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {

    public void testKey() {
        final Key key = Key.create(User.class, 123L);
        final String string = key.asString();
        assertEquals("User(123)", string);

        final Key key2 = Key.fromDecodedString(string);
        final String name = key2.getId().getClass().getSimpleName();
        assertEquals("Long", name);

        final Key key3 = Key.create(User.class, "Bob");
        final String string3 = key3.asString();
        assertEquals("User(\"Bob\")", string3);

        final Key key4 = Key.fromDecodedString(string3);
        final String name4 = key4.getId().getClass().getSimpleName();
        assertEquals("String", name4);
        assertEquals("Bob", key4.getId());
    }

    @Test
    public void test() {
        String string = "/user?username=bink.lynch@gmail.com";
        String pattern = "{{resource}}\\?{{query}}";
        final List<String> names = Arrays.asList("resource", "query");
        Map<String, String> parts = Utils.getParts(string, pattern, names);
        assertEquals(2, parts.size());

        string = "/tags/Watch?symbol=KO";
        pattern = "{{resource}}/{{tag}}\\?{{query}}";
        parts = Utils.getParts(string, pattern);
        assertEquals(3, parts.size());
    }

    public void test1a() {
        final Request request = new Request(Method.GET, "/data/tags/bob?name=test");
        final Reference rootRef = new Reference("/data/");
        request.setRootRef(rootRef);

        final String path = Utils.cleansPath(request);
        assertEquals("tags/bob", path);

        final String[] split = path.split("/");
        assertEquals(2, split.length);
        assertEquals("tags", split[0]);
        assertEquals("bob", split[1]);
    }

    public void test2() {
        final String source = "data/user/bob/arm?username=bink.lynch@gmail.com&url=/bob/bob";

        final String regex = "^/?([^/\\?]+)/([^/\\?]+)";

        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(source);

        final boolean find = matcher.find();

        for(int i = 0; i < matcher.groupCount() + 1; i++) {
            System.out.println(i + " = " + matcher.group(i));
        }
    }

    public void test3() {
        String source = "/data/{resource}?username={username}&url=/bob/bob";

        final String regex = "\\{(\\w+)\\}";

        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(source);

        final int count = 0;
        while (matcher.find()) {
            System.out.println(count + "=" + matcher.group(0));
        }
        source = matcher.replaceAll("(\\\\w+)");
        System.out.println(source);
    }

    public void test4() {
        final String template = "^([\\w.]+)\\/(.+)$";

        final String regex = "\\{\\{(\\w+)\\}\\}";

        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(template);

        final Map<String, String> variables = new LinkedHashMap<String, String>();
        final int count = 0;
        while (matcher.find()) {
            variables.put(matcher.group(1), null);
            System.out.println(count + "=" + matcher.group(1));
        }

        final String regex2 = matcher.replaceAll("(.+)");
        System.out.println(regex2);

        final Pattern pattern2 = Pattern.compile(regex2);

        final String source = "KO1.A/http://www.msn.com/hello.htm";
        final Matcher matcher2 = pattern2.matcher(source);

        final boolean find = matcher2.find();

        if (find) {
            for(int i = 0; i < matcher2.groupCount() + 1; i++) {
                System.out.println(i + " = " + matcher2.group(i));
            }
        }
    }

    public void test5() {
        String source = "/data/user?username=bink.lynch@gmail.com&url=/bob/bob";
        String template = "/data/{{resource}}\\?username={{username}}&url=/bob/bob";

        System.out.println(Utils.getParts(source, template, null));
        System.out.println(Utils.getParts(source, template, Arrays.asList("resource", "username")));

        template = "/data/(.+)\\?username=(.+)&";
        System.out.println(Utils.getParts(source, template, Arrays.asList("resource", "username")));

        template = "/data/(.+)\\?(.+)&";
        System.out.println(Utils.getParts(source, template, Arrays.asList("resource", "query")));

        source = "/data/resource/key?param=something";
        template = "/data/(.+)/(.+)[/\\?]";
        System.out.println(Utils.getParts(source, template, Arrays.asList("resource", "key")));
    }

    public void test6() {
        String source = "/user/bink.lynch@gmail.com?param1=value1&param2=value2";
        String template = "/?([^/\\?]+)/([^/\\?]+)[/\\?]?[^=]+=([^&]+)";

        Map<String, String> parts = Utils.getParts(source, template, Arrays.asList("resource", "id", "param1"));
        System.out.println(parts);

        source = "/user";
        template = "(\\w+)([/\\?].+)?";

        parts = Utils.getParts(source, template, Arrays.asList("resource", "id"));
        System.out.println(parts);

    }

    public void testJsonStringParsing() {

        final String json = "{\"field\": \"bob\", \"field1\":{\"test1\":\"test\",\"test2\":2}}";

        final Gson gson = Utils.getGson();
        final Json fromJson = gson.fromJson(json, Json.class);

        assertNotNull(fromJson);
    }

    public class Json {
        private String field;
        private JsonString field1;
    }

    public void testTextEncryption() {
        final BasicTextEncryptor encryptor = new BasicTextEncryptor();
        encryptor.setPassword("My very extra secret password");

        final String string = encryptor.encrypt("1234 4567 8901 2344");
        System.out.println(string);

        final String string2 = encryptor.decrypt(string);
        System.out.println(string2);
    }

    public void testMyField() {
        Field[] fields = FieldTest.class.getDeclaredFields();
        Field f = fields[0];

        MyField myField = new MyField(f);
    }

    private static class FieldTest {
        private String name;
    }
}
