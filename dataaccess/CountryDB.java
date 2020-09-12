package com.asylumproject.asylumproject.dataaccess;


import com.asylumproject.asylumproject.problemdomain.Country;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CountryDB extends CrudRepository<Country, Integer> {

    /**
     * Retrieve a list containing all countries.
     * @return a list of all countries.
     */
    List<Country> findAll();

    /**
     * Retrieve one country based on it's id.
     * @param code the country code.
     * @return a country that matches the provided country code.
     */
    Country findByCode(String code);
}
