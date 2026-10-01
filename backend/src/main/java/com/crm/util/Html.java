package com.crm.util;

/** Escaping at the HTML boundary; never use this to construct SQL. */
public final class Html {
    private Html() { }
    public static String escape(Object value) {
        return value == null ? "" : value.toString().replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
