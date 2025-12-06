package com.whatsapp.android.util;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class SimpleCookieStore {
    private final TreeSet<Cookie> cookies;
    private final transient ReadWriteLock lock;

    public SimpleCookieStore() {
        this.cookies = new TreeSet<>(new CookieIdentityComparator());
        this.lock = new ReentrantReadWriteLock();
    }

    public static SimpleCookieStore parse(String cookies) {
        SimpleCookieStore cookieStore = new SimpleCookieStore();
        List<String> split = StrUtil.splitTrim(cookies, "; ");
        for (String s : split) {
            String key = StrUtil.subBefore(s, "=", false);
            String value = StrUtil.subAfter(s, "=", false);
            DefaultCookie cookie = new DefaultCookie(key, value);
            cookieStore.addCookie(cookie);
        }
        return cookieStore;
    }

    public static SimpleCookieStore parseJson(String cookies) {
        JSONArray objects = JSONArray.parseArray(cookies);
        SimpleCookieStore cookieStore = new SimpleCookieStore();
        for (int i = 0; i < objects.size(); i++) {
            JSONObject jsonObject = objects.getJSONObject(i);
            String name = jsonObject.getString("name");
            String value = jsonObject.getString("value");
            if (StringUtils.hasLength(name)) {
                cookieStore.addCookie(new DefaultCookie(name, value));
            }
        }
        return cookieStore;
    }

    public String getFirstCookie(String name) {
        for (Cookie cookie : cookies) {
            if (cookie.name().equals(name)) {
                return cookie.value();
            }
        }
        return null;
    }

    public void addCookie(final Cookie cookie) {
        if (cookie != null) {
            lock.writeLock().lock();
            try {
                //先删除旧cookie
                cookies.remove(cookie);
                cookies.add(cookie);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    public void addCookies(final Cookie[] cookies) {
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                this.addCookie(cookie);
            }
        }
    }

    public List<Cookie> getCookies() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(cookies);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            cookies.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return cookies.stream().map(x -> x.name() + "=" + x.value()).collect(Collectors.joining("; "));
        } finally {
            lock.readLock().unlock();
        }
    }

    private static class CookieIdentityComparator implements Comparator<Cookie> {
        @Override
        public int compare(Cookie c1, Cookie c2) {
            return c1.name().compareTo(c2.name());
        }
    }

}
