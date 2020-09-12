package com.asylumproject.asylumproject.dataaccess;

import com.asylumproject.asylumproject.problemdomain.ShortenedUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShortenedUrlDB extends JpaRepository<ShortenedUrl, Integer> {

    /**
     * retrieve  a ShortenedUrl object from database.
     *
     * @param randomString the string that is used to get the ShortenedUrl object.
     *
     * @return it returns a ShortenedUrl object.
     */
    ShortenedUrl findByRandomString(String randomString);

}
