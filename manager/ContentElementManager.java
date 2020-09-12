package com.asylumproject.asylumproject.manager;

import com.asylumproject.asylumproject.broker.ContentElementBroker;
import com.asylumproject.asylumproject.reports.PieReportData;
import com.asylumproject.asylumproject.reports.ReportElement;
import com.asylumproject.asylumproject.problemdomain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Contains functionality for creating, retrieving, and archiving ContentElements from the list of available
 * ContentElements.
 */
@Service
public class ContentElementManager {

    private ArrayList<ContentElement> availableElements;
    private ContentElementBroker contentElementBroker;
    private ContentManager contentManager;
    private EventManager eventManager;

    /**
     * no-arg constructor
     */
    public ContentElementManager() {}


    /**
     * A constructor for ContentElementManager class.
     *
     * @param contentElementBroker an object of ContentElementBroker class.
     * @param contentManager an object of ContentManager class.
     * @param eventManager an object of EventManager class.
     */
    @Autowired
    public ContentElementManager(ContentElementBroker contentElementBroker,
                                 ContentManager contentManager,
                                 EventManager eventManager) {
        this.contentElementBroker = contentElementBroker;
        this.contentManager = contentManager;
        this.eventManager = eventManager;
    }


    /**
     * create a text content element object and save it to the database.
     *
     *
     * @param language the language of uploaded text element.
     * @param filePath the filePath of uploaded text element.
     * @param fileSize the fileSize of uploaded text element.
     * @param description the description of uploaded text element.
     * @param fileType the fileType of uploaded text element.
     * @param length the length of uploaded text element.
     * @param state the state of uploaded text element.
     * @param contentType the contentType of uploaded text element.
     * @param mappointID the mappointID of uploaded text element.
     *
     * @return it returns saved text content element from database.
     */
    public ContentElement createTextElement(String language, String filePath, long fileSize, String description, String fileType, long length, String state, String contentType, int mappointID) {
        Text text = new Text(language, description, length, filePath, fileSize, fileType, state, contentType);
        MapPoint mp = contentElementBroker.getMapPoint(mappointID);
        text.setMappoint(mp);
        return contentElementBroker.createContentElement(text);
    }


    /**
     * Create a single Image ContentElement object.
     * None of these parameters may be null.
     *
     * @param language    the language of the included content
     * @param filePath    the filepath where the element is stored on the server
     * @param fileSize    the size in bytes of the uploaded file
     * @param description the description provided by the user
     * @param fileType    the file type of the file
     * @param width       the pixel width of the image
     * @param height      the pixel height of the image
     * @param caption     the below-image text caption
     * @return the new Image object
     */
    public Story createImageElement(String language, String filePath, long fileSize, String description, String fileType, int width, int height, String caption, int mappointID, String contentType, int contentID) {
        Image image = new Image(language, description, width, height);
        image.setFilePath(filePath);
        image.setFileSize(fileSize);
        image.setFileType(fileType);
        image.setContentType(contentType);
        image.setCaption(caption);
        image.setState("DRAFT");
        MapPoint mp = contentElementBroker.getMapPoint(mappointID);
        image.setMappoint(mp);

        ContentElement savedImage = contentElementBroker.createContentElement(image);
        Story updatedStory = saveElementToStory(savedImage, mappointID, contentID);

        if(updatedStory!=null)
            eventManager.logEvent(updatedStory.getCreatorUser(), Event.Operation.CREATED, savedImage);

        return updatedStory;
    }


    /**
     * retrieve language object from database.
     *
     * @param code the code that is used to retrieve language object.
     *
     * @return it returns a language object.
     */
    public Language getLanguage(String code){
        return contentElementBroker.getLanguage(code);
    }


    /**
     * retrieve map point object from database.
     *
     * @param mapPointId map point id that is used to retrieve map point object from database.
     *
     * @return it returns map point object to contentController class.
     */
    public MapPoint getMapPoint(int mapPointId) {
        return contentElementBroker.getMapPoint(mapPointId);
    }


