package stockscreener.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import stockscreener.model.PremarketAlert;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendAlertEmail(String to, PremarketAlert alert) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Stock Alert: " + alert.getSymbol() + " (" + alert.getAlertType() + ")");

        String body =
                "A new stock alert has been triggered.\n\n" +
                "Symbol: " + alert.getSymbol() + "\n" +
                "Type: " + alert.getAlertType() + "\n" +
                "Triggered Price: " + alert.getTriggeredPrice() + "\n" +
                "Premarket High: " + alert.getPremarketHigh() + "\n" +
                "Premarket Low: " + alert.getPremarketLow() + "\n" +
                "Time: " + alert.getTriggeredAt() + "\n\n" +
                "— Stock Screener System";

        message.setText(body);

        mailSender.send(message);
    }
}
