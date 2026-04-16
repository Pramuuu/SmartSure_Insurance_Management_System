package com.smartSure.claimService.service;

import com.smartSure.claimService.entity.Claim;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

/**
 * Auto-generates a PDF claim form from the digital data submitted by the customer.
 *
 * This implementation uses OpenPDF (a community-maintained fork of iText)
 * to reliably generate compliant, perfectly formatted PDF documents.
 */
@Slf4j
@Service
public class PdfGenerationService {

    @Value("${claim.file.storage.path:./claim-files}")
    private String storagePath;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    /**
     * Generates a PDF claim form from the claim data and saves it to disk.
     * Returns the file path where the PDF was saved.
     */
    public String generateClaimFormPdf(Claim claim) throws IOException {

        Path claimDir = Paths.get(storagePath, String.valueOf(claim.getId()));
        Files.createDirectories(claimDir);

        String fileName = "claim_form_" + claim.getId() + ".pdf";
        Path pdfPath = claimDir.resolve(fileName);

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(pdfPath.toFile()));
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            document.add(new Paragraph("SMARTSURE INSURANCE - CLAIM FORM", titleFont));
            document.add(new Paragraph("==================================================", normalFont));
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("CLAIM REFERENCE", headerFont));
            document.add(new Paragraph("------------------------------", normalFont));
            document.add(new Paragraph("Claim ID        : " + claim.getId(), normalFont));
            document.add(new Paragraph("Policy ID       : " + claim.getPolicyId(), normalFont));
            document.add(new Paragraph("Status          : " + claim.getStatus(), normalFont));
            String filedOn = claim.getTimeOfCreation() != null ? claim.getTimeOfCreation().format(DATETIME_FORMAT) : "N/A";
            document.add(new Paragraph("Filed On        : " + filedOn + "\n\n", normalFont));

            document.add(new Paragraph("CLAIM DETAILS", headerFont));
            document.add(new Paragraph("------------------------------", normalFont));
            document.add(new Paragraph("Claim Amount    : INR " + claim.getAmount(), normalFont));
            document.add(new Paragraph("Incident Type   : " + (claim.getIncidentType() != null ? claim.getIncidentType().name() : "N/A"), normalFont));
            String incidentDate = claim.getIncidentDate() != null ? claim.getIncidentDate().format(DATE_FORMAT) : "N/A";
            document.add(new Paragraph("Incident Date   : " + incidentDate, normalFont));
            document.add(new Paragraph("Location        : " + safe(claim.getIncidentLocation()), normalFont));
            document.add(new Paragraph("Description     : " + safe(claim.getDescription()) + "\n\n", normalFont));

            if (claim.getIncidentType() != null) {
                switch (claim.getIncidentType()) {
                    case HEALTH -> {
                        document.add(new Paragraph("HEALTH DETAILS", headerFont));
                        document.add(new Paragraph("------------------------------", normalFont));
                        document.add(new Paragraph("Hospital Name   : " + safe(claim.getHospitalName()), normalFont));
                        document.add(new Paragraph("Treatment Type  : " + safe(claim.getTreatmentType()) + "\n\n", normalFont));
                    }
                    case ACCIDENT, THEFT -> {
                        document.add(new Paragraph("ACCIDENT / THEFT DETAILS", headerFont));
                        document.add(new Paragraph("------------------------------", normalFont));
                        document.add(new Paragraph("Vehicle Number  : " + safe(claim.getVehicleNumber()), normalFont));
                        document.add(new Paragraph("Garage/Workshop : " + safe(claim.getGarageRepairShop()), normalFont));
                        document.add(new Paragraph("Police Report # : " + safe(claim.getPoliceReportNumber()), normalFont));
                        document.add(new Paragraph("Witness Name    : " + safe(claim.getWitnessName()), normalFont));
                        document.add(new Paragraph("Witness Contact : " + safe(claim.getWitnessContact()) + "\n\n", normalFont));
                    }
                    default -> {}
                }
            }

            document.add(new Paragraph("DIGITAL CONSENT", headerFont));
            document.add(new Paragraph("------------------------------", normalFont));
            document.add(new Paragraph("Consent Given   : " + (Boolean.TRUE.equals(claim.getConsentGiven()) ? "YES" : "NO"), normalFont));
            String consentTime = claim.getConsentTimestamp() != null ? claim.getConsentTimestamp().format(DATETIME_FORMAT) : "N/A";
            document.add(new Paragraph("Consent Time    : " + consentTime, normalFont));
            document.add(new Paragraph("IP Address      : " + safe(claim.getConsentIpAddress()) + "\n\n", normalFont));

            document.add(new Paragraph("DECLARATION", headerFont));
            document.add(new Paragraph("------------------------------", normalFont));
            document.add(new Paragraph("I hereby declare that the information provided above is true and accurate to the best of my knowledge. Any false information may result in rejection of this claim and may constitute fraud.\n", normalFont));
            document.add(new Paragraph("This claim was submitted digitally and is legally binding under the Information Technology Act, 2000.\n", normalFont));
            document.add(new Paragraph("\nSmartSure Insurance Platform", headerFont));
            document.add(new Paragraph("Auto-generated document - No physical signature required", normalFont));

            document.close();

            log.info("Auto-generated claim form PDF using OpenPDF — claimId={}, path={}", claim.getId(), pdfPath);
            return pdfPath.toString();

        } catch (DocumentException e) {
            log.error("PDF generation failed using OpenPDF: {}", e.getMessage());
            throw new IOException("Failed to generate PDF document", e);
        }
    }

    private String safe(String value) {
        return value != null ? value : "N/A";
    }
}