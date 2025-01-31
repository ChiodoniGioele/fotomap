package ch.chiodonig.fotomap.controller;

import ch.chiodonig.fotomap.dto.*;
import ch.chiodonig.fotomap.model.User;
import ch.chiodonig.fotomap.security.JwtService;
import ch.chiodonig.fotomap.service.EmailService;
import ch.chiodonig.fotomap.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequestMapping("/api/v0/auth")
@Slf4j
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtTokenProvider;

    @Autowired
    private EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtTokenProvider.generateToken(loginRequest.getEmail());

            JwtResponse jwtResponse = new JwtResponse();
            jwtResponse.setToken(jwt);
            jwtResponse.setEmail(loginRequest.getEmail());
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(401).body("Invalid email or password");
        }
    }


    @PostMapping("/registration")
    public ResponseEntity<User> register(@RequestBody RegistrationRequest registrationRequest) {
        User user = new User();
        user.setEmail(registrationRequest.getEmail());
        user.setUsername(registrationRequest.getUsername());
        user.setPassword(registrationRequest.getPassword());
        if (userService.isRegistrationValid(user)) {
            userService.saveUser(user);
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.badRequest().body(user);
        }
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleTokenRequest googleTokenDTO) {
        String googleToken = googleTokenDTO.getToken();
        try {
            User googleUser = verifyGoogleToken(googleToken);
            if (googleUser == null) {
                return ResponseEntity.status(401).body("Invalid Google token");
            }
            if(!userService.isUserAlreadyExist(googleUser)){
                userService.saveUser(googleUser);
            }
            String jwt = jwtTokenProvider.generateToken(googleUser.getEmail());
            return ResponseEntity.ok(new JwtResponse(jwt, "Bearer", googleUser.getEmail(), "USER")); // TODO modify role
        } catch (Exception e) {
            log.error("Google login failed: {}", e.getMessage());
            return ResponseEntity.status(500).body("Google login failed");
        }
    }


    private User verifyGoogleToken(String token) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList("755040447389-fhiod0iuda4deqn2hf24ao3rva49b2j3.apps.googleusercontent.com")) // todo new key
                    .build();
            GoogleIdToken idToken = verifier.verify(token);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                User user = new User();
                user.setEmail(payload.getEmail());
                user.setUsername((String) payload.get("name"));
                user.setGoogleUser(true);
                return user;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("Error verifying Google token: {}", e.getMessage());
            return null;
        }
    }

    @PostMapping("/newPasswordEmail")
    public ResponseEntity<?> newPasswordEmail(@RequestBody EmailRequest email){
        if(!email.getEmail().isBlank() && !email.getEmail().isEmpty()){
            try {
                this.emailService.sendPasswordResetEmail(email.getEmail());
            } catch (Exception e) {
                log.error(null,e);
                return ResponseEntity.status(404).body("Ivalid email");
            }
            return ResponseEntity.status(200).body("Email sended");
        }
        return ResponseEntity.status(404).body("Ivalid email");
    }

}
