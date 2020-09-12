package com.asylumproject.asylumproject.manager;

import com.asylumproject.asylumproject.broker.AccountBroker;
import com.asylumproject.asylumproject.broker.EventBroker;
import com.asylumproject.asylumproject.problemdomain.Content;
import com.asylumproject.asylumproject.problemdomain.Event;
import com.asylumproject.asylumproject.problemdomain.EventLog;
import com.asylumproject.asylumproject.problemdomain.User;
import com.asylumproject.asylumproject.reports.requests.utils.ReportEvents;
import com.asylumproject.asylumproject.reports.responses.EventReport;
import com.asylumproject.asylumproject.reports.responses.StoryEvent;
import com.asylumproject.asylumproject.reports.responses.StoryReport;
import com.asylumproject.asylumproject.reports.responses.UserEvent;
import com.asylumproject.asylumproject.security.JwtTokenProvider;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class handles all functionality related to Event logging
 */
@Service
public class EventManager {

    private EventBroker eventBroker;
    private AccountBroker accountBroker;
    private JwtTokenProvider tokenProvider;

    @Autowired
    public EventManager(EventBroker eventBroker,
                        AccountBroker accountBroker,
                        JwtTokenProvider tokenProvider) {
        this.eventBroker = eventBroker;
        this.accountBroker = accountBroker;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Log an event with details about the acting user, the type of operation and the item affected by the event.
     * @param jwt the acting user JWT token.
     * @param operation the type of operation.
     * @param item the item affected by the event.
     */
    public void logEvent(String jwt, Event.Operation operation, Object item) {
        String cleanJwt = jwt.substring(7);
        int userId = tokenProvider.getUserIdFromJWT(cleanJwt);
        Optional<User> actingUser = accountBroker.getUserById(userId);
        actingUser.ifPresent(user -> eventBroker.logEvent(EventLog.getInstance(user, operation, item, null)));
    }

    /**
     * Log an event with details about the acting user, the type of operation and the item affected by the event.
     * @param jwt the acting user JWT token.
     * @param operation the type of operation.
     * @param item the item affected by the event.
     */
    public void logEvent(String jwt, Event.Operation operation, Object item, String uuid) {
        String cleanJwt = jwt.substring(7);
        int userId = tokenProvider.getUserIdFromJWT(cleanJwt);
        Optional<User> actingUser = accountBroker.getUserById(userId);
        actingUser.ifPresent(user -> eventBroker.logEvent(EventLog.getInstance(user, operation, item, uuid)));
    }

    /**
     * Log an event with details about the acting user, the type of operation and the item affected by the event.
     * @param actingUser the acting user.
     * @param operation the type of operation.
     * @param item the item affected by the event.
     */
    public void logEvent(User actingUser, Event.Operation operation, Object item) {
        eventBroker.logEvent(EventLog.getInstance(actingUser, operation, item, null));
    }

    /**
     * Build event's datasheet for exporting event's information.
     * @param reportEvents events to include in the data sheet.
     * @param sheet data sheet where data is loaded to.
     * @param dateTimeFormatter date time format.
     */
    public void buildEventsDataSheet(ReportEvents reportEvents, Sheet sheet, DateTimeFormatter dateTimeFormatter) {
        List<EventLog> events = eventBroker.getAllEvents();
        Row headerRow = sheet.createRow(0);
        int cellNum = 0;
        headerRow.createCell(cellNum++).setCellValue("ID");
        headerRow.createCell(cellNum++).setCellValue("Date Time");
        headerRow.createCell(cellNum++).setCellValue("Acting Username");
        headerRow.createCell(cellNum++).setCellValue("Action");
        headerRow.createCell(cellNum++).setCellValue("Type of Element");
        headerRow.createCell(cellNum++).setCellValue("Element ID");
        int rowNum = 1;
        for(EventLog e: events) {
            Row row = sheet.createRow(rowNum++);
            int index = 0;
            row.createCell(index++).setCellValue(e.getEventId());
            row.createCell(index++).setCellValue(dateTimeFormatter.format(e.getDateTime().toLocalDateTime()));
            row.createCell(index++).setCellValue(e.getActingUser().getName());
            row.createCell(index++).setCellValue(e.getOperation());
            row.createCell(index++).setCellValue(e.getAffectedItem().getClass().getName());
            row.createCell(index).setCellValue(e.getAffectedItem().getClass().getName());
        }
        for(int i = 0; i < cellNum; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Retrieve a list with user event information based on a provided list of usernames.
     * @param listUsernames the list of usrenames to look for.
     * @return a list with user even information.
     */
    public List<UserEvent> getFilteredEventsUsers(List<String> listUsernames) {
        List<EventLog> eventLogList = eventBroker.getAllEvents();
        List<UserEvent> eventDataList = new ArrayList<>();

        for(String user: listUsernames) {
            boolean first = true;
            for(EventLog event: eventLogList) {
                if(event.isUser() && event.getUser().getUsername().equals(user)) {
                    eventDataList.add(EventReport.userEvent(event, first));
                    if(first)
                        first = false;
                }
            }
        }
        return eventDataList;
    }


    /**
     * Retrieve a list of all story event.
     *
     * @return it returns a StoryReport object.
     */
    public StoryReport getStoriesEvents() {
        List<StoryEvent> events = eventBroker.findStoryEvents();
        return new StoryReport(events);
//        List<EventLog> events = eventBroker.getAllEvents();
//        List<StoryEvent> eventDataList = new ArrayList();
//
//        for(EventLog event: events) {
//            if(event.isContent() && ((Content)event.getAffectedItem()).getDtype().equals("Story")) {
//                eventDataList.add(EventReport.storyEvent(event));
////                if(first) first = false;
//            }
//        }
//        return eventDataList;
    }
}
