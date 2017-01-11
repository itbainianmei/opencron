package org.opencron.server.job;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by benjobs on 2016/12/9.
 */
public class OpencronContext {

    private static Map<String,Object> context = new ConcurrentHashMap<String,Object>(0);

    public static Object get(String key){
        return context.get(key);
    }

    public static <T>T get(String key,Class<T> clazz){
        return (T) context.get(key);
    }

    public static void put(String key,Object value){
        context.put(key,value);
    }

    public static Object remove(String key){
        return context.remove(key);
    }

}
