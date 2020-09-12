package com.asylumproject.asylumproject.manager;
import com.asylumproject.asylumproject.broker.AccountBroker;
import com.asylumproject.asylumproject.payload.UpdateUserRequest;
import com.asylumproject.asylumproject.permission.Permission;
import com.asylumproject.asylumproject.permission.PermissionName;
import com.asylumproject.asylumproject.problemdomain.User;
import com.asylumproject.asylumproject.reports.requests.utils.ReportUsers;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Class description: This class takes care of creating, editing, and deleting users. The operations in this class
 * are accessed only by Users with the Permission.SYSTEM_ADMIN permission set to true.
 */
@Service
public class UserManager implements UserDetailsService {

    private AccountBroker accountBroker;

    @Autowired
    public UserManager(AccountBroker accountBroker){
        this.accountBroker = accountBroker;
    }

    /**
     * Edits the attributes of a User which already exists in the system.
     * Attributes that can be edited:
     * username, first name, last name, email address, default language, photo.
     * @param oldUser the User object that will be edited
     * @param newUser the new User data to be saved
     * @return the modified User object
     */
    public User updateUserInformation(User oldUser, UpdateUserRequest newUser) {

            oldUser.setFirstName(getUpdatedValue(oldUser.getFirstName(), newUser.getFirstName()));
            oldUser.setLastName(getUpdatedValue(oldUser.getLastName(), newUser.getLastName()));
            oldUser.setPhoneNumber(getUpdatedValue(oldUser.getPhoneNumber(), newUser.getPhoneNumber()));
            oldUser.setDefaultLanguage(getUpdatedValue(oldUser.getDefaultLanguage(), newUser.getDefaultLanguage()));
            oldUser.setPhotoPath(getUpdatedValue(oldUser.getPhotoPath(), newUser.getPhotoPath()));

            oldUser.setEnabled(newUser.isEnabled());

            //Spring Security authorities
            List<GrantedAuthority> authorities = new ArrayList<>();
            //Create a Role for each Permission the User has
            for (Permission permission : newUser.getPermissions()) {
                SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(permission.getName().name());
                authorities.add(simpleGrantedAuthority);
            }

            oldUser.setPermissionList(newUser.getPermissions());
            //Apply the Roles to the User
            oldUser.setAuthorities(authorities);

        try {
                return accountBroker.updateUserInfo(oldUser);
            }catch (IllegalArgumentException e) {
                return null;
            }
    }

    /**
     * Validate if the "new" value matches the "old" value and returns the final value.
     * @param oldValue old value.
     * @param newValue new value.
     * @return the final value.
     */
    private String getUpdatedValue(String oldValue, String newValue) {
        return (newValue != null && !newValue.trim().equals("")) ? newValue: oldValue;
    }

    /**
     * Adds a new User to the system.
     * A new object of type User will be created and specific site permissions will be applied to the new User based
     * on the type of User that was selected to be created on the System Administration screen. Then, the user will be
     * saved to the system.
     * @param newUser the new User object to register to the system
     * @return the User that was added successfully to the system
     */
    public User addUser(User newUser){

        //Spring Security authorities
        List<GrantedAuthority> authorities = new ArrayList<>();
        //Create a Role for each Permission the User has
        for (Permission permission : newUser.getPermissions()) {
            SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(permission.getName().name());
            authorities.add(simpleGrantedAuthority);
        }
        //Apply the Roles to the User
        newUser.setAuthorities(authorities);
        return accountBroker.registerUser(newUser);
    }

    /**
     * Delete a user.
     * @param user user to delete.
     */
    public void deleteUser(User user){
        accountBroker.deleteUser(user);
    }

    /**
     * Allows the User to change their password to a new one. The old password must be verified with the db before the
     * new password can be set.
     * @param userName the User's user name.
     * @param newPassword the User's new password. The new password must be between 8 and 12 characters.
     * @return true if the password was successfully changed, otherwise false
     */
    public User resetPassword(String userName, String newPassword){

        Optional<User> userOpt = accountBroker.getUserByUserName(userName);
        if(userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(newPassword);
            return accountBroker.resetPassword(user);
        }
        return null;
    }

    /**
     * Retrieves a User object based on a provided id.
     * @param userId the username for which a User object is needed
     * @return the User object corresponding to the provided username. Return null if no such username exists.
     */
    public Optional<User> getUserById(int userId) {
        return accountBroker.getUserById(userId);
    }

