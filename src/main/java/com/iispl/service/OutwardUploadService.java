package com.iispl.service;

import java.io.File;

public interface OutwardUploadService {

    // main method - takes the uploaded ZIP file and operator user id
    void processUpload(File zipFile, String uploadedBy);

}