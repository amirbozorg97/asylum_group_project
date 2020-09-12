package com.asylumproject.asylumproject.controller;

import com.asylumproject.asylumproject.manager.ContentManager;
import com.asylumproject.asylumproject.problemdomain.Content;
import com.asylumproject.asylumproject.problemdomain.ShortenedUrl;
import com.asylumproject.asylumproject.problemdomain.Story;
import com.asylumproject.asylumproject.problemdomain.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles all requests for the home page and shareable link requests
 */
@RestController
@CrossOrigin
public class HomeController extends HttpServlet {

    private static final String BASE_URL = "https://pacific-caverns-19608.herokuapp.com";

    private ContentManager contentManager;

    @Autowired
    HomeController(ContentManager contentManager){
        this.contentManager = contentManager;
    }

    /**
     * Requests from a user to create a shareable link will use this method. The shareable link is returned in a ShortenedUrl object.
     * @param tagIds the IDs of the content tags that are requested to be filtered by the user
     * @return the ShortenedUrl object that contains the shareable link
     */
    @PostMapping(path = "/stories/filtered")
    public ResponseEntity<ShortenedUrl> getShareLink(@RequestBody List<Integer> tagIds) {
        //save tagIds in db with random string
        ShortenedUrl shortened = contentManager.createShortenedUrl(tagIds);
        if (shortened != null){
            return new ResponseEntity<>(shortened, HttpStatus.OK);
        }else{
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Returns a list of Story objects that don't contain the specified content tags.
     * @param randomString a randomized string that corresponds to a list of content tags
     * @return the filtered list of Story objects
     */
    @GetMapping(path = "/api/content/filtered/{randomString}")
    public ResponseEntity<List<Story>> getFilteredStories(@PathVariable("randomString") String randomString){
        ShortenedUrl shortened = contentManager.getShortenedUrl(randomString);
        //Get list of filtered Stories
        ArrayList<Tag> tags = new ArrayList<>();
        for (Integer tagId : shortened.getTagIds()){
           Tag t = contentManager.getTagByID(tagId);
           tags.add(t);
        }
        if (tags.size() > 0){
            List<Story> stories = contentManager.getFilteredStories(tags);
            return new ResponseEntity<>(stories, HttpStatus.OK);
        }

        return new ResponseEntity<List<Story>>(HttpStatus.NO_CONTENT);
    }

    /** Method used to redirect from port 8080 to the client application on port 3000.
     * @param response
     * @param randomString
     * @throws IOException
     */
    @GetMapping(path = "/share/{randomString}")
    public void redirectShareLink(HttpServletResponse response, @PathVariable("randomString") String randomString) throws IOException {

        System.out.println("redirect");
        response.sendRedirect("/?share=" + randomString);
    }
}