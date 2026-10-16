package com.crm.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

public class CorsFilter implements Filter {

    private static final Set<String> ALLOWED_ORIGINS =
            Set.of(
                    "http://localhost:5500",
                    "http://127.0.0.1:5500"
            );

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpServletRequest httpRequest =
                (HttpServletRequest) request;

        HttpServletResponse httpResponse =
                (HttpServletResponse) response;

        String origin =
                httpRequest.getHeader("Origin");

        boolean developmentOrigin = origin != null && ALLOWED_ORIGINS.contains(origin);
        httpResponse.addHeader("Vary", "Origin");

        if (
                origin != null &&
                (developmentOrigin ||
                        ("POST".equalsIgnoreCase(httpRequest.getMethod()) ||
                         ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod()) &&
                          "POST".equalsIgnoreCase(httpRequest.getHeader("Access-Control-Request-Method")))) &&
                        (httpRequest.getRequestURI().equals(httpRequest.getContextPath() + "/api/leads/web-form") ||
                         httpRequest.getRequestURI().equals(httpRequest.getContextPath() + "/api/leads/web-form/submit")))
        ) {

            httpResponse.setHeader(
                    "Access-Control-Allow-Origin",
                    origin
            );

            httpResponse.setHeader(
                    "Access-Control-Allow-Credentials",
                    String.valueOf(developmentOrigin)
            );
        }

        httpResponse.setHeader(
                "Access-Control-Allow-Headers",
                developmentOrigin ? "Content-Type, Authorization" : "Content-Type"
        );

        httpResponse.setHeader(
                "Access-Control-Allow-Methods",
                developmentOrigin ? "GET, POST, PUT, PATCH, DELETE, OPTIONS" : "POST, OPTIONS"
        );

        if (
                "OPTIONS".equalsIgnoreCase(
                        httpRequest.getMethod()
                )
        ) {

            httpResponse.setStatus(
                    HttpServletResponse.SC_OK
            );

            return;
        }

        chain.doFilter(
                request,
                response
        );
    }
}
