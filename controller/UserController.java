package com.asylumproject.asylumproject.controller;

import com.asylumproject.asylumproject.manager.EventManager;
import com.asylumproject.asylumproject.manager.UserManager;
import com.asylumproject.asylumproject.payload.*;
import com.asylumproject.asylumproject.problemdomain.Event;
import com.asylumproject.asylumproject.problemdomain.User;
import com.asylumproject.asylumproject.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServlet;
import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * This class handles all requests for editing, creating and deleting users.
 * Creating Users is handled in AuthenticationController
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController extends HttpServlet {

    private UserManager userManager;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider tokenProvider;
    private EventManager eventManager;

    @Autowired
    public UserController(UserManager userManager,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider tokenProvider,
                          EventManager eventManager){
        this.userManager = userManager;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.eventManager = eventManager;
    }

    /**
     * Endpoint to retrieve a user based on a user id.
     * @param userId the user id.
     * @return matching user.
     */
    @GetMapping(path="/{userId}")
    public ResponseEntity<User> getUser(@PathVariable int userId){

        Optional<User> userOpt = userManager.getUserById(userId);
        if (userOpt.isPresent()){
            User user = userOpt.get();
            return new ResponseEntity<>(user, HttpStatus.OK);
        }
        else{
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    /**
     * Endpoint to retrieve a list of all users.
     * @return list of all users.
     */
    @GetMapping
    public ResponseEntity<List<User>> getUsers(){

            List<User> users = userManager.getUsers();
            if (null != users) {
                return new ResponseEntity<>(users, HttpStatus.OK);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * Endpoint to update a user's information.
     * @param userId the user id.
     * @param userUpdate the updated information.
     * @param jwt the JWT token to authenticate the user.
     * @return response entity indicating if update was successful.
     */
    @PutMapping (path="/{userId}")
    public ResponseEntity<?> updateUserInfo(@PathVariable int userId,
                                           @Valid @RequestBody UpdateUserRequest userUpdate,
                                            @RequestHeader ("Authorization") String jwt){

        Optional<User> oldUserOpt = userManager.getUserById(userId);
        if (oldUserOpt.isPresent()) {
            User oldUser = oldUserOpt.get();
            try {
                User updatedUser = userManager.updateUserInformation(oldUser, userUpdate);
                if (updatedUser != null) {
                    eventManager.logEvent(jwt, Event.Operation.MODIFIED, updatedUser);
                    return ResponseEntity.ok(updatedUser);
                }
                else {
                    return new ResponseEntity<>(new ApiResponse(false, "User information not updated."), HttpStatus.NOT_MODIFIED);
                }
            } catch (Exception e) {
                return new ResponseEntity<>(new ApiResponse(false, "Unable to update user information."), HttpStatus.NOT_MODIFIED);
            }
        }
        else {
            return new ResponseEntity<>(new ApiResponse(false, "Provided userId does not exist."), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Endpoint to delete a user.
     * @param userId the user id.
     * @param jwt the JWT token to authenticate the user.
     * @return response entity indicating if update was successful.
     */
    @DeleteMapping (path="/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable int userId,
                                           @RequestHeader ("Authorization") String jwt){
        Optional<User> userOptional = userManager.getUserById(userId);
        if (userOptional.isPresent()){
            User user = userOptional.get();
            userManager.deleteUser(user);
            eventManager.logEvent(jwt, Event.Operation.DELETED, user);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        else{
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }
    }

    /**
     * Endpoint to create a new user.
     * @param signUpRequest the sign up request with information for the new user.
     * @param jwt the JWT token to authenticate.
     * @return response entity indicating if update was successful.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest,
                                          @RequestHeader ("Authorization") String jwt){

        if (userManager.checkUserNameExists(signUpRequest.getUserName())){
            return new ResponseEntity<>(new ApiResponse(false, "Username is already taken!"),
                    HttpStatus.BAD_REQUEST);
        }

        if (userManager.checkEmailExists(signUpRequest.getEmail())){
            return new ResponseEntity<>(new ApiResponse(false, "Email address is already in use!"),
                    HttpStatus.BAD_REQUEST);
        }

        String[] values = jwt.split(" ");
        int creatorId = userManager.getUserIdByUserName(tokenProvider.getUsernameFromJWT(values[1]));

        //Create user account
        signUpRequest.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        Optional<User> userOptional = userManager.getUserById(creatorId);
        userOptional.ifPresent(signUpRequest::setCreator);

        User user = new User(signUpRequest);

        User result = userManager.addUser(user);

        //URI where the User was created. We can delete this if not needed.
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/users/{username}")
                .buildAndExpand(result.getUsername()).toUri();

        eventManager.logEvent(jwt, Event.Operation.CREATED, user);

        return ResponseEntity.created(location).body(new ApiResponse(true, "User registered successfully"));
    }

    /**
     * Endpoint to change a user's password.
     * @param updatePasswordRequest the password update request.
     * @param jwt the JWT token to authenticate the user.
     * @return response entity indicating if update was successful.
     */
    @PutMapping("/changepassword")
    public ResponseEntity<?> changeUserPassword(@Valid @RequestBody UpdatePasswordRequest updatePasswordRequest,
                                                @RequestHeader ("Authorization") String jwt) {

        User user = userManager.resetPassword(updatePasswordRequest.getUserName(), passwordEncoder.encode(updatePasswordRequest.getNewPassword()));

        if(user != null) {
            eventManager.logEvent(jwt, Event.Operation.PASSWORD_CHANGE, user);
            return new ResponseEntity<>(new ApiResponse(true, "success"), HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>(new ApiResponse(false, "Provided username does not exist."), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Endpoint to validate if a username exists.
     * @param userName the username.
     * @return confirmation if the username exists.
     */
    @GetMapping(path = "/username/{userName}")
    public ResponseEntity<Boolean> checkUserName(@PathVariable String userName) {
        return new ResponseEntity<>( userManager.checkUserNameExists(userName), HttpStatus.OK);
    }

    /**
     * Endpoint to validate if an email address exists.
     * @param email the email address.
     * @return confirmation if the email address exists.
     */
    @GetMapping(path = "/email/{email}")
    public ResponseEntity<Boolean> checkEmail(@PathVariable String email) {
        return new ResponseEntity<>( userManager.checkEmailExists(email), HttpStatus.OK);
    }

}
