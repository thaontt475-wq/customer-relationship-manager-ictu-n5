package com.crm.filter;

public class EncodingFilter implements jakarta.servlet.Filter {
    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
                         jakarta.servlet.FilterChain chain) throws java.io.IOException, jakarta.servlet.ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        chain.doFilter(request, response);
    }
}
