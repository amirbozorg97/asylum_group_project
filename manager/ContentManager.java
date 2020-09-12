package com.asylumproject.asylumproject.manager;

import com.asylumproject.asylumproject.broker.ContentBroker;
import com.asylumproject.asylumproject.dataaccess.MapPointDB;
import com.asylumproject.asylumproject.problemdomain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class representing the system that manages the various content classes and subclasses
 * 
 * @author CapstoneAwesome
 * 
 */
@Service
public class ContentManager {

	private MapPointDB mapPointDB;
	private ContentBroker contentBroker;

	@Autowired
	public ContentManager(MapPointDB mapPointDB,
						  ContentBroker contentBroker) {
		this.mapPointDB = mapPointDB;
		this.contentBroker = contentBroker;
	}


	private static final String BASE_URL = "https://pacific-caverns-19608.herokuapp.com";

	/**
	 * Arraylist containing objects of content
	 */
	ArrayList<Content> contentList;
	
	/**
	 * Arraylist containing objects of content that have been archived
	 */
	private ArrayList<Content> archivedContentList;
	private ArrayList<Story> storyList;


	/**
	 * Displays a report of site traffic 
	 */
	public void viewTrafficReport() {
	}
	
	/**
	 * Allows rating a story.
	 * 
	 * @param story the story to be rated. Cannot be null.
	 * @return the rated Story object
	 */
	public Content rateStory(Story story) {

		return null;
	}
	
	/**
	 * Edit a piece of content.
	 * 
	 * //@param oldContent the old Content object to be modified
	 * //@param newContent the new Content object to be saved
     * @return the edited Content object
	 */
	/*
	public Content editContent(Content editedContent){
	   //Check each attribute of newContent, if it not null, apply it to the oldContent object and save the new
	   // updated Content object to the DB.
	   // If archived state has changed, call archiveContent();
        return contentBroker.editContent(editedContent);
	}

	 */


	/**
	 * edit passed story object.
	 *
	 * @param oldStory the story object that is updated.
	 * @param newStory the updated story object.
	 *
	 * @return it returns a Story object to the controller class.
	 */
	public Story editStory(Story oldStory, Story newStory){

		//set full country name
		String countryFull = getCountryOfOriginFull(newStory);

		if (countryFull != null){
			oldStory.setCountryFull(countryFull);
		}

		//Check each attribute of newContent, apply it to the oldContent object and save the edited Story object
		oldStory.setState(newStory.getState());
		//not needed -- oldStory.setMapPoints(newStory.getMapPoints());
		oldStory.setAsylumSeekerName(newStory.getAsylumSeekerName());
		oldStory.setContentRating(newStory.getContentRating());
		oldStory.setCountryOfOrigin(newStory.getCountryOfOrigin());
		oldStory.setLanguages(newStory.getLanguages());
		oldStory.setUpdateDateTime(newStory.getUpdateDateTime());
		oldStory.setAvailableDate(newStory.getAvailableDate());
		oldStory.setEndDate(newStory.getEndDate());
		oldStory.setLanguages(newStory.getLanguages());
		oldStory.setContentElements(newStory.getContentElements());
		oldStory.setTags(newStory.getTags());
		oldStory.setDescription(newStory.getDescription());
		oldStory.setTitle(newStory.getTitle());
		oldStory.setDeleted(newStory.isDeleted());

		return contentBroker.editStory(oldStory);
	}


	/**
	 * save passed story object into the database.
	 *
	 * @param story the story object that is saved to the database.
	 *
	 * @return it returns A content object to the controller class.
	 */
	public Content createStory(Story story){
		return contentBroker.createStory(story);
	}

	/**
	 * Submits content out of the drafting state, and makes it ready to be rated (if it's a Story).
	 * 
	 * @param content the content to be submitted. Cannot be null.
	 * @return the submitted Content object
	 */
	public Content submitContent(Content content) {
		return null;
	}
	
	/**
	 * Deletes the currently open content draft.
	 * @return the discarded Content
	 */
	public Content discardDraft() {

		return null;
	}
	
	/**
	 * Publishes the currently open content.
	 * @return the published Content object
	 */
	public Content publish() {
		return null;
	}
	
	/**
	 * Archives target content
	 * 
	 * @param content the content to be archived. Cannot be null.
	 * @return the archived Content object
	 */
	public Content archiveContent(Content content) {
		return null;
	}
	
	/**
	 * Returns a list of archived content. Retrieves all Content objects, and filters out the ones with a state of
	 * "archived".
	 * 
	 * @return list of archived content
	 */
	public ArrayList<Content> listArchivedContent() {
		
		return this.archivedContentList;
		
	}
	
	/**
	 * Un-archives target content.
	 * 
	 * @param content the content to be un-archived. Cannot be null.
	 * @return the unarchived Content
	 */
	public Content unarchiveContent(Content content) {
		return null;
	}
	
	/**
	 * Allows user to create new piece of Content.
	 * @return the created Content
	 */
	public Content createContent() {
		return null;
	}
	
	/**
	 * Returns a list of all content.
	 * 
	 * @return list of content
	 */
	public ArrayList<Content> listContent() {
		
		return this.contentList;
	}

	/**
	 * Returns a list of active (not disabled) content.
	 *
	 * @return list of content
	 */
	public ArrayList<Content> listActiveContent() {

		return this.contentList;
	}

