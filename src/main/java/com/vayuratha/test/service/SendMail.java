package com.vayuratha.test.service;


import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class SendMail {
    private final JavaMailSenderImpl mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void sendCredentials(String toEmail, String name, String username, String temporaryPassword) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setFrom(fromAddress);
            helper.setSubject("Welcome to Vayuratha - Your Test Portal Access");
            helper.setText(buildCredentialsHtml(name, username, temporaryPassword), true);

            mailSender.send(mimeMessage);

            System.out.println("======================================");
            System.out.println("EMAIL CREDENTIALS SENT");
            System.out.println("TO       : " + toEmail);
            System.out.println("USERNAME : " + username);
            System.out.println("======================================");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.out.println("======================================");
            System.out.println("EMAIL SEND FAILED");
            System.out.println("TO    : " + toEmail);
            System.out.println("ERROR : " + e.getMessage());
            System.out.println("======================================");
        }
    }

    private String buildCredentialsHtml(String name, String username, String temporaryPassword) {
        return "<!DOCTYPE html>" +
                "<html><body style=\"margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;\">" +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f4f4f4;padding:30px 0;\">" +
                "<tr><td align=\"center\">" +
                "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 10px rgba(0,0,0,0.08);\">" +

                // Header banner - gradient-style split block instead of plain black bar
                "<tr><td style=\"background-color:#EAB308;padding:6px;\"></td></tr>" +
                "<tr><td style=\"background-color:#0a0a0a;padding:36px 32px 28px 32px;text-align:center;\">" +
                "<span style=\"font-size:22px;font-weight:bold;color:#ffffff;letter-spacing:1px;\">VAYURATHA</span>" +
                "<div style=\"font-size:11px;color:#EAB308;letter-spacing:2px;margin-top:4px;text-transform:uppercase;\">Test Portal Access</div>" +
                "</td></tr>" +

                // Welcome line
                "<tr><td style=\"padding:28px 32px 4px 32px;text-align:center;\">" +
                "<p style=\"font-size:18px;color:#111111;margin:0 0 6px 0;font-weight:bold;\">Welcome, " + name + "!</p>" +
                "<p style=\"font-size:14px;color:#666666;line-height:20px;margin:0;\">" +
                "Your Vayuratha test portal account is ready. Use the credentials below to log in." +
                "</p>" +
                "</td></tr>" +

                // Credential "ticket" style box with dashed divider
                "<tr><td style=\"padding:24px 32px 8px 32px;\">" +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border:2px dashed #EAB308;border-radius:10px;\">" +
                "<tr><td style=\"padding:20px 22px;\">" +

                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" +
                "<tr>" +
                "<td style=\"padding-bottom:14px;\">" +
                "<span style=\"display:inline-block;background-color:#111111;color:#EAB308;font-size:10px;font-weight:bold;letter-spacing:1px;padding:3px 10px;border-radius:4px;text-transform:uppercase;\">Username</span>" +
                "<div style=\"font-size:17px;color:#111111;font-weight:bold;margin-top:8px;font-family:'Courier New',monospace;\">" + username + "</div>" +
                "</td>" +
                "</tr>" +
                "<tr><td style=\"border-top:1px solid #f0e2b0;padding-top:14px;\">" +
                "<span style=\"display:inline-block;background-color:#111111;color:#EAB308;font-size:10px;font-weight:bold;letter-spacing:1px;padding:3px 10px;border-radius:4px;text-transform:uppercase;\">Password</span>" +
                "<div style=\"font-size:17px;color:#111111;font-weight:bold;margin-top:8px;font-family:'Courier New',monospace;\">" + temporaryPassword + "</div>" +
                "</td></tr>" +
                "</table>" +

                "</td></tr>" +
                "</table>" +
                "</td></tr>" +

                // Security note
                "<tr><td style=\"padding:18px 32px 30px 32px;text-align:center;\">" +
                "<p style=\"font-size:12px;color:#999999;line-height:18px;margin:0;\">" +
                "For your security, please don't share your credential with any one." +
                "</p>" +
                "</td></tr>" +

                // Footer
                "<tr><td style=\"background-color:#0a0a0a;padding:18px 32px;text-align:center;\">" +
                "<span style=\"font-size:11px;color:#999999;\">&copy; " + LocalDate.now().getYear() + " Vayuratha Private Limited. All rights reserved.</span>" +
                "</td></tr>" +

                "</table>" +
                "</td></tr>" +
                "</table>" +
                "</body></html>";
    }

    public void sendRpcBookingConfirmation(String toEmail, String name, int slotNumber, LocalDate slotDate) {
        try {
            String formattedDate = slotDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setFrom(fromAddress);
            helper.setSubject("Vayuratha - RPC Training Slot Confirmed");
            helper.setText(buildRpcConfirmationHtml(name, slotNumber, formattedDate), true);

            mailSender.send(mimeMessage);

            System.out.println("======================================");
            System.out.println("RPC CONFIRMATION EMAIL SENT");
            System.out.println("TO   : " + toEmail);
            System.out.println("SLOT : " + slotNumber);
            System.out.println("DATE : " + formattedDate);
            System.out.println("======================================");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("======================================");
            System.out.println("RPC CONFIRMATION EMAIL FAILED");
            System.out.println("TO    : " + toEmail);
            System.out.println("ERROR : " + e.getMessage());
            System.out.println("======================================");
        }
    }

    private String buildRpcConfirmationHtml(String name, int slotNumber, String formattedDate) {
        return "<!DOCTYPE html>" +
                "<html><body style=\"margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;\">" +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f4f4f4;padding:30px 0;\">" +
                "<tr><td align=\"center\">" +
                "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 10px rgba(0,0,0,0.08);\">" +

                // Header
                "<tr><td style=\"background-color:#000000;padding:28px 32px;text-align:center;\">" +
                "<span style=\"font-size:22px;font-weight:bold;color:#EAB308;letter-spacing:1px;\">VAYURATHA</span>" +
                "<div style=\"font-size:11px;color:#ffffff;letter-spacing:2px;margin-top:2px;text-transform:uppercase;\">Private Limited</div>" +
                "</td></tr>" +

                // Success badge
                "<tr><td style=\"padding:32px 32px 0 32px;text-align:center;\">" +
                "<div style=\"display:inline-block;background-color:#EAB308;color:#000000;font-weight:bold;font-size:13px;padding:8px 20px;border-radius:999px;\">SLOT CONFIRMED</div>" +
                "</td></tr>" +

                // Body
                "<tr><td style=\"padding:20px 32px 8px 32px;\">" +
                "<p style=\"font-size:16px;color:#111111;margin:0 0 12px 0;\">Hi " + name + ",</p>" +
                "<p style=\"font-size:14px;color:#444444;line-height:22px;margin:0 0 20px 0;\">" +
                "Your advance amount of <strong>&#8377;1000</strong> has been received and your RPC drone pilot training slot is confirmed." +
                "</p>" +
                "</td></tr>" +

                // Details card
                "<tr><td style=\"padding:0 32px 24px 32px;\">" +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#fafafa;border:1px solid #eeeeee;border-radius:10px;\">" +
                "<tr>" +
                "<td style=\"padding:16px 20px;border-bottom:1px solid #eeeeee;\">" +
                "<span style=\"font-size:11px;color:#999999;text-transform:uppercase;letter-spacing:1px;\">Slot Number</span><br/>" +
                "<span style=\"font-size:16px;color:#111111;font-weight:bold;\">" + slotNumber + "</span>" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style=\"padding:16px 20px;\">" +
                "<span style=\"font-size:11px;color:#999999;text-transform:uppercase;letter-spacing:1px;\">Training Date</span><br/>" +
                "<span style=\"font-size:16px;color:#111111;font-weight:bold;\">" + formattedDate + "</span>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td></tr>" +

                // Note
                "<tr><td style=\"padding:0 32px 28px 32px;\">" +
                "<p style=\"font-size:13px;color:#666666;line-height:20px;margin:0;\">" +
                "Please arrive 15 minutes early on the training day. For any queries, feel free to contact our team." +
                "</p>" +
                "</td></tr>" +

                // Footer
                "<tr><td style=\"background-color:#000000;padding:18px 32px;text-align:center;\">" +
                "<span style=\"font-size:11px;color:#999999;\">&copy; " + LocalDate.now().getYear() + " Vayuratha Private Limited. All rights reserved.</span>" +
                "</td></tr>" +

                "</table>" +
                "</td></tr>" +
                "</table>" +
                "</body></html>";
    }
}