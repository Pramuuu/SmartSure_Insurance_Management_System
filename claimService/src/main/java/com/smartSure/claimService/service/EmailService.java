package com.smartSure.claimService.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendClaimDecisionEmail(String toEmail, String userName, Long claimId, String decision) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("SmartSure — Your Claim #" + claimId + " has been " + decision);
        message.setText(buildEmailBody(userName, claimId, decision));
        mailSender.send(message);
    }

    private String buildEmailBody(String userName, Long claimId, String decision) {
        if ("APPROVED".equals(decision)) {
            return "Dear " + userName + ",\n\n"
                + "We are pleased to inform you that your insurance claim (ID: " + claimId + ") "
                + "has been APPROVED.\n\n"
                + "Our team will process the settlement shortly. "
                + "You will receive further communication regarding the payout.\n\n"
                + "Thank you for choosing SmartSure.\n\nRegards,\nSmartSure Claims Team";
        } else {
            return "Dear " + userName + ",\n\n"
                + "We regret to inform you that your insurance claim (ID: " + claimId + ") "
                + "has been REJECTED.\n\n"
                + "If you believe this decision is incorrect or wish to appeal, "
                + "please contact our support team with your claim ID.\n\n"
                + "Thank you for choosing SmartSure.\n\nRegards,\nSmartSure Claims Team";
        }
    }
}