    /**
     * create a map point object and save it to the database.
     *
     * @param mapPoint map point object that is saved to the database.
     *
     * @return it returns saved map point object to the contentController  class.
     */
    public MapPoint createMapPoint(MapPoint mapPoint) {
        return contentElementBroker.createMapPoint(mapPoint);
    }

    /**
     * Create a single Audio ContentElement object.
     * None of these parameters may be null.
     *
     * @param language    the language of the included content
     * @param filePath    the filepath where the element is stored on the server
     * @param fileSize    the size in bytes of the uploaded file
     * @param description the description provided by the user
     * @param fileType    the file type of the file
     * @param length      the length in seconds of the uploaded audio
     * @return the created ContentElement
     */
    public Story createAudioElement(String language, String filePath, long fileSize, String description, String fileType, double length, int mappointID, String contentType, int contentID) {
        Audio audio = new Audio(language, description, length);
        System.out.println("length: " + length);
        audio.setFilePath(filePath);
        audio.setFileSize(fileSize);
        audio.setFileType(fileType);
        audio.setContentType(contentType);
        audio.setState("DRAFT");
        MapPoint mp = contentElementBroker.getMapPoint(mappointID);
        audio.setMappoint(mp);

        ContentElement savedAudio = contentElementBroker.createContentElement(audio);
        Story updatedStory = saveElementToStory(savedAudio, mappointID, contentID);

        if(updatedStory!=null)
            eventManager.logEvent(updatedStory.getCreatorUser(), Event.Operation.CREATED, savedAudio);

        return updatedStory;
    }

    /**
     * Create a single Video ContentElement object.
     * None of these parameters may be null.
     *
     * @param language    the language of the included content
     * @param filePath    the filepath where the element is stored on the server
     * @param fileSize    the size in bytes of the uploaded file
     * @param description the description provided by the user
     * @param fileType    the file type of the file
     * @param length      the length in seconds of the uploaded video
     * @return the new Video object
     */
    public Story createVideoElement(String language, String filePath, long fileSize, String description, String fileType, double length, int mappointID, String contentType, int contentID) {
        Video video = new Video(language, description, length);
        video.setFilePath(filePath);
        video.setFileSize(fileSize);
        video.setFileType(fileType);
        video.setContentType(contentType);
        MapPoint mp = contentElementBroker.getMapPoint(mappointID);
        video.setMappoint(mp);
        video.setState("DRAFT");

        ContentElement savedVideo = contentElementBroker.createContentElement(video);
        Story updatedStory = saveElementToStory(savedVideo, mappointID, contentID);

        if(updatedStory!=null)
            eventManager.logEvent(updatedStory.getCreatorUser(), Event.Operation.CREATED, savedVideo);

        return updatedStory;
    }


    /**
     * save uploaded content element into a specific story.
     *
     * @param element content element that is saved to the story object and database.
     * @param mapPointID map point id that is used to get the map point object from database.
     * @param contentID content id that is used to save content element under that.
     *
     * @return it returns a story object to the contentController class.
     */
    public Story saveElementToStory(ContentElement element, int mapPointID, int contentID) {
        //Save text element to map point
        MapPoint mapPoint = getMapPoint(mapPointID);
        mapPoint.addContentElement(element);
        MapPoint savedMapPoint = createMapPoint(mapPoint);

        Story story = (Story) contentManager.getContent(contentID);

        Language language = getLanguage(element.getLanguage());
        if (language != null){
            story.getLanguages().add(language);
        }

        //Save map point to the current Story
        List<MapPoint> mapPoints = story.getMapPoints();

        int found = -1;
        for (int i = 0; i < mapPoints.size(); i++) {
            if (mapPointID == mapPoints.get(i).getId()) {
                found = i;
            }
        }

        Story finalStory = null;
        if (found != -1) {
            story.getMapPoints().set(found, savedMapPoint);
            finalStory = (Story) contentManager.createStory(story);
        } else {
            story.getMapPoints().add(savedMapPoint);
            finalStory = (Story) contentManager.createStory(story);
        }

        return finalStory;
    }

