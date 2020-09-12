package com.asylumproject.asylumproject.broker;

import com.asylumproject.asylumproject.dataaccess.EventLogDB;
import com.asylumproject.asylumproject.problemdomain.EventLog;
import com.asylumproject.asylumproject.reports.responses.StoryEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventBroker {

    private EventLogDB eventLogDB;

    /**
     * A constructor for EventBroker class.
     *
     * @param eventLogDB an object of EventLogDB class.
     */
    @Autowired
    public EventBroker(EventLogDB eventLogDB) {
        this.eventLogDB = eventLogDB;
    }

    /**
     * Log an event based on an Event Log object.
     * @param eventLog he Event Log.
     */
    public void logEvent(EventLog eventLog) {
        eventLogDB.save(eventLog);
    }

    /**
     * Retrieve a list with all the events.
     * @return a list with all the events.
     */
    public List<EventLog> getAllEvents() {
        return eventLogDB.findAll();
    }


    /**
     * Retrieve a list of all story event.
     *
     * @return it returns a list of StoryEvent objects.
     */
    public List<StoryEvent> findStoryEvents() {
        return eventLogDB.findStoryEvents();
    }
}
