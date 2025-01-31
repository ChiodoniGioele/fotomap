package ch.chiodonig.fotomap.service;


import ch.chiodonig.fotomap.model.User;
import ch.chiodonig.fotomap.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public boolean isRegistrationValid(User request){
        if(
                !(request.getEmail().isEmpty() || request.getEmail().isBlank()) &&
                        !(request.getUsername().isEmpty() || request.getUsername().isBlank()) &&
                        !(request.getPassword().isEmpty() || request.getPassword().isBlank()))
        {
            if(isUserAlreadyExist(request)) return false;
            return true;
        }
        return false;
    }

    public void saveUser(User user) {
        if(user.getPassword() != null){
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userRepository.save(user);
    }

    public User findByEmail(String email){
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isPresent()){
            return user.get();
        }
        return null;
    }

    public boolean isUserAlreadyExist(User user) {
        if(userRepository.findByEmail(user.getEmail()).isPresent()){
            return true;
        }
        return false;
    }

    public void updateUser(User user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateUser'");
    }
}

