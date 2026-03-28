package com.smartSure.claimService.entity;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class FileData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fileName;
    private String fileType;
    @Lob
    private byte[] data;
}
