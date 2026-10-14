package com.crm.dto.permissions;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DTO đại diện cho mục menu điều hướng (CRM-26).
 * Phục vụ hiển thị trên giao diện Desktop và Mobile (360px).
 */
public final class MenuItem {
    private final String code;
    private final String label;
    private final String path;
    private final String section;
    private final String icon;
    private final List<MenuItem> children;

    public MenuItem(String code, String label, String path, String section, String icon) {
        this(code, label, path, section, icon, List.of());
    }

    public MenuItem(String code, String label, String path, String section, String icon, List<MenuItem> children) {
        this.code = Objects.requireNonNull(code, "code");
        this.label = Objects.requireNonNull(label, "label");
        this.path = path != null ? path : "";
        this.section = section != null ? section : "workspace";
        this.icon = icon != null ? icon : "";
        this.children = children != null ? Collections.unmodifiableList(children) : List.of();
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getPath() {
        return path;
    }

    public String getSection() {
        return section;
    }

    public String getIcon() {
        return icon;
    }

    public List<MenuItem> getChildren() {
        return children;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("name", code);
        map.put("label", label);
        map.put("path", path);
        map.put("section", section);
        map.put("icon", icon);
        if (children != null && !children.isEmpty()) {
            map.put("children", children.stream().map(MenuItem::toMap).toList());
        }
        return map;
    }
}
