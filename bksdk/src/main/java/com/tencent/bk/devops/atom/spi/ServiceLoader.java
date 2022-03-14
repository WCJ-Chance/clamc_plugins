package com.tencent.bk.devops.atom.spi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ServiceLoader {

    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();

    /**
     * 清除缓存
     */
    public static void clear() {
        CACHE.clear();
    }


    /**
     * 加载class类
     *
     * @param clazz 接口
     * @param <T>   接口类型
     * @return 接口 实现实例
     */
    public static <T> T load(Class<T> clazz, boolean canIgnore) {
        return load(clazz, null, canIgnore);
    }


    /**
     * 根据名称加载class类（优先从缓存中获取，如有多线程调用，接口实现需要自己实现线程安全）
     *
     * @param clazz 接口
     * @param name  利用spi 注解中的名字获取实例
     * @param <T>   接口 类型
     * @return 接口 实现实例
     */
    @SuppressWarnings("all")
    public static <T> T load(Class<T> clazz, String name, boolean canIgnore) {
        String key = clazz.getName();
        Object o = CACHE.get(key);
        if (o == null) {
            T t = load0(clazz, name, canIgnore);
            if (t != null) {
                CACHE.put(key, t);
                return t;
            }
        } else if (clazz.isInstance(o)) {
            return (T) o;
        }
        return load0(clazz, name, canIgnore);
    }

    /**
     * @param clazz service
     * @param name  加载的类
     * @param <T>   类型
     * @return 类
     */
    private static <T> T load0(Class<T> clazz, String name, boolean canIgnore) {
        java.util.ServiceLoader<T> factories = java.util.ServiceLoader.load(clazz);
        T t = filterByName(factories, name);

        if (t == null) {
            factories = java.util.ServiceLoader.load(clazz, ServiceLoader.class.getClassLoader());
            t = filterByName(factories, name);
        }

        if (t != null) {
            return t;
        } else if (canIgnore)
            return null;
        else {
            throw new IllegalStateException("Cannot find META-INF/services/" + clazz.getName() + " on classpath");
        }
    }

    private static <T> T filterByName(java.util.ServiceLoader<T> factories, String name) {
        Iterator<T> it = factories.iterator();
        if (name == null) {
            List<T> list = findList(it);
            if (list.size() > 0) {
                return list.get(0);
            }
        } else {
            while (it.hasNext()) {
                T t = it.next();
                if (name.equals(t.getClass().getName()) || name.equals(t.getClass().getSimpleName())) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * @param clazz 接口
     * @param <T>   接口类型
     * @return 接口 实现实例
     */
    public static <T> List<T> loadList(Class<T> clazz) {
        return loadList(clazz, null);
    }

    /**
     * 其实就取一次, 缓存没意义
     *
     * @param clazz service
     * @param name  加载的类
     * @param <T>   类型
     * @return 类
     */
    public static <T> List<T> loadList(Class<T> clazz, String name) {
        java.util.ServiceLoader<T> factories = java.util.ServiceLoader.load(clazz);
        List<T> list = filterListByName(factories, name);
        factories = java.util.ServiceLoader.load(clazz, ServiceLoader.class.getClassLoader());
        List<T> list1 = filterListByName(factories, name);
        list.addAll(list1);
        return list.stream().filter(distinctByKey(T::getClass)).collect(Collectors.toList());
    }

    private static <T> List<T> filterListByName(java.util.ServiceLoader<T> factories, String name) {
        Iterator<T> it = factories.iterator();
        if (name == null) {
            return findList(it);
        } else {
            ArrayList<T> list = new ArrayList<>();
            while (it.hasNext()) {
                T t = it.next();
                if (name.equals(t.getClass().getName()) ||
                    name.equals(t.getClass().getSimpleName())) {
                    list.add(t);
                }
            }
            return list;
        }
    }

    private static <T> List<T> findList(Iterator<T> it) {
        List<T> list = new ArrayList<>(2);
        while (it.hasNext()) {
            list.add(it.next());
        }
        if (list.size() > 1) {
            list.sort((o1, o2) -> {
                AtomService spi1 = o1.getClass().getAnnotation(AtomService.class);
                AtomService spi2 = o2.getClass().getAnnotation(AtomService.class);
                int order1 = spi1 == null ? 0 : spi1.order();
                int order2 = spi2 == null ? 0 : spi2.order();
                return order1 - order2;
            });
        }
        return list;
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
