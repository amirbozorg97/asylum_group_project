package com.asylumproject.asylumproject.controller;

import com.asylumproject.asylumproject.manager.StudyClassManager;
import com.asylumproject.asylumproject.problemdomain.StudyClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/studyclass")
@CrossOrigin
public class StudyClassController {

    StudyClassManager studyClassManager;

    @Autowired
    StudyClassController (StudyClassManager studyClassManager){
        this.studyClassManager = studyClassManager;
    }

    /**
     * Only users with teacher permission can access this URL.
     * @param studyClass the StudyClass data to create a new StudyClass from
     * @return the new StudyClass object and 200 OK if the class was successfully created
     */
    @GetMapping
    public ResponseEntity<StudyClass> createStudyClass(@RequestBody StudyClass studyClass){
        StudyClass newClass = studyClassManager.openStudyClass();
        return new ResponseEntity<StudyClass>(newClass, HttpStatus.OK);
    }
}
