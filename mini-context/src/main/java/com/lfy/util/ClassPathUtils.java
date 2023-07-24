package com.lfy.util;

import com.lfy.io.InputStreamCallback;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * @Author FeiYang
 * @Date 7/24/2023 2:57 PM
 */

public class ClassPathUtils {

    public static <T> T readInputStream(String path, InputStreamCallback<T> inputStreamCallback){
        if (path.startsWith("/")){
            path = path.substring(1);
        }
        try (InputStream input = getContextClassLoader().getResourceAsStream(path)) {
            if (input == null){
                throw new FileNotFoundException("File not found in classpath" + path);
            }
            return inputStreamCallback.doWithInputStream(input);
        }catch (IOException e){
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    static ClassLoader getContextClassLoader() {
        ClassLoader classLoader = null;
        classLoader = Thread.currentThread().getContextClassLoader();;
        if (classLoader == null){
            classLoader = ClassLoader.class.getClassLoader();
        }
        return classLoader;
    }
}
