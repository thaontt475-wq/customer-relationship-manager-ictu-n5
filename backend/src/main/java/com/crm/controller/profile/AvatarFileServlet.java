package com.crm.controller.profile;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.nio.file.*;

@WebServlet("/uploads/avatars/*")
public class AvatarFileServlet
        extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        String path =
                request.getPathInfo();

        if (
                path == null ||
                path.length() <= 1
        ) {
            response.sendError(404);
            return;
        }

        String filename =
                Paths.get(
                        path.substring(1)
                )
                .getFileName()
                .toString();

        Path file =
                Paths.get(
                        System.getProperty(
                                "user.home"
                        ),
                        "crm_uploads",
                        "avatars",
                        filename
                );

        if (!Files.exists(file)) {
            response.sendError(404);
            return;
        }

        String contentType =
                Files.probeContentType(
                        file
                );

        if (contentType != null) {
            response.setContentType(
                    contentType
            );
        }

        response.setContentLengthLong(
                Files.size(file)
        );

        Files.copy(
                file,
                response.getOutputStream()
        );
    }
}