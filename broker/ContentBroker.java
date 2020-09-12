package com.asylumproject.asylumproject.broker;

import com.asylumproject.asylumproject.dataaccess.*;
import com.asylumproject.asylumproject.reports.ReportElement;
import com.asylumproject.asylumproject.problemdomain.*;
import com.asylumproject.asylumproject.reports.responses.ReportStoriesPerCountry;
import com.asylumproject.asylumproject.reports.responses.ReportStoriesPerCurator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContentBroker {

    private ContentDB contentDB;
    private LanguageDB languageDB;
    private TagDB tagDB;
    private StoryDB storyDB;
    private CountryDB countryDB;
    private ShortenedUrlDB shortenedUrlDB;

    @Autowired
    public ContentBroker(ContentDB contentDB, LanguageDB languageDB, TagDB tagDB, StoryDB storyDB, CountryDB countryDB, ShortenedUrlDB shortenedUrlDB) {
        this.contentDB = contentDB;
        this.languageDB = languageDB;
        this.tagDB = tagDB;
        this.storyDB = storyDB;
        this.countryDB = countryDB;
        this.shortenedUrlDB = shortenedUrlDB;
    }

    /**
     * A content object is created and saved to the database.
     * @param newContent the new content to be added.
     * @return a reference to the created content object.
     */
    public Content createContent(Content newContent) {
        return null;
    }

    /**
     * An existing content is updated based on a Content object passed as parameter.
     * @param updatedContent the Content object to be updated.
     * @return a reference to the edited Content object.
     */
    public Content editContent(Content updatedContent) {
        return contentDB.save(updatedContent);
    }

    public Story editStory(Story updatedContent) {
        return contentDB.save(updatedContent);
    }
    /**
     * Sets the status attribute of a Content to "pre-published".
     * @param content the Content object to be pre-published.
     * @return a reference to the pre-published Content object.
     */
    public Content prepublishContent(Content content) {
        return null;
    }

    /**
     * Sets the status attribute of a Content to "published".
     * @param content the Content object to be published.
     * @return a reference to the published Content object.
     */
    public Content publishContent(Content content) {
        return null;
    }

    /**
     * Sets the status attribute of a Content to "archived".
     * @param content the Content object to be archived.
     * @return a reference to the archived Content object.
     */
    public Content archiveContent(Content content) {
        return null;
    }

    /**
     * Sets the status attribute of a Content to "un-archived".
     * @param content the Content object to be un-archived.
     * @return a reference to the un-archived Content object.
     */
    public Content unArchiveContent(Content content) {
        return null;
    }

    public Content createStory(Story story){
        return contentDB.save(story);
    }


    public Content getContent(int contentId){
        return contentDB.findByContentId(contentId);
    }
    /**
     * Retrieves a collection of Story objects based on a filter that is passed as parameter.
     * Retrieved Content list will be filtered to only include Story objects.
     * @return a collection of stories based on a filter.
     */
    public List<Story> getStories() {
        return (List<Story>) contentDB.getAllByDtypeAndDeletedFalse("Story");
    }


    /**
     * Retrieves a Story object based on a Content ID passed as parameter.
     * @param contentID the Content ID value corresponding to the Story object to be retrieved.
     * @return the Story object with a matching Content ID.
     */
    public Story getStory(int contentID) {
        return null;
    }

    /**
     * Retrieves an FAQ object based on a Content ID passed as parameter.
     * @param contentID the Content ID value corresponding to the FAQ object to be retrieved.
     * @return the FAQ object with a matching Content ID.
     */
    public FAQ getFAQ(int contentID) {
        return null;
    }

    /**
     * Retrieves an Asylum Information object based on a Content ID passed as parameter.
     * @param contentID the Content ID value corresponding to the Asylum Information object to be retrieved.
     * @return the Asylum Information object with a matching Content ID.
     */
    public AsylumInformation getInformation(int contentID) {
        return null;
    }

    /**
     * Retrieves a Resource object based on a Content ID passed as parameter.
     * @param contentID the Content ID value corresponding to the Resource object to be retrieved.
     * @return the Resource object with a matching Content ID.
     */
    public Resource getResource(int contentID) {
        return null;
    }


    /**
     * retrieve elements that are used for reporting purposes.
     *
     * @return it returns a list of ReportElement objects.
     */
    public List<ReportElement> getReportExistingContents() {
        return contentDB.findElementsReport();
    }


    /**
     * retrieve all stories per curator.
     *
     * @return it returns a list of ReportStoriesPerCurator objects.
     */
    public List<ReportStoriesPerCurator> getStoriesPerCurator() {
        return contentDB.findStoriesPerCurator();
    }


    /**
     * retrieve all stories per status.
     *
     * @return it returns a list of Tuple objects.
     */
    public List<Tuple> getStoriesPerStatus() {
        return contentDB.getStoriesPerStatus();
    }


    /**
     * retrieve all stories per language.
     *
     * @return it returns a list of Tuple objects.
     */
    public List<Tuple> getStoriesPerLanguage() {
        return contentDB.getStoriesPerLanguage();
    }


    /**
     * retrieve all stories per country.
     *
     * @return it returns a list of ReportStoriesPerCurator objects.
     */
    public List<ReportStoriesPerCountry> getStoriesPerCountry() {
        return contentDB.findStoriesPerCountry();
    }


    /**
     * Retrieve one language based on it's id.
     * @param code the language id.
     * @return a language that matches the provided language id.
     */
    public Language findLanguageByCode(String code){
        return languageDB.findByCode(code);
    }


    /**
     * save provided tag object into the database.
     *
     * @param tag the tag object that is saved int the database.
     * @return it returns a tag object.
     */
    public Tag createTag(Tag tag) {
        return tagDB.save(tag);
    }


    /**
     * Retrieve a list containing all tags.
     * @return a list of all tags.
     */
    public List<Tag> getAllTags() {
        return tagDB.findAll();
    }

    /**
     * Retrieve a tag based on provided string tag.
     *
     * @param tagText the string tag.
     * @return a tag that matches the provided tag string.
     */
    public Tag getTagByTagText(String tagText) {
        return tagDB.findByTagAndDeletedFalse(tagText);
    }


    /**
     * retrieve a list of archived stories.
     *
     * @return it returns a list of stories.
     */
    public List<Story> getArchivedStories() {
        return (List<Story>) storyDB.findAllByState(Story.State.ARCHIVED);
    }


    /**
     * remove content from database based on passed content id.
     *
     * @param contentId the content is that is used to delete the content from database.
     */
    public void deleteContent(int contentId) {
        contentDB.deleteById(contentId);
    }


    /**
     * retrieve a country object from database based on provided country code.
     *
     * @param countryOfOrigin the country code that is used to get country object from database.
     * @return it returns a country object.
     */
    public Country findByCountryCode(String countryOfOrigin) {
        return countryDB.findByCode(countryOfOrigin);
    }


    /**
     * retrieve a list of filtered stories based on provided tags.
     *
     * @param tags the list of tags that is used to filter stories.
     * @return  it returns a list of story object.
     */
    public List<Story> getFilteredStories(List<Tag> tags) {
        List<Story> filtered = (List<Story>) contentDB.findAllByDtypeAndStateAndDeletedFalse("Story", Story.State.PUBLISHED);

        List<Story> tagsRemoved = new ArrayList<>();
        for (Story story: filtered){
            boolean hasTag = false;
            for (Tag tag: story.getTags()){

                if (tags.contains(tag)){
                   hasTag = true;
                }
            }

            if (!hasTag){
                tagsRemoved.add(story);
            }
        }

        return tagsRemoved;
    }


    /**
     * save the provided ShortenedUrl object to the database.
     *
     * @param shortened the ShortenedUrl object that is saved to the database.
     * @return it returns a ShortenedUrl object.
     */
    public ShortenedUrl saveShortenedUrl(ShortenedUrl shortened) {
        return shortenedUrlDB.save(shortened);
    }


    /**
     * retrieve  a ShortenedUrl object from database.
     *
     * @param randomString the string that is used to get the ShortenedUrl object.
     * @return it returns a ShortenedUrl object.
     */
    public ShortenedUrl getShortenedUrl(String randomString) {
        return shortenedUrlDB.findByRandomString(randomString);
    }


    /**
     * Retrieve one tag based on it's id.
     *
     * @param tagID the tag id.
     *
     * @return a tag that matches the provided tag id.
     */
    public Tag getTagByID(int tagID) {
        return tagDB.findByTagIdAndDeletedFalse(tagID);
    }
}
