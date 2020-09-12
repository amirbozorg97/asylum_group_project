package com.asylumproject.asylumproject.manager;

import java.util.ArrayList;

import com.asylumproject.asylumproject.problemdomain.StudyClass;
import org.springframework.stereotype.Service;

/** 
 * Basic service for managing StudyClass objects. 
 * It creates a URL when filtered contents are retrieved, and creates a temporary activity page with the URL.
 * 
 * @author CapstoneAwesome
 */
@Service
public class StudyClassManager {
	
	/**
	 * The list of study classes created by a teacher.
	 * A teacher can create multiple study classes and it keeps track of all of the study classes a teacher created as a list.
	 * Valid data type of the list is StudyClass.
	 */
	private ArrayList<StudyClass> studyClassList;
	
	/**
	 * Creates a URL for when a teacher creates a study class.
	 * The URL allows the teacher to invite students to a created session.
	 * A temporary URL has a hashed suffix so that a session can be uniquely indentified.
	 * Valid string format: MD5 hashed string.
	 * @return The URL with hashed suffix 
	 */
	public String createURL() {
		return "Site url" + "MD5 hashed string";
	}
	
	/**
	 * Creates a study class representing a teacher classroom session.
	 * It invokes filter manager so that filter settings set by teacher can be applied to all connected students.
	 * Once filtered contents are retrieved by the filter manager,
	 * it reflexively calls a createURL method in order to create a URL with hashed suffix.
	 * @return the newly created StudyClass
	 */
	public StudyClass openStudyClass() {
		return null;
	}

}
