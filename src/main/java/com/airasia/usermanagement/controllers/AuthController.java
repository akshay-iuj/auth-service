package com.airasia.usermanagement.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import com.airasia.usermanagement.models.Role;
import com.airasia.usermanagement.models.User;
import com.airasia.usermanagement.payload.request.LoginRequest;
import com.airasia.usermanagement.payload.request.SignupRequest;
import com.airasia.usermanagement.payload.response.JwtResponse;
import com.airasia.usermanagement.payload.response.MessageResponse;
import com.airasia.usermanagement.repository.PermissionRepository;
import com.airasia.usermanagement.repository.RoleRepository;
import com.airasia.usermanagement.repository.UserRepository;
import com.airasia.usermanagement.security.jwt.JwtUtils;
import com.airasia.usermanagement.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;


    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Optional<User> userByEmail=userRepository.findByEmail(loginRequest.getUsername());
        Optional<User> userByUserName=userRepository.findByUsername(loginRequest.getUsername());
        if(!(userByEmail.isPresent() || userByUserName.isPresent())){
            return ResponseEntity.ok(new MessageResponse("User does not exist!"));
        }
        Authentication authentication ;
        if(userByEmail.isPresent())
         authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userByEmail.get().getUsername(), loginRequest.getPassword()));
        else
             authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userByUserName.get().getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());
        StringBuilder rolesString=new StringBuilder();
        for(String role : roles)
        {
            rolesString.append(role);
            rolesString.append(",");
        }
        String jwt = jwtUtils.generateJwtToken(authentication,rolesString.toString());
        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}
