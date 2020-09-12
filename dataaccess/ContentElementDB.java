package com.asylumproject.asylumproject.dataaccess;

import com.asylumproject.asylumproject.problemdomain.Language;
import com.asylumproject.asylumproject.reports.PieReportData;
import com.asylumproject.asylumproject.reports.ReportElement;
import com.asylumproject.asylumproject.problemdomain.ContentElement;
import com.asylumproject.asylumproject.problemdomain.MapPoint;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.Tuple;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;


@Repository
public interface ContentElementDB extends CrudRepository<ContentElement, Integer> {

    /**
     * Retrieve a list containing all ContentElements.
     * @return a list of all ContentElements.
     */
    @Override
    @Query("SELECT c FROM ContentElement c WHERE c.deleted = false ")
    List<ContentElement> findAll();

    /**
     * Retrieve a list containing all ContentElements.
     * @return a list of all ContentElements.
     */
    @Query("SELECT c FROM ContentElement c WHERE c.deleted = true")
    List<ContentElement> recycleBin();

//    List<ContentElement> findAll();

//    List<ContentElement> findAllByMapPointID(int id);

//    List<ContentElement> findAllByFileTypeAndMapPointID(int id, String type);


    /**
     * retrieve a list of content elements based on map point object and state.
     *
     * @param mappoint map point object that is used to get content elements from.
     * @param state the state that is used to get content elements from.
     * @return it returns a list of content elemetns.
     */
    List<ContentElement> findAllByMappointAndStateAndDeletedFalse(MapPoint mappoint, String state);


    /**
     * retrieve a list of content elements based on map point object and state.
     *
     * @param mappoint map point object that is used to get content elements from.
     * @return it returns a list of content elemetns.
     */
    List<ContentElement> findAllByMappointAndDeletedFalse(MapPoint mappoint);


    /**
     * retrieve a list of content elements based on provided fileType and mappoint.
     *
     * @param fileType the fileType that is used to get content elements from database.
     * @param mappoint the mappoint that is used to get content elements from database.
     * @return it returns a list of content elements from database.
     */
    List<ContentElement> findAllByFileTypeAndMappointAndDeletedFalse(@NotBlank String fileType, MapPoint mappoint);


    /**
     * Retrieve one ContentElement based on it's id.
     * @param id the content element id.
     * @return a content element that matches the provided content element id.
     */
    Optional<ContentElement> findById(int id);

    @Query("SELECT " +
            "    new com.asylumproject.asylumproject.reports.ReportElement(v.dtype, COUNT(v)) " +
            "FROM " +
            "    ContentElement v " +
            "GROUP BY " +
            "    v.dtype")
    List<ReportElement> findContentsReport();

    @Query(value = "CALL storageData();", nativeQuery = true)
    List<Tuple> findStorageReport();


    /**
     * check if the passed file exists in the database or not.
     *
     * @param filePath the file path that is checked.
     *
     * @return it returns true if the passed file already exists in the database.
     */
    boolean existsByFilePathAndMappointId(String filePath, int mapPointID);
}