    /**
     * Receives request to upload a ContentElement from the UI.
     *
     * @param file the file object received from the ContentManagerServlet. This file object contains all binary and
     *             meta-data for the uploaded file. Cannot be null.
     */
    public void uploadContentElementData(Object file) {
        //Create The ContentElement after the file has been verified.
        //Choose type of ContentElement based upon the file type (i.e. createTextElement())
    }


    /**
     * retrieve archived content elements from database that are under passed map point object.
     *
     * @param mapPoint map point object that is used to get archived content elements from.
     *
     * @return it returns a list of content elements that are archived.
     */
    public List<ContentElement> getArchivedElements(MapPoint mapPoint) {
        return contentElementBroker.getArchivedElements(mapPoint);
    }


    /**
     * retrieve text content elements from database that are under passed map point object.
     *
     * @param mapPoint map point object that is used to get text content elements from.
     *
     * @return it returns a list of text content elements.
     */
    public List<ContentElement> getTexts(MapPoint mapPoint) {
        return contentElementBroker.getTexts(mapPoint);
    }


    /**
     * retrieve all content elements from database that are under passed map point object.
     *
     * @param mapPoint map point object that is used to get all content elements from.
     *
     * @return it returns a list of all content elements.
     */
    public List<ContentElement> getAll(MapPoint mapPoint) {
        return contentElementBroker.getAll(mapPoint);
    }


    /**
     * retrieve image content elements from database that are under passed map point object.
     *
     * @param mapPoint map point object that is used to get image content elements from.
     *
     * @return it returns a list of image content elements.
     */
    public List<ContentElement> getImages(MapPoint mapPoint) {
        return contentElementBroker.getImages(mapPoint);
    }


    /**
     * retrieve audio content elements from database that are under passed map point object.
     *
     * @param mapPoint map point object that is used to get audio content elements from.
     *
     * @return it returns a list of audio content elements.
     */
    public List<ContentElement> getAudios(MapPoint mapPoint) {
        return contentElementBroker.getAudios(mapPoint);
    }


    /**
     * retrieve video content elements from database that are under passed map point object.
     *
     * @param mapPoint map point object that is used to get video content elements from.
     *
     * @return it returns a list of video content elements.
     */
    public List<ContentElement> getVideos(MapPoint mapPoint) {
        return contentElementBroker.getVideos(mapPoint);
    }


    /**
     * retrieve a content element object from database.
     *
     * @param contentElementId id that is used to get a content element object from database.
     *
     * @return it returns a content element object to the contentController class.
     */
    public Optional<ContentElement> getContentElement(int contentElementId) {
        return contentElementBroker.getContentElement(contentElementId);
    }


    /**
     * remove passed content element object from database.
     *
     * @param contentElement content element object that is removed from database.
     *
     * @return it returns true if the deletion process was successful.
     */
    public boolean deleteContentElement(ContentElement contentElement) {
        return contentElementBroker.deleteContentElement(contentElement);
    }


    /**
     * remove passed map point object from database.
     *
     * @param mapPoint map point object that is removed from database.
     */
    public void deleteMapPoint(MapPoint mapPoint) {
        contentElementBroker.deleteMapPoint(mapPoint);
    }

    /**
     * Sends the entire list of available (active) ContentElements to the UI to be viewed in list form.
     *
     * @return the list of available ContentElements
     */
    public ArrayList<ContentElement> sendAvailableElements() {
        //call service class to update availableElements list
        return availableElements;
    }


    /**
     * Archives a single ContentElement as selected by the user. Called when the user selects "Remove" on the Content
     * Manager screen.
     *
     * @param elementID the elementID of the ContentElement selected by the user. Cannot be null.
     * @return true if the ContentElement
     */
    public ContentElement archiveContentElement(int elementID, String action) {
        return contentElementBroker.setArchivedElement(elementID, action);
    }


