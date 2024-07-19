package com.app.MailService.Model;

import java.util.HashMap;
import java.util.Map;

public class RequestContext {
    private static final ThreadLocal<Map<String, String>> CONTEXT = ThreadLocal.withInitial(HashMap::new);

    // Get the current request context map
    public static Map<String, String> getCurrentContext() {
        return CONTEXT.get();
    }

    // Set a value in the context
    public static void set(String key, String value) {
        getCurrentContext().put(key, value);
    }

    // Get a value from the context
    public static String get(String key) {
        return getCurrentContext().get(key);
    }

    // Clear the context after request processing
    public static void clear() {
        CONTEXT.remove();
    }
}
