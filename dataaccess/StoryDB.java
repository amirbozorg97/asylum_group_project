package com.asylumproject.asylumproject.dataaccess;

import com.asylumproject.asylumproject.problemdomain.Story;
import org.springframework.data.repository.CrudRepository;

public interface StoryDB extends CrudRepository<Story, Integer> {

    /**
     * retrieve a list of stories based on provided state.
     *
     * @param state the state that is used to get stories from database.
     * @return it returns a list of stories.
     */
    Iterable<Story> findAllByState(Story.State state);
}
