package com.smartSure.claimService.exception;

public class DocumentNotUploadedException extends RuntimeException {
    public DocumentNotUploadedException(String documentType, Long claimId) {
        super(documentType + " not uploaded for claim id: " + claimId);
    }
}
