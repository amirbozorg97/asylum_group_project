package com.asylumproject.asylumproject.dataaccess;

import com.asylumproject.asylumproject.problemdomain.MapPoint;
import com.asylumproject.asylumproject.problemdomain.Story;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface MapPointDB extends CrudRepository<MapPoint, Integer> {

    @Override
    @Query("SELECT m FROM MapPoint m WHERE m.deleted = false")
    List<MapPoint> findAll();

    @Query("SELECT m FROM MapPoint m WHERE m.deleted = true")
    List<MapPoint> recycleBin();

    /**
     * Retrieve a MapPoint based on it's id.
     *
     * @param mapPointID the MapPoint id.
     * @return a MapPoint that matches the provided MapPoint id.
     */
    MapPoint findById(int mapPointID);


    /**
     * retrieve all of the map point objects from database based on story object.
     *
     * @param story the story object that is used to get all map points under that.
     *
     * @return it returns a list of map point objects to the ContentElementManager class.
     */
    List<MapPoint> findAllByStoryAndDeletedFalse(Story story);
}
