package com.asylumproject.asylumproject.dataaccess;

import com.asylumproject.asylumproject.problemdomain.Tag;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TagDB extends CrudRepository<Tag, Integer> {

    /**
     * Retrieve a list containing all tags.
     * @return a list of all tags.
     */
    @Override
    @Query("SELECT t FROM Tag t WHERE t.deleted = false")
    List<Tag> findAll();


    /**
     * Retrieve a list containing all tags.
     *
     * @return a list of all tags.
     */
    @Query("SELECT t FROM Tag t WHERE t.deleted = true")
    List<Tag> recycleBin();

    /**
     * Retrieve one tag based on it's id.
     * @param tagId the tag id.
     * @return a tag that matches the provided tag id.
     */
    Tag findByTagIdAndDeletedFalse(int tagId);

    /**
     * Retrieve a tag based on provided string tag.
     *
     * @param tag the string tag.
     * @return a tag that matches the provided tag string.
     */
    Tag findByTagAndDeletedFalse(String tag);
}
