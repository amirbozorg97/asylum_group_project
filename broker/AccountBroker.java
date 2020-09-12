package com.asylumproject.asylumproject.broker;


import com.asylumproject.asylumproject.dataaccess.UserDB;
import com.asylumproject.asylumproject.reports.ExistingUsers;
import com.asylumproject.asylumproject.reports.ReportElement;
import com.asylumproject.asylumproject.permission.Permission;
import com.asylumproject.asylumproject.permission.PermissionName;
import com.asylumproject.asylumproject.problemdomain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AccountBroker {

    private UserDB userDB;

    @Autowired
    public AccountBroker(UserDB userDB){
        this.userDB = userDB;
    }

    /**
     * Creates a new user record based on a User object passed as parameter and sets the creator of that new user.
     * @param newUser the User object to add.
     * @return a reference to the User object recently created.
     */
    public User registerUser (User newUser){
        return userDB.save(newUser);
    }

    /**
     * Updates the information of an existing user.
     * updates include:
     *     First name
     *     Last name
     *     email
     *     Phone
     *     Default language
     *     Photo
     *     username
     * @param user the User object to update.
     * @return a reference to the User object updated.
     */
    public User updateUserInfo(User user) {
        return userDB.save(user);
    }

    /**
     * Retrieves a User object based on a provided username.
     * @param id the id used to retrieve a User object.
     * @return the User object that matches the provided id.
     */
    public Optional<User> getUserById(int id) {
        return userDB.findById(id);
    }

    /**
     * Retrieve a user based on the username or email address.
     * @param username username.
     * @param email email address.
     * @return the matching user. Null if not found.
     */
    public Optional<User> findByUserNameOrEmailAddress(String username, String email){
       return userDB.findByUserNameOrEmailAddress(username, email);
    }

    /**
     * Retrieve a list containing all users in the database.
     * @return a list of all Users in the database.
     */
    public List<User> getAllUsers() {
        return (List<User>) userDB.findAll();
    }

    /**
     * Reset the password value for a User.
     * @param user the User object to reset the password value.
     * @return true if successfull reset of password, otherwise false.
     */
    public User resetPassword(User user) {
        return userDB.save(user);
    }

    /**
     * Check if a username exists.
     * @param userName username to check
     * @return true if exists, otherwise false.
     */
    public boolean checkUserNameExists(String userName) {
        return userDB.existsByUserName(userName);
    }

    /**
     * Check if an email address exists.
     * @param email email address to check.
     * @return true if exists, otherwise false.
     */
    public boolean checkEmailExists(String email) {
        return userDB.existsByEmailAddress(email);
    }

    /**
     * Retrieves a user based on a username.
     * @param username the username to look for.
     * @return the matching user, otherwise null.
     */
    public Optional<User> getUserByUserName(String username) {
        return userDB.findByUserName(username);
    }

    /**
     * Delete a user (soft delete).
     * @param user the user to delete.
     */
    public void deleteUser(User user) {
        userDB.delete(user);
    }

    /**
     * Set a UUID to a user.
     * @param user the user.
     * @return the user.
     */
    public User setUserUUID(User user) {
        return userDB.save(user);
    }

    /**
     * Counts number of users based on permissions.
     * @return existing users.
     */
    public ExistingUsers getReportExistingUsers() {
        int totalUsers = 0;
        int numTeachers = 0;
        int numCurators = 0;
        int numSysAdmins = 0;
        Iterable<User> users = userDB.findAll();
        for(User u: users) {
            totalUsers++;
            Set<Permission> permissions = u.getPermissions();
            for(Permission p: permissions) {
                if(p.getName().equals(PermissionName.ROLE_CONTENT_CURATOR))
                    numCurators++;
                if(p.getName().equals(PermissionName.ROLE_SYSTEM_ADMIN))
                    numSysAdmins++;
                if(p.getName().equals(PermissionName.ROLE_TEACHER))
                    numTeachers++;
            }
        }
        return new ExistingUsers(totalUsers, numSysAdmins, numCurators, numTeachers);
    }

    /**
     * Retrieves a list of report elements containing the number of existing users per language.
     * @return a list of the report elements.
     */
    public List<ReportElement> getReportUsersPerLanguage() {
        return userDB.findUsersPerLanguage();
    }

    /**
     * Retrieves a list of all users including those that have been soft-deleted.
     * @return a list of all users.
     */
    public List<User> getAllUsersAdm() {
        return userDB.findAllUsers();
    }

    /**
     * Retrieves a list of all users that have been soft-deleted.
     * @return list of all soft-deleted users.
     */
    public List<User> getDeletedUsers() {
        return userDB.recycleBin();
    }
}
