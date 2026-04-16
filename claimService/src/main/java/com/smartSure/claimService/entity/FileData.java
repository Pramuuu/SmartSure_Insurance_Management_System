package com.smartSure.claimService.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class FileData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;
    private String fileType;
    private String filePath;   // ← replaces byte[] data — stores path on disk
}