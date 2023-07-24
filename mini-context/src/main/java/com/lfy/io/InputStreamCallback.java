package com.lfy.io;

import java.io.IOException;
import java.io.InputStream;
/**
 * @Author FeiYang
 * @Date 7/24/2023 2:59 PM
 */
@FunctionalInterface
public interface InputStreamCallback<T> {

    T doWithInputStream(InputStream stream) throws IOException;
}
