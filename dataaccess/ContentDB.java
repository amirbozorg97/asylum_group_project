package com.asylumproject.asylumproject.dataaccess;

import com.asylumproject.asylumproject.reports.ReportElement;
import com.asylumproject.asylumproject.problemdomain.Content;
import com.asylumproject.asylumproject.problemdomain.Story;
import com.asylumproject.asylumproject.problemdomain.Tag;
import com.asylumproject.asylumproject.reports.responses.ReportCountResult;
import com.asylumproject.asylumproject.reports.responses.ReportStoriesPerCountry;
import com.asylumproject.asylumproject.reports.responses.ReportStoriesPerCurator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;

import javax.persistence.Tuple;
import java.util.List;

@Repository
public interface ContentDB extends JpaRepository<Content, Integer> {

    /**
     * Retrieve a list containing all Stories that match the filter criteria.
     * @param tag the tags to match when searching for Stories
     * @return a list of all Stories that match the filter criteria.
     */
    List<Content> findAllByTags(Tag tag);

    @Override
    @Query("SELECT c FROM Content c WHERE c.deleted = false")
    List<Content> findAll();

    @Query("SELECT c FROM Content c WHERE c.deleted = true")
    List<Content> recycleBin();

    /**
     * Retrieve one Content based on it's id.
     * @param contentId the content id.
     * @return a content that matches the provided content id.
     */
    Content findByContentId(int contentId);

    List<Content> findByDeletedFalse();

    Iterable<Story> getAllByDtypeAndDeletedFalse(String dtype);

    Iterable<Story> findAllByDtypeAndStateAndDeletedFalse(String dtype, Story.State state);

    @Query("SELECT " +
            "    new com.asylumproject.asylumproject.reports.ReportElement(v.dtype, COUNT(v)) " +
            "FROM " +
            "    Content v " +
            "WHERE v.deleted = false " +
            "GROUP BY " +
            "    v.dtype")
    List<ReportElement> findElementsReport();

    @Query(value = "CALL numStoriesByState();", nativeQuery = true)
    List<Tuple> getStoriesPerStatus();

    @Query("SELECT " +
            "   new com.asylumproject.asylumproject.reports.responses.ReportStoriesPerCurator(s.dtype, s.creatorUser, s.state, count(s))" +
            "FROM " +
            "   Story s " +
            "WHERE s.deleted = false " +
            "GROUP BY " +
            "   s.dtype, s.creatorUser, s.state " +
            "HAVING " +
            "   s.dtype = 'Story'")
    List<ReportStoriesPerCurator> findStoriesPerCurator();

    @Query(value = "CALL numStoriesByLanguage();", nativeQuery = true)
    List<Tuple> getStoriesPerLanguage();

    @Query("SELECT " +
            "   new com.asylumproject.asylumproject.reports.responses.ReportStoriesPerCountry(s.dtype, s.countryFull, s.state, count(s))" +
            "FROM " +
            "   Story s " +
            "WHERE s.deleted = false " +
            "GROUP BY " +
            "   s.dtype, s.countryFull, s.state " +
            "HAVING " +
            "   s.dtype = 'Story'" +
            "ORDER BY COUNT(s) DESC")
    List<ReportStoriesPerCountry> findStoriesPerCountry();

}
