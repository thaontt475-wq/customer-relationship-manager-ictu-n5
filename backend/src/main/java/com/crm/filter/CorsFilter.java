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

        HttpServletRequest httpRequest =
                (HttpServletRequest) request;

        HttpServletResponse httpResponse =
                (HttpServletResponse) response;

        String origin =
                httpRequest.getHeader("Origin");

        httpResponse.addHeader("Vary", "Origin");

        if (
                origin != null &&
                ALLOWED_ORIGINS.contains(origin)
        ) {

            httpResponse.setHeader(
                    "Access-Control-Allow-Origin",
                    origin
            );

            httpResponse.setHeader(
                    "Access-Control-Allow-Credentials",
                    "true"
            );
        }

        httpResponse.setHeader(
                "Access-Control-Allow-Headers",
                "Content-Type, Authorization"
        );

        httpResponse.setHeader(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, PATCH, DELETE, OPTIONS"
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
