package com.asylumproject.asylumproject.manager;

import com.asylumproject.asylumproject.broker.AccountBroker;
import com.asylumproject.asylumproject.broker.ContentBroker;
import com.asylumproject.asylumproject.broker.ContentElementBroker;
import com.asylumproject.asylumproject.problemdomain.User;
import com.asylumproject.asylumproject.reports.ExistingUsers;
import com.asylumproject.asylumproject.reports.ReportElement;
import com.asylumproject.asylumproject.reports.dropbox.DBoxManager;
import com.asylumproject.asylumproject.reports.requests.ListUsernames;
import com.asylumproject.asylumproject.reports.requests.ReportDataRequest;
import com.asylumproject.asylumproject.reports.responses.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages creation of Content user traffic, viewing traffic reports, and sharing traffic reports.
 */
@Service
public class ReportManager{

    private UserManager userManager;
    private AccountBroker accountBroker;
    private EventManager eventManager;
    private ContentBroker contentBroker;
    private ContentElementBroker contentElementBroker;


    @Autowired
    public ReportManager(UserManager userManager, ContentElementBroker contentElementBroker,
                         EventManager eventManager, ContentBroker contentBroker,
                         AccountBroker accountBroker){
        this.userManager = userManager;
        this.eventManager = eventManager;
        this.contentBroker = contentBroker;
        this.accountBroker = accountBroker;
        this.contentElementBroker = contentElementBroker;
    }

    /**
     * Retrieves a list of report elements containing the number of content objects.
     * @return list of report elements.
     */
    public List<ReportElement> getReportNumContents() {
        return contentBroker.getReportExistingContents();
    }

    /**
     * Retrieves the number of existing users.
     * @return existing users.
     */
    public ExistingUsers getReportTotalUsers() {
        return accountBroker.getReportExistingUsers();
    }

    /**
     * Retrieves the report containing the number of users per language.
     * @return number of users per language.
     */
    public List<ReportElement> getReportUsersPerLanguage() {
        return accountBroker.getReportUsersPerLanguage();
    }

    /**
     * Builds the raw data report for download.
     * @param request the report request containing the report filters.
     * @return report data.
     */
    public String getReportData(ReportDataRequest request) {

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy (hh:mm:ss a)");

        Workbook workbook = new XSSFWorkbook();

        if(request.getReportUsers().getInclude()) {
            Sheet sheet = workbook.createSheet("Users");
            userManager.buildUserDataSheet(request.getReportUsers(), sheet, dateTimeFormatter);
        }
        if(request.getReportStories().getInclude()) {
//            Sheet sheet = workbook.createSheet("Stories"); TODO
//            buildStoriesDataSheet(request.getReportStories(), sheet, dateTimeFormatter);
        }
        if(request.getReportResources().getInclude()) {
//            Sheet sheet = workbook.createSheet("Resources"); TODO
//            buildResourcesDataSheet(request.getReportResources(), sheet, dateTimeFormatter);
        }
        if(request.getReportFaqs().getInclude()) {
//            Sheet sheet = workbook.createSheet("FAQs"); TODO
//            buildFaqsDataSheet(request.getReportFaqs(), sheet, dateTimeFormatter);
        }
        if(request.getReportEvents().getInclude()) {
            Sheet sheet = workbook.createSheet("Events");
            eventManager.buildEventsDataSheet(request.getReportEvents(), sheet, dateTimeFormatter);
        }

        DateTimeFormatter form = DateTimeFormatter.ofPattern("yyyyMMddhhmmssa");
        String path = "src/main/resources/static/tempFiles/";
        String fileName = "REPORT" + form.format((new Timestamp(System.currentTimeMillis()).toLocalDateTime())) + ".xlsx";

        try {
            FileOutputStream out = new FileOutputStream(path + fileName);
            workbook.write(out);
            out.close();
//            workbook.close();

            File uploadFile = new File(path + fileName);
            return DBoxManager.uploadFile(DBoxManager.FileType.REPORT, uploadFile, fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Retrieves the number of existing content elements.
     * @return existing content elements.
     */
    public List<ReportElement> getReportNumElements() {
        return contentElementBroker.getReportExistingElements();
    }

    /**
     * Retrieves the space in memory used by content element type (audio, video, images, text).
     * @return space in memory used by content element type.
     */
    public PieData getStorageReport() {
        return  new PieData(contentElementBroker.getStorageReport());
    }

    /**
     * Retrieves the number of stories per content curator.
     * @return number of stories per content curator.
     */
    public BarStoriesPerCurator getStoriesPerCurator() {
        return new BarStoriesPerCurator(contentBroker.getStoriesPerCurator());
    }

    /**
     * Retrieves the number of stories based on their status (Draft, Prepublished, Published, Archived).
     * @return number of stories based on their status.
     */
    public PieData getStoriesPerStatus() {
        return new PieData(contentBroker.getStoriesPerStatus());
    }

    /**
     * Retrieves the number of stories per language (to be used in a pie chart).
     * @return number of stories per language.
     */
    public PieData getStoriesPerLanguage() {
        return new PieData(contentBroker.getStoriesPerLanguage());
    }

    /**
     * Retrieves the number of stories per country.
     * @return number of stories per country.
     */
    public BarStoriesPerCountry getStoriesPerCountry() {
        return new BarStoriesPerCountry(contentBroker.getStoriesPerCountry());
    }

    /**
     * Retrieve a list containing all traffic data.
     * @return a list containing all traffic data.
     */
    public List<TrafficData> getTrafficData() {
        List<TrafficData> data = new ArrayList<>();
        data.add(new TrafficData());
        return data;
    }

    /**
     * Retrieve a list containing all users including the soft-deleted.
     * @return a list containing all users including the soft-deleted.
     */
    public List<User> getAllUsers() {
        return accountBroker.getAllUsersAdm();
    }
}
