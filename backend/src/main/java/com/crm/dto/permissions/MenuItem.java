package com.crm.dto.permissions;

import java.util.List;
import java.util.Objects;

/** Public navigation contract; authorization details stay in the service. */
public final class MenuItem {
    private final String code;
    private final String label;
    private final String url;
    private final String icon;
    private final List<MenuItem> children;

    public MenuItem(String code, String label, String url, String icon, List<MenuItem> children) {
        this.code = Objects.requireNonNull(code, "code");
        this.label = Objects.requireNonNull(label, "label");
        this.url = url;
        this.icon = icon;
        this.children = List.copyOf(Objects.requireNonNull(children, "children"));
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public String getUrl() { return url; }
    public String getIcon() { return icon; }
    public List<MenuItem> getChildren() { return children; }
}
