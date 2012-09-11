package com.maintainer.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.junit.Test;

public class UtilsTest extends TestCase {

    @Test
    public void test() {
        final String string = "/user?username=bink.lynch@gmail.com";
        final String pattern = "\\?";
        final List<String> names = Arrays.asList("resource", "query");
        final Map<String, String> parts = Utils.getParts(string, pattern, names);
        assertEquals(2, parts.size());
    }

    public void test2() {
        final String source = "data/user/bob/arm?username=bink.lynch@gmail.com&url=/bob/bob";

        final String regex = "^/?(\\w+)/(\\w+)[/\\?]?(.+)$";

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
}
