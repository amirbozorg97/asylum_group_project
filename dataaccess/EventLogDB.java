package com.asylumproject.asylumproject.dataaccess;

import com.asylumproject.asylumproject.problemdomain.EventLog;
import com.asylumproject.asylumproject.reports.responses.StoryEvent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventLogDB extends CrudRepository<EventLog, Integer> {

    /**
     * Retrieve a list with all event logs.
     * @return list with all event logs.
     */
    List<EventLog> findAll();

    @Query("SELECT " +
            "new com.asylumproject.asylumproject.reports.responses.StoryEvent(" +
            "                       s.contentId, " +
            "                       e.dateTime, " +
            "                       e.operation, " +
            "                       e.actingUser.firstName, " +
            "                       e.actingUser.lastName, " +
            "                       s.description, " +
            "                       s.title, " +
            "                       s.asylumSeekerName, " +
            "                       s.state) " +
            "FROM EventLog e LEFT JOIN User u ON e.actingUser = u " +
            "               LEFT JOIN Story s ON s = e.content " +
            "WHERE e.content IS NOT NULL " +
            "AND s.title IS NOT NULL " +
            "AND s.title <> '' " +
            "ORDER BY e.content.contentId, e.dateTime")
    List<StoryEvent> findStoryEvents();

}
