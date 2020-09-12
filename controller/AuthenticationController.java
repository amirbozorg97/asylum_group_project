package com.asylumproject.asylumproject.controller;

import com.asylumproject.asylumproject.manager.*;
import com.asylumproject.asylumproject.payload.*;
import com.asylumproject.asylumproject.problemdomain.Event;
import com.asylumproject.asylumproject.problemdomain.User;
import com.asylumproject.asylumproject.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthenticationController {

    /**
     * Authentication Manager instance used for user sign in.
     */
    private AuthenticationManager authenticationManager;
    /**
     * User Manager instance used to retrieve user information.
     */
    private UserManager userManager;
    /**
     * Password Encoder service used to encrypt passwords
     */
    private PasswordEncoder passwordEncoder;
    /**
     * JWT Token Provider used for token creation as well as for user authentication.
     */
    private JwtTokenProvider tokenProvider;
    /**
     * Email Service Manager used to send password restoring information to users.
     */
    private EmailServiceManager emailServiceManager;
    /**
     * Event Manager used to log events.
     */
    private EventManager eventManager;

    /**
     * Constructor
     * @param authenticationManager Authentication Manager
     * @param userManager User Manager
     * @param passwordEncoder Password Encoder
     * @param tokenProvider Token Provider
     * @param eventManager Event Manager
     * @param emailServiceManager Email Service Manager
     */
    @Autowired
    public AuthenticationController(AuthenticationManager authenticationManager,
                                    UserManager userManager,
                                    PasswordEncoder passwordEncoder,
                                    JwtTokenProvider tokenProvider,
                                    EventManager eventManager,
                                    EmailServiceManager emailServiceManager) {
        this.authenticationManager = authenticationManager;
        this.userManager = userManager;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.eventManager = eventManager;
        this.emailServiceManager = emailServiceManager;
    }

    /**
     * Endpoint for user login.
     * @param loginRequest login request information holding username and password for authentication.
     * @return http response entity.
     */
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsernameOrEmail(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            Optional<User> userOptional = userManager.getUserByUserNameOrEmail(loginRequest.getUsernameOrEmail());
            if(userOptional.isPresent()) {
                User user = userOptional.get();
                UserLoginInfo userInfo = new UserLoginInfo(user);
                eventManager.logEvent (user, Event.Operation.SIGN_IN, null);
                return ResponseEntity.ok(new JwtAuthenticationResponse(jwt, userInfo));
            }
            else {
                return new ResponseEntity<>(new ApiResponse(false, "Unable to signin"), HttpStatus.SERVICE_UNAVAILABLE);
            }
        }catch (BadCredentialsException | InternalAuthenticationServiceException e){
            return new ResponseEntity<>(new ApiResponse(false, "Incorrect username or password"), HttpStatus.UNAUTHORIZED);
        }catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(new ApiResponse(false, "Could not sign in. An error occured."), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<Boolean> logout(@RequestHeader("Authorization") String jwt){
        eventManager.logEvent(jwt, Event.Operation.SIGN_OUT, null);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    /**
     * Endpoint to request a password request.
     * @param request the http servlet request intance
     * @param userNameOrEmail user's username of email
     * @return http response entity.
     */
    @GetMapping("/rqpwdreset/{userNameOrEmail}")
    public ResponseEntity<?> requestPasswordReset(HttpServletRequest request,
                                                  @PathVariable String userNameOrEmail) {

        Optional<User> userOptional = userManager.getUserByUserNameOrEmail(userNameOrEmail);

        if(userOptional.isPresent()) {
            User user = userOptional.get();
            if(user.isDeleted()) {
                return new ResponseEntity<>(new ApiResponse(false, "User inactive, contact System Admin."),
                        HttpStatus.BAD_REQUEST);
            }
            else {
                user.setResetToken(UUID.randomUUID().toString());
                user = userManager.setUserUUID(user);
                // TODO REVIEW HOW TO RETRIEVE THE URL USED TO CALL THIS METHOD. This is part of the reset password link.
                // TODO The following lines are retrieving the server's url...
                String path = request.getRequestURL().toString();
                path = path.substring(0, path.indexOf("/api") + 4);
                //System.out.println("path is: " + path);
                try {
                    if(emailServiceManager.sendResetPasswordMail(path, user)) {
                        eventManager.logEvent(user, Event.Operation.REQUEST_PASSWORD_RESET, null);
                        return new ResponseEntity<>(new ApiResponse(true, user.getEmail()),
                                HttpStatus.OK);
                    }
                    else {
                        return new ResponseEntity<>(new ApiResponse(false, "Unable to send email."),
                                HttpStatus.BAD_REQUEST);
                    }
                } catch (Exception e) {
                    return new ResponseEntity<>(new ApiResponse(false, "Unable to send email."),
                            HttpStatus.REQUEST_TIMEOUT);
                }
            }
        }
        else {
            return new ResponseEntity<>(new ApiResponse(false, "No such username or password."),
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Endpoint to change a user's password.
     * @param updatePasswordRequest the password request including old and new password.
     * @param jwt JWT token to authenticate the user before resetting the password.
     * @return http response entity.
     */
    @PutMapping("/changepassword")
    public ResponseEntity<?> changeUserPassword(@Valid @RequestBody UpdatePasswordRequest updatePasswordRequest,
                                                @RequestHeader ("Authorization") String jwt) {

        String[] values = jwt.split(" ");
        if(tokenProvider.getUsernameFromJWT(values[1]).equals(updatePasswordRequest.getUserName())) {
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(updatePasswordRequest.getUserName(),
                                updatePasswordRequest.getOldPassword()));
                User user = userManager.resetPassword(updatePasswordRequest.getUserName(), passwordEncoder.encode(updatePasswordRequest.getNewPassword()));
                if(user != null) {
                    eventManager.logEvent(jwt, Event.Operation.PASSWORD_CHANGE, user);
                    return new ResponseEntity<>(new ApiResponse(true, "success"), HttpStatus.OK);
                }
                else {
                    return new ResponseEntity<>(new ApiResponse(false, "Error updating password."), HttpStatus.BAD_REQUEST);
                }
            } catch (BadCredentialsException e) {
                return new ResponseEntity<>(new ApiResponse(false, "Invalid current password."), HttpStatus.BAD_REQUEST);
            }
        }
        else {
            return new ResponseEntity<>(new ApiResponse(false, "Provided username does not match."), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Endpoint user to update user information.
     * @param userId user id.
     * @param userUpdate updated user information.
     * @param jwt JWT Token for user authentication
     * @return http response entity.
     */
    @PutMapping(path = "/{userId}")
    public ResponseEntity<?> updateUserInformation(@PathVariable int userId,
                                                   @Valid @RequestBody UpdateUserRequest userUpdate,
                                                   @RequestHeader ("Authorization") String jwt ) {
        Optional<User> userOptional = userManager.getUserById(userId);
        if(userOptional.isPresent()) {
            User oldUser = userOptional.get();
            String[] values = jwt.split(" ");
            if(tokenProvider.getUsernameFromJWT(values[1]).equals(oldUser.getUsername())) {
                try {
                    User updatedUser = userManager.updateUserInformation(oldUser, userUpdate);
                    if(updatedUser != null) {
                        eventManager.logEvent(jwt, Event.Operation.MODIFIED, updatedUser);
                        return ResponseEntity.ok(updatedUser);
                    }
                    else {
                        return new ResponseEntity<>(new ApiResponse(false, "Unable to update user information"), HttpStatus.NOT_MODIFIED);
                    }
                } catch (Exception e) {
                    return new ResponseEntity<>(new ApiResponse(false, "Unable to update user information"), HttpStatus.NOT_MODIFIED);
                }
            }
            else {
                return new ResponseEntity<>(new ApiResponse(false, "Provided userId does not match credentials."), HttpStatus.BAD_REQUEST);
            }
        }
        else {
            return new ResponseEntity<>(new ApiResponse(false, "Invalid userId."), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Endpoint to refresh JWT token
     * @param request Http Servlet request
     * @return http response entity.
     */
    @GetMapping(path = "/refresh")
    public ResponseEntity<?> refreshAndGetAuthenticationToken(HttpServletRequest request) {
        String authToken = request.getHeader("Authorization");
        final String token = authToken.substring(7);

        if (tokenProvider.canTokenBeRefreshed(token)){
            String refreshedToken = tokenProvider.refreshToken(token);
            return ResponseEntity.ok(new JwtRefreshResponse(refreshedToken));
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }

    }
}

