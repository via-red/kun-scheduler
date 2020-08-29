package com.miotech.kun.common.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.simple.JSONObject;

import java.lang.reflect.Type;

public class JSONUtils {

    private JSONUtils() {}

    public static String toJsonString(Object obj) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(obj);
    }

    public static JSONObject toJsonObject(Object object) {
        return toJsonObject(toJsonString(object));
    }

    public static JSONObject toJsonObject(String json)  {
        return toJavaObject(json, JSONObject.class);
    }

    public static <T> T toJavaObject(JSONObject jsonObject, Class<T> clazz) {
        return toJavaObject(jsonObject.toJSONString(), clazz);
    }

    public static <T> T toJavaObject(String json, Class<T> clazz) {
        Gson gson = new Gson();
        return gson.fromJson(json, clazz);
    }

    public static <T> T toJavaObject(String json, Type type) {
        Gson gson = new Gson();
        return gson.fromJson(json, type);
    }
}
