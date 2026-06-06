package com.iispl.util;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@WebServlet("/imageServlet")
public class InwardImageServlet extends HttpServlet {

    /** Only allow access to CTS image directory — no directory traversal */
    private static final String ALLOWED_PREFIX = "/home/administrator/BpxfFile/Bpxf_Batch_1";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String path = req.getParameter("path");

        // ── Security check ──
        if (path == null || path.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Missing path parameter.");
            return;
        }

        // Prevent directory traversal attacks (e.g. path with "..")
        String normalized = new File(path).getCanonicalPath();
        if (!normalized.startsWith(ALLOWED_PREFIX)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                "Access denied.");
            return;
        }

        File file = new File(normalized);

        if (!file.exists() || !file.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                "Image not found: " + path);
            return;
        }

        // ── Set content type based on extension ──
        String lower = normalized.toLowerCase();
        if (lower.endsWith(".png")) {
            resp.setContentType("image/png");
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            resp.setContentType("image/jpeg");
        } else if (lower.endsWith(".gif")) {
            resp.setContentType("image/gif");
        } else {
            resp.setContentType("application/octet-stream");
        }

        // ── Stream file bytes to browser ──
        resp.setContentLengthLong(file.length());
        resp.setHeader("Cache-Control", "max-age=3600"); // cache 1 hour

        try (InputStream  in  = new FileInputStream(file);
             OutputStream out = resp.getOutputStream()) {

            byte[] buf = new byte[8192];
            int    n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }

        } catch (IOException e) {
            System.err.println("ImageServlet → Failed to stream: "
                    + normalized + " | " + e.getMessage());
        }
    }
}