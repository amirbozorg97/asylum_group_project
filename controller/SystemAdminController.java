package com.asylumproject.asylumproject.controller;

import com.asylumproject.asylumproject.manager.*;
import com.asylumproject.asylumproject.problemdomain.User;
import com.asylumproject.asylumproject.reports.*;
import com.asylumproject.asylumproject.reports.requests.ReportDataRequest;
import com.asylumproject.asylumproject.reports.responses.*;
import com.asylumproject.asylumproject.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServlet;
import javax.validation.Valid;
import java.util.*;

/**
 * This class handles all requests to change and view data having to do with System Administration.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class SystemAdminController extends HttpServlet {

    private ReportManager reportManager;
    private BackupManager backupManager;
    private UserManager userManager;
    private JwtTokenProvider tokenProvider;
    private EventManager eventManager;

    @Autowired
    public SystemAdminController (ReportManager reportManager,
                                  BackupManager backupManager,
                                  UserManager userManager,
                                  JwtTokenProvider tokenProvider,
                                  EventManager eventManager){
        this.reportManager = reportManager;
        this.backupManager = backupManager;
        this.userManager = userManager;
        this.tokenProvider = tokenProvider;
        this.eventManager = eventManager;
    }

    /**
     * Endpoint to retrieve all traffic report information (formatted for a line chart report)
     * @return all traffic report data.
     */
    @GetMapping (path = "/reports/traffic")
    public ResponseEntity<List<TrafficData>> getTrafficData() {
        return new ResponseEntity<>(reportManager.getTrafficData(), HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve the total number of users and the number of users per permission.
     * @return total number of users and the number of users per permission.
     */
    @GetMapping (path="/reports/users")
    public ResponseEntity<ExistingUsers> getUsersReport() {
        return new ResponseEntity<>(reportManager.getReportTotalUsers(), HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve a list with all users including the soft-deleted.
     * @return a list with all users.
     */
    @GetMapping (path = "/reports/all_users")
    public ResponseEntity<List<User>> getAllUsers() {
        return new ResponseEntity<>(reportManager.getAllUsers(), HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve a list of users based on a list of usernames.
     * @param listUsernames the list of usernames to retrieve information for.
     * @return a list of users.
     */
    @PostMapping (path = "/reports/filter_users")
    public ResponseEntity<List<UserEvent>> getFilteredUsers (@Valid @RequestBody List<String> listUsernames) {
        return new ResponseEntity<>(eventManager.getFilteredEventsUsers(listUsernames), HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve a list of all the soft-deleted users.
     * @return list of soft-deleted users.
     */
    @GetMapping(path = "/reports/deleted_users")
    public ResponseEntity<List<User>> getDeletedUsers() {
        return new ResponseEntity<>(userManager.getDeletedUsers(), HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve the number of content elements by type (audio, video, image, text).
     * @return number of content elements by type.
     */
    @GetMapping (path = "/reports/content_elements")
    public ResponseEntity<List<ReportElement>> getElementsReport() {
        return new ResponseEntity<>(reportManager.getReportNumElements(), HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve the number of contents by type (story, faq, resource).
     * @return number of contents by type.
     */
    @GetMapping (path = "/reports/contents")
    public ResponseEntity<List<ReportElement>> getContentsReport() {
        return new ResponseEntity<>(reportManager.getReportNumContents(), HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve the memory usage (in KB) per content element type (audio, video, image, text)
     * @return memory usage (in KB) per content element type.
     */
    @GetMapping (path = "/reports/storage")
    public ResponseEntity<PieData> getStorageReport() {
        return new ResponseEntity<>(reportManager.getStorageReport(), HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve the number of users per language.
     * @return number of users per language.
     */
    @GetMapping (path = "/reports/user_language")
    public ResponseEntity<List<ReportElement>> getUsersPerLanguage() {
        return new ResponseEntity<>(reportManager.getReportUsersPerLanguage(), HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve the raw data to be downloaded in excel format.
     * @param reportDataRequest the report data request that includes filters.
     * @return path to the raw data to be downloaded in excel format.
     */
    @GetMapping (path = "/reports/data")
    public ResponseEntity<String> getUsersData(@Valid @RequestBody ReportDataRequest reportDataRequest) {
        String link = reportManager.getReportData(reportDataRequest);
        return new ResponseEntity<>(link, !link.equals("")? HttpStatus.OK: HttpStatus.CONFLICT);
    }

    /**
     * Endpoint used to retrieve the number of stories per content curator.
     * @return number of stories per content curator.
     */
    @GetMapping (path = "/reports/curator_stories")
    public ResponseEntity<BarStoriesPerCurator> getStoriesPerCurator() {
        return new ResponseEntity<>(reportManager.getStoriesPerCurator(), HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve the number of stories by status (draft, prepublished, published, archived)
     * @return number of stories by status.
     */
    @GetMapping (path = "/reports/story_status")
    public ResponseEntity<PieData> getStoriesPerStatus() {
        return new ResponseEntity<>(reportManager.getStoriesPerStatus(), HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve the number of stories by country.
     * @return number of stories by country.
     */
    @GetMapping (path = "/reports/story_country")
    public ResponseEntity<BarStoriesPerCountry> getStoriesPerCountry() {
        return new ResponseEntity<>(reportManager.getStoriesPerCountry(), HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve the number of stories by language.
     * @return number of stories by language.
     */
    @GetMapping (path = "/reports/story_language")
    public ResponseEntity<PieData> getStoriesPerLanguage() {
        return new ResponseEntity<>(reportManager.getStoriesPerLanguage(), HttpStatus.OK);
    }

    @GetMapping (path = "/events/stories")
    public ResponseEntity<StoryReport> getStoriesEvents() {
        return new ResponseEntity<>(eventManager.getStoriesEvents(), HttpStatus.OK);
    }

    /**
     * Endpoint to request a database backup to be generated and uploaded to a cloud based storage service and
     * to the user's email address if selected.
     * @param backupRequest backup request indicating if a copy will be sent to the user's email address.
     * @param authorization JWT token to authenticate the user.
     * @return the path where the backup file has been saved and a confirmation if the email was sent.
     */
    @PostMapping (path = "/backup")
    public ResponseEntity<BackupGenerateResponse> generateBackupFile(@Valid @RequestBody BackupRequest backupRequest,
                                                                     @RequestHeader ("Authorization") String authorization) {
        String jwt = authorization.substring(7);
        Optional<User> user = userManager.getUserByUserName(tokenProvider.getUsernameFromJWT(jwt));
        if(user.isPresent()) {
            BackupGenerateResponse response = backupManager.generateBackupFile(user.get(), backupRequest);
            if(response != null)
                return new ResponseEntity<>(response, HttpStatus.OK);
            else
                return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }
        else
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    /**
     * Endpoint to request a backup file is retrieved from the cloud based storage service to the server.
     * @param path the path to the backup file to be retrieved.
     * @return Http Status.
     */
    @GetMapping (path = "/retrieve")
    public ResponseEntity<Void> zipFile(@RequestParam String path) {
        return new ResponseEntity<>(backupManager.restoreDB(path) ? HttpStatus.OK : HttpStatus.CONFLICT);
    }

}
