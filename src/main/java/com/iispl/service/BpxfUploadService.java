package com.iispl.service;

import java.util.List;

import org.zkoss.util.media.Media;

import com.iispl.dto.InwardBatchDto;
import com.iispl.dto.LoginDTO;
import com.iispl.jaxb.BpxfRoot;

public interface BpxfUploadService {
    void parseAndSave(Media file, String batchName, LoginDTO operator);
    void parseAndSaveRoot(BpxfRoot root,String resolvedBatchId, String fileName, LoginDTO operator);
    List<InwardBatchDto> getAllBatches();
}