package com.lfy.io;
/*
 *@Author:lfy
 *@Date:7/23/2023 3:08 PM
 */

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceResolver {

    Logger logger = LoggerFactory.getLogger(getClass());

    String basePackage;

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    //获取扫描到得basePackage
    public <R> List<R> scan(Function<Resource, R> mapper) throws IOException, URISyntaxException {
        //将包名转化为路径
        String basePackagePath = this.basePackage.replace(".", "/");
        String path = basePackagePath;
        List<R> collector = new ArrayList<>();
        scanO(basePackagePath, path, collector, mapper);
        return collector;
    }

    <R> void scanO(String basePackagePath, String path, List<R> collector, Function<Resource, R> mapper) throws IOException, URISyntaxException {
        logger.atDebug().log("scan path: {}", path);
        // 通过ClassLoader获取URL列表:
        Enumeration<URL> en = getContextClassLoader().getResources(path);
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            URI uri = url.toURI();
            String uriStr = removeTrailingSlash(uriToString(uri));
            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
            // 在目录中搜索
            if (uriBaseStr.startsWith("file:")) {
                uriBaseStr = uriBaseStr.substring(5);
            }
            // 在Jar包中搜索
            if (uriStr.startsWith("jar:")) {
                scanFile(true, uriBaseStr, jarUriToPath(basePackagePath, uri), collector, mapper);
            }

        }
    }

    <R> void scanFile(boolean isJar, String uriBaseStr, Path jarUriToPath, List<R> collector, Function<Resource, R> mapper) throws IOException {
        //除去路径中最后的分隔符
        String baseDir = removeTrailingSlash(uriBaseStr);
        //遍历文件并且过滤掉不可读文件
        Files.walk(jarUriToPath).filter(Files::isReadable).forEach(file -> {
            Resource resource = null;
            if (isJar) {
                //创建文件
                resource = new Resource(baseDir, removeLeadingSlash(file.toString()));
            } else {
                String path = file.toString();
                String name = removeLeadingSlash(path.substring(baseDir.length()));
                resource = new Resource("file:" + path, name);
            }
            logger.atDebug().log("found resource: {}", resource);
            //传递文件对象
            R r = mapper.apply(resource);
            if (r != null) {
                collector.add(r);
            }
        });
    }

    private String removeLeadingSlash(String s) {
        if (s.startsWith("/") || s.startsWith("\\")) {
            s.substring(1);
        }
        return s;
    }


    ClassLoader getContextClassLoader() {
        ClassLoader classLoader = null;
        classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        return classLoader;
    }

    String removeTrailingSlash(String uriToString) {
        if (uriToString.endsWith("/") || uriToString.endsWith("\\")) {
            uriToString = uriToString.substring(0, uriToString.length() - 1);
        }
        return uriToString;
    }

    String uriToString(URI uri) {
        return URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8);
    }

    Path jarUriToPath(String basePackagePath, URI jarUri) throws IOException {
        return FileSystems.newFileSystem(jarUri, Map.of()).getPath(basePackagePath);
    }
}
