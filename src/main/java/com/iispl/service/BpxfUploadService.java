package com.iispl.service;

import com.iispl.dto.InwardBatchDto;
import com.iispl.dto.LoginDTO;
import org.zkoss.util.media.Media;

import java.util.List;

public interface BpxfUploadService {
    void parseAndSave(Media file, String batchName, LoginDTO operator);
    List<InwardBatchDto> getAllBatches();
}