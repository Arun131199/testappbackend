package com.vayuratha.test.service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendMail {
    private final JavaMailSenderImpl mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void sendCredentials(String toEmail,String name,String username,String temporaryPassword){
        try{
            SimpleMailMessage message=new SimpleMailMessage();
            message.setTo(toEmail);
            message.setFrom(fromAddress);
            message.setSubject("Vayuratha -Test Portal Credentials");
            message.setText(
                    "Hi " + name + ",\n\n" +
                            "Your account has been created successfully.\n\n" +
                            "Username: " + username + "\n" +
                            "Password: " + temporaryPassword + "\n\n" +
                            "Please log in with this credentials.\n\n" +
                            "Regards,\nVayuratha Pvt Ltd."
            );
            mailSender.send(message);
            System.out.println("======================================");
            System.out.println("EMAIL CREDENTIALS SENT");
            System.out.println("TO       : " + toEmail);
            System.out.println("USERNAME : " + username);
            System.out.println("======================================");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.out.println("======================================");
            System.out.println("EMAIL SEND FAILED");
            System.out.println("TO    : " + toEmail);
            System.out.println("ERROR : " + e.getMessage());
            System.out.println("======================================");
        }
    }
}
