package com.asylumproject.asylumproject.dataaccess;

import com.asylumproject.asylumproject.reports.ReportElement;
import com.asylumproject.asylumproject.problemdomain.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserDB extends CrudRepository<User, Integer>{

    /**
     * Retrieve a list of all users that were not deleted.
     * @return a list of all users that were not deleted.
     */
    @Override
    @Query("SELECT u FROM User u WHERE u.deleted = false")
    List<User> findAll();

    /**
     * Retrieve a list of all soft-deleted users only.
     * @return a list of all soft-deleted users only.
     */
    @Query("SELECT u FROM User u WHERE u.deleted = true")
    List<User> recycleBin();

    /**
     * Retrieve from database a user based on username or email.
     * @param username username.
     * @param email email.
     * @return matching user or null of not found.
     */
    Optional<User> findByUserNameOrEmailAddress(String username, String email);

    /**
     * Validates if a username exists.
     * @param username the username to validate.
     * @return true if exists, false otherwise.
     */
    Boolean existsByUserName(String username);

    /**
     * Validates if an email address exists.
     * @param email the email address to validate.
     * @return true if exists, false otherwise.
     */
    Boolean existsByEmailAddress(String email);

    /**
     * Retrieve from database a user based on username.
     * @param username username.
     * @return matching user or null if not found.
     */
    Optional<User> findByUserName(String username);

    /**
     * Retrieves the number of users per language.
     * @return number of users per language.
     */
    @Query("SELECT " +
            "    new com.asylumproject.asylumproject.reports.ReportElement(v.defaultLanguage, COUNT(v)) " +
            "FROM " +
            "    User v " +
            "WHERE v.deleted = false " +
            "GROUP BY " +
            "    v.defaultLanguage")
    List<ReportElement> findUsersPerLanguage();

    /**
     * Retrieve a list with all users including the soft-deleted.
     * @return a list with all users including the soft-deleted.
     */
    @Query(value = "CALL findAllUsers();", nativeQuery = true)
    List<User> findAllUsers();

}
