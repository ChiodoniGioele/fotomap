package ch.chiodonig.fotomap.service;


import ch.chiodonig.fotomap.model.User;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final UserService userService;

    @Value("${mailjet.api.key.public}")
    private String apiKeyPublic;

    @Value("${mailjet.api.key.private}")
    private String apiKeyPrivate;

    public void sendPasswordResetEmail(String email) throws Exception {
        User user = userService.findByEmail(email);
        String code = generateVerificationCode();
        if(user == null || user.isGoogleUser()){
            throw new Exception();
        }
        user.setResetPasswordCode(code);
        userService.saveUser(user);
        String subject = "Password Reset Request";
        String message = formatEmailHtml(user.getUsername(), code);

        sendMail(user, subject, message);
    }

    public void sendMail(User to, String subject, String text) throws MailjetException {
        MailjetClient client = new MailjetClient(apiKeyPublic, apiKeyPrivate);
        MailjetRequest request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject()
                                        .put("Email", "gioele.chiodoni@gmail.com")
                                        .put("Name", "Training Plan App"))
                                .put(Emailv31.Message.TO, new JSONArray()
                                        .put(new JSONObject()
                                                .put("Email", to.getEmail())
                                                .put("Name", to.getUsername())))
                                .put(Emailv31.Message.SUBJECT, subject)
                                .put(Emailv31.Message.HTMLPART, text)));
        log.info("Sending email to {}: {}", to.getEmail(), subject);
        client.post(request);
    }

    private String formatEmailHtml(String username, String code) {
        return String.format(
                "<!DOCTYPE html>" +
                        "<html lang='en'>" +
                        "<head>" +
                        "    <meta charset='UTF-8'>" +
                        "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "    <title>Password Reset</title>" +
                        "    <style>" +
                        "        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
                        "        .email-container { max-width: 600px; margin: 20px auto; background: #ffffff; border: 1px solid #ddd; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); }" +
                        "        .header { background: #007BFF; color: #ffffff; text-align: center; padding: 20px; font-size: 24px; font-weight: bold; }" +
                        "        .content { padding: 20px; line-height: 1.6; color: #333333; }" +
                        "        .content p { margin: 10px 0; }" +
                        "        .verification-code { font-size: 18px; font-weight: bold; color: #007BFF; text-align: center; margin: 20px 0; }" +
                        "        .footer { text-align: center; font-size: 12px; color: #888888; padding: 20px; border-top: 1px solid #ddd; }" +
                        "        .footer a { color: #007BFF; text-decoration: none; }" +
                        "    </style>" +
                        "</head>" +
                        "<body>" +
                        "    <div class='email-container'>" +
                        "        <div class='header'>Training Plan App</div>" +
                        "        <div class='content'>" +
                        "            <p>Hi <strong>%s</strong>,</p>" +
                        "            <p>You requested a password reset for your account. Please use the verification code below to reset your password:</p>" +
                        "            <div class='verification-code'>%s</div>" +
                        "            <p>If you did not request this password reset, you can safely ignore this email. No changes will be made to your account.</p>" +
                        "            <p>Thank you,<br>The Training Plan App Team</p>" +
                        "        </div>" +
                        "        <div class='footer'>" +
                        "            <p>If you have any questions, please contact us at " +
                        "               <a href='mailto:gioele.chiodoni@gmail.com'>gioele.chiodoni@gmail.com</a>.</p>" +
                        "        </div>" +
                        "    </div>" +
                        "</body>" +
                        "</html>",
                username, code
        );
    }



    private String generateVerificationCode() {
        return String.format("%06d", new SecureRandom().nextInt(1000000));
    }
}