package com;

import com.lfy.io.ResourceResolver;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class ResourceResolverTest {
    @Test
    public void scanClass() throws IOException, URISyntaxException {
        var pkg = "com.lfy.test";
        var rr = new ResourceResolver(pkg);
        List<String> classes = rr.scan(res -> {
            String name = res.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            return null;
        });
        Collections.sort(classes);
        System.out.println(classes);
        String[] listClasses = new String[] {
                // list of some scan classes:
                "com.lfy.test.StudentBean", //

        };
        for (String clazz : listClasses) {
            assertTrue(classes.contains(clazz));
        }
    }

}
