package com.crm.service.customers;

import java.net.*;
import java.text.Normalizer;
import java.util.*;

/** Matching suggests records only; it never authorizes or performs a merge. */
public final class CustomerDuplicateMatcher {
    private CustomerDuplicateMatcher() {}
    public static String tax(String value) {
        return value==null?"":value.replaceAll("[\\s.-]","").toUpperCase(Locale.ROOT);
    }
    public static String website(String value) {
        if(value==null || value.isBlank()) return "";
        try {
            String raw=value.trim().toLowerCase(Locale.ROOT);
            URI uri=URI.create(raw.contains("://")?raw:"https://"+raw);
            if(!Set.of("http","https").contains(String.valueOf(uri.getScheme())) || uri.getHost()==null || uri.getUserInfo()!=null) return "";
            String host=IDN.toASCII(uri.getHost()).replaceFirst("^www\\.","").replaceFirst("\\.$","");
            return host;
        } catch(IllegalArgumentException e) {return "";}
    }
    public static String name(String value) {
        if(value==null) return "";
        String normalized=Normalizer.normalize(value.toLowerCase(Locale.ROOT).replace('đ','d'),Normalizer.Form.NFD)
                .replaceAll("\\p{M}+","").replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();
        // Remove only legal-form phrases, preserving meaningful business names.
        return normalized.replaceAll("\\b(cong ty|co phan|trach nhiem huu han|tnhh|company|limited|joint stock)\\b"," ")
                .replaceAll("\\s+"," ").trim();
    }
    public static double similarity(String a,String b) {
        String left=name(a),right=name(b);
        if(left.isEmpty() || right.isEmpty()) return 0;
        Set<String> l=new HashSet<>(List.of(left.split(" "))),r=new HashSet<>(List.of(right.split(" ")));
        Set<String> union=new HashSet<>(l);union.addAll(r);l.retainAll(r);
        return (double)l.size()/union.size();
    }
    public static Map<String,Object> match(Map<String,Object> a,Map<String,Object> b) {
        List<String> reasons=new ArrayList<>();
        String taxA=tax((String)a.get("taxCode")),taxB=tax((String)b.get("taxCode"));
        if(!taxA.isEmpty() && taxA.equals(taxB)) reasons.add("Trùng mã số thuế");
        else if(taxA.matches("[0-9]{10}([0-9]{3})?") && taxB.matches("[0-9]{10}([0-9]{3})?") && taxA.substring(0,10).equals(taxB.substring(0,10)))
            reasons.add("Trùng mã số thuế gốc chi nhánh; cần kiểm tra trước khi gộp");
        String web=website((String)a.get("website"));
        if(!web.isEmpty() && web.equals(website((String)b.get("website")))) reasons.add("Trùng domain website");
        double similarity=similarity((String)a.get("name"),(String)b.get("name"));
        if(similarity>=0.65) reasons.add("Tên doanh nghiệp tương đồng "+Math.round(similarity*100)+"%");
        if(reasons.isEmpty()) return null;
        Map<String,Object> result=new LinkedHashMap<>();result.put("record",b);result.put("reasons",reasons);result.put("similarityScore",similarity);
        return result;
    }
}