	public Tag createTag(Tag tag){
		return contentBroker.createTag(tag);
	}
	/**
	 * Returns a single Content object.
	 * @param contentId the id of the Content object to return
	 * @return the Content object with the matching contentId
	 */
	public Content getContent(int contentId){
		return contentBroker.getContent(contentId);
	}

	/**
	 * Returns a list of active (not disabled) Story objects.
	 * @return list of Story objects
	 */
	public List<Story> listStories() {
		return contentBroker.getStories();
	}


	/**
	 * Retrieves a collection of Story objects based on a filter that is passed as parameter.
	 *
	 * @return it returns a list of story objects.
	 */
	public List<Story> listPublishedStories(){
		List<Story> allStories = contentBroker.getStories();
		List<Story> published = allStories.stream().filter(s -> s.getState().name().equals("PUBLISHED")).collect(Collectors.toList());
		return published;
	}
	/**
	 * Saves the currently open draft/content. Called by a PUT request from the REST API with the updated Content object.
	 * @return the saved draft Content object
	 */
	public Content saveDraft() {
		return null;
	}
	
	/**
	 * Opens a saved draft. Called by a GET request from the REST API to get the Content object.
	 * 
	 * @param draft the draft/content to be opened. Cannot be null.
	 * @return the Content object to be edited
	 */
	public Content continueDraft(Content draft) {
	    return null;
	}

	/**
	 * Only accessible by a User with SystemAdmin permission. Removes selected content from the
	 * archivedContentList. Deletes content from database LOGICALLY.
	 * @param contentId the content id of the Content object to delete
	 */
	public void deleteContent(int contentId){
		contentBroker.deleteContent(contentId);
	}


	/**
	 * save passed map point object to the database.
	 *
	 * @param mapPoint the map point object that is saved to the database.
	 *
	 * @return it returns a map point object.
	 */
	public MapPoint saveMapPoint(MapPoint mapPoint) {
		return mapPointDB.save(mapPoint);
	}


	/**
	 * retrieve a language object from database based on passed code.
	 *
	 * @param code the code that is used to get the language from database.
	 *
	 * @return it returns a language object to the controller class.
	 */
	public Language findLanguageByCode(String code) {
		return contentBroker.findLanguageByCode(code);
	}


	/**
	 * retrieve a list containing all tags.
	 *
	 * @return a list of all tags.
	 */
	public List<Tag> getAllTags() {
		return contentBroker.getAllTags();
	}


	/**
	 * Retrieve a tag based on provided string tag.
	 *
	 * @param tagText the string tag.
	 * @return a tag that matches the provided tag string.
	 */
	public Tag getTagByText(String tagText) {
	    return contentBroker.getTagByTagText(tagText);
	}


	/**
	 * retrieve a list of archived stories from database.
	 *
	 * @return it returns a list of story objects.
	 */
	public List<Story> listArchivedStories() {
		//List<Story> allStories = contentBroker.getStories(null);
		List<Story> allArchivedStories = contentBroker.getArchivedStories();
		return allArchivedStories.stream().filter(s -> s.getState().name().equals("ARCHIVED")).collect(Collectors.toList());
	}


	/**
	 * retrieve a origin country from database.
	 *
	 * @param story a story object that is used to get the country origin.
	 * @return it returns a string which contains country origin.
	 */
	public String getCountryOfOriginFull(Story story) {
		Country country = contentBroker.findByCountryCode(story.getCountryOfOrigin());

		String countryName = null;
		if (country != null){
			countryName = country.getName();
		}

		return countryName;
	}


	/**
	 * retrieve a list of filtered stories based on provided tags.
	 *
	 * @param tags the list of tags that is used to filter stories.
	 * @return  it returns a list of story object.
	 */
	public List<Story> getFilteredStories(List<Tag> tags) {
		return contentBroker.getFilteredStories(tags);
	}


	/**
	 * create ShortenedUrl object.
	 *
	 * @param tagIds the list of tag ids.
	 * @return it returns a ShortenedUrl object.
	 */
	public ShortenedUrl createShortenedUrl(List<Integer> tagIds) {
		String randomString = getRandomString();
		ShortenedUrl shortened = new ShortenedUrl(randomString, tagIds);
		shortened.setShortUrl(BASE_URL + "/share/" + randomString);
		ShortenedUrl saved = contentBroker.saveShortenedUrl(shortened);

		return saved;
	}


	/**
	 * get a random string.
	 *
	 * @return it retrieves a random string.
	 */
	private String getRandomString() {
		StringBuilder randomStr = new StringBuilder();
		String possibleChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		for (int i = 0; i < 5; i++)
			randomStr.append(possibleChars.charAt((int) Math.floor(Math.random() * possibleChars.length())));
		return randomStr.toString();
	}


	/**
	 * retrieve  a ShortenedUrl object from database.
	 *
	 * @param randomString the string that is used to get the ShortenedUrl object.
	 * @return it returns a ShortenedUrl object.
	 */
	public ShortenedUrl getShortenedUrl(String randomString) {
		return contentBroker.getShortenedUrl(randomString);
	}


	/**
	 * Retrieve one tag based on it's id.
	 *
	 * @param tagID the tag id.
	 *
	 * @return a tag that matches the provided tag id.
	 */
	public Tag getTagByID(int tagID) {
		return contentBroker.getTagByID(tagID);
	}
}
