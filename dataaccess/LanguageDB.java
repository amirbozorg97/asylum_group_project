package com.asylumproject.asylumproject.dataaccess;


import com.asylumproject.asylumproject.problemdomain.Language;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface LanguageDB extends CrudRepository<Language, Integer> {

    /**
     * Retrieve a list containing all languages.
     * @return a list of all languages.
     */
    List<Language> findAll();

    /**
     * Retrieve one language based on it's id.
     * @param code the language id.
     * @return a language that matches the provided language id.
     */
    Language findByCode(String code);


}