    /**
     * edit passed map point object.
     *
     * @param oldMapPoint the map point object that is updated.
     * @param newMapPoint the updated map point object.
     *
     * @return it returns a map point object.
     */
    public MapPoint editMapPoint(MapPoint oldMapPoint, MapPoint newMapPoint) {
        //-- not needed -- oldMapPoint.setContentElement(newMapPoint.getContentElement());
        //-- not needed -- oldMapPoint.setStory(newMapPoint.getStory());
        oldMapPoint.setCoordinates(newMapPoint.getCoordinates());
        oldMapPoint.setZoomLevel(newMapPoint.getZoomLevel());
        oldMapPoint.setUpdateDateTime(newMapPoint.getUpdateDateTime());

        return contentElementBroker.editMapPoint(oldMapPoint);
    }


    /**
     * retrieve a list of all map point objects that are under passed story object.
     *
     * @param story the story object that is used to get all map point objects from.
     *
     * @return it returns a list of map point objects.
     */
    public List<MapPoint> getAllMapPointsByStory(Story story) {
        return contentElementBroker.getAllByStory(story);
    }


    /**
     * retrieve a list of all map point objects from database.
     *
     * @return it returns a list of all map point objects.
     */
    public List<MapPoint> getAllMapPoints() {
        return contentElementBroker.getAllMapPoints();
    }


    /**
     * edit passed content element object.
     *
     *
     * @param elementID the id that is used to get content element object from database to update it.
     * @param newElement the content element object that is saved as updated content element to the database.
     *
     * @return it returns a content element object to contentController class.
     */
    public ContentElement editContentElement(int elementID, ContentElement newElement) {
        Optional<ContentElement> element = contentElementBroker.getContentElement(elementID);
        ContentElement oldElement = null;
        if (element.isPresent()) {
            oldElement = element.get();
            oldElement.setDescription(newElement.getDescription());
            oldElement.setLanguage(newElement.getLanguage());

            return contentElementBroker.editContentElement(oldElement);
        }
        return null;
    }


    /**
     * edit passed image content element.
     *
     * @param elementID the id that is used to get content element from database.
     * @param newElement the image object that is updated.
     *
     * @return it returns a content element object.
     */
    public ContentElement editImageContentElement(int elementID, Image newElement) {
        Optional<ContentElement> element = contentElementBroker.getContentElement(elementID);

        Image oldElement = null;
        if (element.isPresent()) {
            oldElement = (Image) element.get();
            oldElement.setDescription(newElement.getDescription());
            oldElement.setLanguage(newElement.getLanguage());
            oldElement.setCaption(newElement.getCaption());

            return contentElementBroker.editContentElement(oldElement);
        }

        return null;
    }


    /**
     * edit passed text content element.
     *
     * @param elementID the id that is used to get content element from database.
     * @param newElement the text object that is updated.
     *
     * @return it returns a content element object.
     */
    public ContentElement editTextContentElement(int elementID, Text newElement) {
        Optional<ContentElement> element = contentElementBroker.getContentElement(elementID);
        Text oldElement = null;
        if (element.isPresent()) {
            oldElement = (Text) element.get();
            oldElement.setDescription(newElement.getDescription());
            oldElement.setLanguage(newElement.getLanguage());

            oldElement.setFilePath(newElement.getFilePath());
            oldElement.setFileSize(newElement.getFileSize());
            oldElement.setLength(newElement.getLength());
            return contentElementBroker.editContentElement(oldElement);
        }

        return null;
    }


    /**
     * check to see if the passed file path exists or not.
     *
     * @param filePath the file path that is checked to see if it exists in database or not.
     *
     * @return it returns true if the file name exists.
     */
    public boolean fileNameExists(String filePath, String mapPointID) {
        return contentElementBroker.checkFileNameExists(filePath, mapPointID);
    }

    /**
     * Used to save a file to AWS.
     * @return
     */
    public boolean saveFile(MultipartFile multipartFile, String filePath) throws IOException {
        return contentElementBroker.saveFile(multipartFile, filePath);
    }
}
