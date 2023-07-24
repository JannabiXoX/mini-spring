package com.lfy.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;


/**
 * @Author FeiYang
 * @Date 7/24/2023 12:16 PM
 */
public class YamlUtils {


    //将yaml文件内容转化为Map对象
    public static Map<String, Object> loadYamlAsPlainMap(String path) {
        //加载yaml文件转化为对象
        Map<String, Object> data = loadYaml(path);
        //创建容器对象
        Map<String, Object> plain = new LinkedHashMap<>();
        //将加载的Map对象key前缀key - value对应
        converTo(data, "", plain);
        return plain;
    }

    private static Map<String, Object> loadYaml(String path) {
        //通过yaml内置方法创建yaml对象
        var loaderOptions = new LoaderOptions();
        var dumperOptions = new DumperOptions();
        var representer = new Representer(dumperOptions);
        var resolver = new NoImplicitResolver();
        var yaml = new Yaml(new Constructor(loaderOptions), representer, dumperOptions, loaderOptions, resolver);
        //读取指定yaml文件并且转化为map对象返回
        return ClassPathUtils.readInputStream(path, (input) -> {
            return (Map<String, Object>) yaml.load(input);
        });
    }

    private static void converTo(Map<String, Object> source, String prefix, Map<String, Object> plain) {
        for (String key : source.keySet()) {
            Object value = source.get(key);
            if (value instanceof Map) {
                Map<String, Object> subMap = (Map<String, Object>) value;
                converTo(subMap, prefix + key + ".", plain);
            } else if (value instanceof List) {
                plain.put(prefix + key, value);
            }else {
                plain.put(prefix + key,value.toString());
            }
        }
    }


}
//禁用隐式解析器功能
class NoImplicitResolver extends Resolver {

    public NoImplicitResolver() {
        super();
        super.yamlImplicitResolvers.clear();
    }
}