    /**
     * Retrieves a list of all User objects in the system.
     * @return a list of all User objects. Return null if there are no users.
     */
    public List<User> getUsers(){
        return accountBroker.getAllUsers();
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        //Allow login with username or email
        Optional<User> userOptional = accountBroker.findByUserNameOrEmailAddress(usernameOrEmail, usernameOrEmail);
        if(userOptional.isPresent()) {
            User user = userOptional.get();
            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    true,
                    true,
                    true,
                    true,
                    getAuthorities(user.getPermissions()));
        }
        else {
            return null;
        }
    }

    /**
     * Validates if a username is in use.
     * @param userName username to validate.
     * @return true if exists, otherwise false.
     */
    public boolean checkUserNameExists(String userName) {
        return accountBroker.checkUserNameExists(userName);
    }

    /**
     * Validates if an email address is in use.
     * @param email email address to validate.
     * @return true if exists, otherwise false.
     */
    public boolean checkEmailExists(String email) {
        return accountBroker.checkEmailExists(email);
    }

    /**
     * Retrieve a list of granted authorities based on a set of permissions.
     * @param permissions a set of permissions.
     * @return a list of granted authorities.
     */
    private static List<GrantedAuthority> getAuthorities (Set<Permission> permissions) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Permission permission: permissions) {
            authorities.add(new SimpleGrantedAuthority(permission.getName().name()));
        }
        return authorities;
    }

    /**
     * Retrieves a user object based on the username.
     * @param username username to search for.
     * @return the user object, null if not found.
     */
    public Optional<User> getUserByUserName(String username) {
        return accountBroker.getUserByUserName(username);
    }

    /**
     * Retrieve a User based on the username or email information.
     * @param userNameOrEmail user's username or email.
     * @return user of null if not found.
     */
    public Optional<User> getUserByUserNameOrEmail(String userNameOrEmail) {
        return accountBroker.findByUserNameOrEmailAddress(userNameOrEmail, userNameOrEmail);
    }

    /**
     * Retrieves a user id based on the username.
     * @param username username to search for.
     * @return user id. -1 if not found.
     */
    public int getUserIdByUserName(String username) {
        Optional<User> user = accountBroker.getUserByUserName(username);
        return user.map(User::getID).orElse(-1);
    }

    /**
     * Sets a UUID to a user.
     * @param user the user.
     * @return the user object.
     */
    public User setUserUUID(User user) {
        return accountBroker.setUserUUID(user);
    }

    /**
     * Build user's datasheet for exporting user's information.
     * @param reportUsers users to include in the data sheet.
     * @param sheet data sheet where data is loaded to.
     * @param dateTimeFormatter date time format.
     */
    public void buildUserDataSheet(ReportUsers reportUsers, Sheet sheet, DateTimeFormatter dateTimeFormatter) {

//        List<User> users = accountBroker.getAllUsersAdmin();
        List<User> users = accountBroker.getAllUsers();

        Row headerRow = sheet.createRow(0);

        int cellNum = 0;
        headerRow.createCell(cellNum++).setCellValue("ID");
        headerRow.createCell(cellNum++).setCellValue("Username");
        headerRow.createCell(cellNum++).setCellValue("First Name");
        headerRow.createCell(cellNum++).setCellValue("Last Name");
        headerRow.createCell(cellNum++).setCellValue("Email Address");
        headerRow.createCell(cellNum++).setCellValue("Phone Number");
        headerRow.createCell(cellNum++).setCellValue("Language");
        headerRow.createCell(cellNum++).setCellValue("Creator");
        headerRow.createCell(cellNum++).setCellValue("Create Date Time");
        headerRow.createCell(cellNum++).setCellValue("Last Update Date Time");
        headerRow.createCell(cellNum++).setCellValue("Deleted");
        headerRow.createCell(cellNum++).setCellValue("Site User");
        headerRow.createCell(cellNum++).setCellValue("Teacher");
        headerRow.createCell(cellNum++).setCellValue("Content Curator");
        headerRow.createCell(cellNum++).setCellValue("System Administrator");
        headerRow.createCell(cellNum++).setCellValue("Photo Path");

        int rowNum = 1;
        for(User u: users) {
            Row row = sheet.createRow(rowNum++);
            int index = 0;
            row.createCell(index++).setCellValue(u.getID());
            row.createCell(index++).setCellValue(u.getUsername());
            row.createCell(index++).setCellValue(u.getFirstName());
            row.createCell(index++).setCellValue(u.getLastName());
            row.createCell(index++).setCellValue(u.getEmail());
            row.createCell(index++).setCellValue(u.getPhoneNumber());
            row.createCell(index++).setCellValue(u.getDefaultLanguage());
            row.createCell(index++).setCellValue(u.getCreatorUserName());
            row.createCell(index++).setCellValue(dateTimeFormatter.format(u.getCreateDateTime().toLocalDateTime()));
            row.createCell(index++).setCellValue(dateTimeFormatter.format(u.getUpdateDateTime().toLocalDateTime()));
            row.createCell(index++).setCellValue(u.isDeleted());
            Iterator<Permission> permissions = u.getPermissions().iterator();
            ArrayList<String> perms = new ArrayList<>();
            while(permissions.hasNext()) {
                perms.add(permissions.next().getName().name());
            }
            row.createCell(index++).setCellValue(perms.contains(PermissionName.ROLE_SITE_USER.name())? "YES": "");
            row.createCell(index++).setCellValue(perms.contains(PermissionName.ROLE_TEACHER.name())? "YES": "");
            row.createCell(index++).setCellValue(perms.contains(PermissionName.ROLE_CONTENT_CURATOR.name())? "YES": "");
            row.createCell(index++).setCellValue(perms.contains(PermissionName.ROLE_SYSTEM_ADMIN.name())? "YES": "");
            row.createCell(index).setCellValue(u.getPhotoPath());
        }

        for(int i = 0; i < cellNum; i++) {
            sheet.autoSizeColumn(i);
        }

    }

    /**
     * Retrieve a list of all soft-deleted users.
     * @return a list of all soft-deleted users.
     */
    public List<User> getDeletedUsers() {
        return accountBroker.getDeletedUsers();
    }

}
