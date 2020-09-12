package com.asylumproject.asylumproject.manager;

import com.asylumproject.asylumproject.problemdomain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.*;

@Component
public class EmailServiceManager {

    private JavaMailSender emailSender;
    private ResourceLoader resourceLoader;

    @Autowired
    public EmailServiceManager(JavaMailSender emailSender, ResourceLoader resourceLoader) {
        this.emailSender = emailSender;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Send a simple email message.
     * @param to the destination email address.
     * @param subject the subject of the email address.
     * @param text the message's text.
     * @return true if message was sent, otherwise false.
     */
    public boolean sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            System.out.println(message);
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }

    }

    /**
     * Sent an email message wit information to reset a user's password.
     * @param contextPath the context path of the message.
     * @param user the receiver of the message.
     * @return true if message was sent, otherwise false.
     */
    public boolean sendResetPasswordMail(String contextPath, User user) {
//        MailType mailType = MailType.RESET_PASSWORD;
        MimeMessage mimeMessage = emailSender.createMimeMessage();
            StringBuilder content = new StringBuilder();

            try {
//                Resource resource = resourceLoader.getResource("classpath:" + mailType.getMailTemplate());
                Resource resource = resourceLoader.getResource("classpath:" + "static/MailTemplates/reset_password.html");

                BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()));
                String line = br.readLine();
                while (line != null) {
                    content.append(line);
                    line = br.readLine();
                }

                content = new StringBuilder(content.toString().replace("%name%", user.getFirstName() + " " + user.getLastName()));
                content = new StringBuilder(content.toString().replace("%reset_link%",
                        contextPath + "/auth/changePassword?id=" + user.getID() +
                                "&token=" + user.getResetToken()));

                mimeMessage.setContent(content.toString(), "text/html");
//                mimeMessage.setSubject(mailType.getMailSubject());
                mimeMessage.setSubject("Reset Your Password");
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false);
                helper.setTo(user.getEmail());
                emailSender.send(mimeMessage);
                return true;

            } catch (IOException | MessagingException e) {
                e.printStackTrace();
                return  false;
            }
    }
}
