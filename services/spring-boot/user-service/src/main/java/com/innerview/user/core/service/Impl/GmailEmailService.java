package com.innerview.user.core.service.Impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.innerview.user.core.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailEmailService implements EmailService {

	private final JavaMailSender javaMailSender;

	// Inject the Thymeleaf template engine
	private final TemplateEngine templateEngine;

	@Value("${spring.mail.username}")
	private String senderEmail;

	private final String FRONTEND_URL = "http://innrview.com/reset-password";

	@Override
	@Async
	// Notice the updated signature to include 'username'
	public void sendPasswordResetEmail(String to, String username, String rawToken) {
		try {
			// 1. Create a MimeMessage (required for HTML)
			MimeMessage message = javaMailSender.createMimeMessage();

			// 'true' indicates we need a multipart message for HTML
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setFrom(senderEmail);
			helper.setTo(to);
			helper.setSubject("InnerView Platform - Password Reset Request");

			// 2. Prepare the dynamic variables for the HTML template
			String link = FRONTEND_URL + "?token=" + rawToken;

			Context context = new Context();
			context.setVariable("name", username); // Injects into th:text="${name}"
			context.setVariable("resetUrl", link); // Injects into th:href="${resetUrl}"
			context.setVariable("expirationMinutes", 15); // Injects into th:text="${expirationMinutes}"

			// 3. Process the HTML file (looks for reset-password.html in src/main/resources/templates)
			String htmlBody = templateEngine.process("reset-password", context);

			// 4. Set the text. The 'true' flag tells Spring this is HTML, not plain text.
			helper.setText(htmlBody, true);

			// 5. Send it
			javaMailSender.send(message);
			log.info("HTML Email sent successfully to {}", to);

		} catch (MessagingException e) {
			log.error("Failed to build HTML email for {}", to, e);
		} catch (Exception e) {
			log.error("Failed to send email to {}", to, e);
		}
	}
}