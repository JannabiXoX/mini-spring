package com.lfy.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import jakarta.annotation.Nullable;

/**
 * @Author FeiYang
 * @Date 7/24/2023 9:52 AM
 */

public class PropertyResolver {

    Logger logger = LoggerFactory.getLogger(getClass());

    Map<String, String> properties = new HashMap<>();

    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    public PropertyResolver(Properties props) {
        //存入环境变量
        this.properties.putAll(System.getenv());
        //存入Properoties
        Set<String> names = props.stringPropertyNames();
        for (String name : names) {
            this.properties.put(name, props.getProperty(name));
        }
        if (logger.isDebugEnabled()) {
            List<String> keys = new ArrayList<>(this.properties.keySet());
            Collections.sort((keys));
            for (String key : keys){
                logger.debug("PropertyResolver: {} = {}", key, this.properties.get(key));
            }
        }
        converters.put(String.class, s -> s);
        converters.put(boolean.class, s -> Boolean.parseBoolean(s));
        converters.put(Boolean.class, s -> Boolean.valueOf(s));

        converters.put(byte.class, s -> Byte.parseByte(s));
        converters.put(Byte.class, s -> Byte.valueOf(s));

        converters.put(short.class, s -> Short.parseShort(s));
        converters.put(Short.class, s -> Short.valueOf(s));

        converters.put(int.class, s -> Integer.parseInt(s));
        converters.put(Integer.class, s -> Integer.valueOf(s));

        converters.put(long.class, s -> Long.parseLong(s));
        converters.put(Long.class, s -> Long.valueOf(s));

        converters.put(float.class, s -> Float.parseFloat(s));
        converters.put(Float.class, s -> Float.valueOf(s));

        converters.put(double.class, s -> Double.parseDouble(s));
        converters.put(Double.class, s -> Double.valueOf(s));

        converters.put(LocalDate.class, s -> LocalDate.parse(s));
        converters.put(LocalTime.class, s -> LocalTime.parse(s));
        converters.put(LocalDateTime.class, s -> LocalDateTime.parse(s));
        converters.put(ZonedDateTime.class, s -> ZonedDateTime.parse(s));
        converters.put(Duration.class, s -> Duration.parse(s));
        converters.put(ZoneId.class, s -> ZoneId.of(s));
    }

    PropertyExpr parsePropertyExpr(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            // 是否存在defaultValue?
            int n = key.indexOf(':');
            if (n == (-1)) {
                // 没有defaultValue: ${key}
                String k = key.substring(2, key.length() - 1);
                return new PropertyExpr(k, null);
            } else {
                // 有defaultValue: ${key:default}
                String k = key.substring(2, n);
                return new PropertyExpr(k, key.substring(n + 1, key.length() - 1));
            }
        }
        return null;
    }



    @Nullable
    public String getProperty(String key) {
        // 解析${abc.xyz:defaultValue}:
        PropertyExpr keyExpr = parsePropertyExpr(key);
        if (keyExpr != null) {
            if (keyExpr.defaultValue() != null) {
                // 带默认值查询:
                return getProperty(keyExpr.key(), keyExpr.defaultValue());
            } else {
                // 不带默认值查询:
                return getRequiredProperty(keyExpr.key());
            }
        }
        // 普通key查询:
        String value = this.properties.get(key);
        if (value != null) {
            return parseValue(value);
        }
        return value;
    }

    @Nullable
    public <T> T getProperty(String key, Class<T> targetType) {
        String value = getProperty(key);
        if (value == null) {
            return null;
        }
        // 转换为指定类型:
        return convert(targetType, value);
    }

    public <T> T convert(Class<T> clazz, String value) {
        Function<String,Object> fun = this.converters.get(clazz);
        if (fun == null){
            throw new IllegalArgumentException("Unsupported value type: " + clazz.getName());
        }
        return (T) fun.apply(value);
    }

    public String parseValue(String value) {
        PropertyExpr expr = parsePropertyExpr(value);
        if (expr == null) {
            return value;
        }
        if (expr.defaultValue() != null) {
            return getProperty(expr.key(), expr.defaultValue());
        } else {
            return getRequiredProperty(expr.key());
        }
    }

    public String getRequiredProperty(String key) {
        String value = getProperty(key);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        T value = getProperty(key, targetType);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? parseValue(defaultValue) : value;
    }

    record PropertyExpr(String key, String defaultValue) {
    }
}
