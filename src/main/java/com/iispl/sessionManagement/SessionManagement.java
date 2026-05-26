package com.iispl.sessionManagement;

import java.io.IOException;


import com.iispl.dto.UserModel;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class SessionManagement implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request   = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String requestURI = request.getRequestURI();

        // 1. Allow public paths — login page, ZK internals, static files
        if (requestURI.contains("login.zul")    ||
            requestURI.contains(".css")         ||
            requestURI.contains(".js")          ||
            requestURI.contains("/zkau")        ||
            requestURI.contains("/zkcomet")     ||
            requestURI.contains("favicon")) {
            chain.doFilter(req, res);
            return;
        }

        // 2. Check session exists and has loggedInUser
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInUser") == null) {
            response.sendRedirect(request.getContextPath() + "/zul/login.zul");
            return;
        }

        // 3. Read the UserModel from session — same key LoginController stores it
        UserModel user = (UserModel) session.getAttribute("loggedInUser");
        String role = user.getRoleId().toLowerCase().trim();

        // 4. Role-based URL protection
        if (requestURI.contains("/zul/makerDashboard")      && !"maker".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/zul/login.zul");
            return;
        }
        if (requestURI.contains("/zul/checkerDashboard")    && !"checker".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/zul/login.zul");
            return;
        }
        if (requestURI.contains("/zul/adminDashboard")      && !"admin".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/zul/login.zul");
            return;
        }
        if (requestURI.contains("/zul/micrRepairDashboard") && !"micr_repair".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/zul/login.zul");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override public void init(FilterConfig f) {}
    @Override public void destroy() {}
}