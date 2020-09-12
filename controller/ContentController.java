package com.asylumproject.asylumproject.controller;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.asylumproject.asylumproject.manager.*;
import com.asylumproject.asylumproject.payload.ApiResponse;
import com.asylumproject.asylumproject.problemdomain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servlet class for handling HTTP requests related to loading Content objects.
 */
@RestController
@RequestMapping("/api/content")
@CrossOrigin
public class ContentController implements ResourceLoaderAware {


    private static String UPLOAD_DIR = "uploads";
    private ClassLoader classLoader = getClass().getClassLoader();
    private String basePath = "src/main/webapp/uploads";
    ContentManager contentManager;
    ContentElementManager contentElementManager;
    private EventManager eventManager;
    private ResourceLoader resourceLoader;
    private UserManager userManager;



    /**
     * A constructor for ContentController class.
     *
     * @param contentManager        an object of ContentManager class.
     * @param contentElementManager an object of ContentElementManager class.
     * @param eventManager          an object of EventManager class.
     * @param userManager           an object of UserManager class.
     */
    @Autowired
    ContentController(ContentManager contentManager,
                      ContentElementManager contentElementManager,
                      EventManager eventManager,
                      UserManager userManager) {
        this.contentManager = contentManager;
        this.contentElementManager = contentElementManager;
        this.eventManager = eventManager;
        this.userManager = userManager;
    }

    /**
     * A rest API endpoint which receives a content object from frontend and
     * it saves it to the database.
     *
     * @param content content object that is saved to the database.
     * @return it returns a content object and 200 ok code if the creating process was successful.
     */
    @PostMapping
    public ResponseEntity<Content> createContent(@Valid @RequestBody Content content) {
        Content newContent = contentManager.createContent();
        return new ResponseEntity<Content>(content, HttpStatus.OK);
    }


    /**
     * A rest API endpoint which receives story object from frontend and
     * it edits this story object and return it back to the frontend.
     *
     * @param story a story object that is updated.
     * @param jwt   jwt the acting user JWT token.
     * @return it returns a content object and 200 ok code if the updating process was successful otherwise
     * it returns 204 No Content code if the updating process was unsuccessful.
     */
    @PutMapping(path = "/stories")
    public ResponseEntity<Content> editStory(@Valid @RequestBody Story story,
                                             @RequestHeader("Authorization") String jwt) {

        Content oldContent = contentManager.getContent(story.getContentID());
        Content updatedContent = contentManager.editStory((Story) oldContent, story);

        if (updatedContent != null) {
            eventManager.logEvent(jwt, Event.Operation.MODIFIED, updatedContent);
            return new ResponseEntity<>(updatedContent, HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }
    }


    /**
     * A rest API endpoint which receives content id and tag text from frontend and
     * it uses the tag text to get tag object from database. Then, the method uses content id to
     * get story object from database. Finally, the tag object has been saved to story object and database.
     *
     * @param contentID content id that is used to get the story object from database.
     * @param tag       a tag text that is used to get a tag object from database.
     * @param jwt       jwt the acting user JWT token.
     * @return it returns a content object and 200 OK response if the creation was successful.
     */
    @PutMapping(path = "{contentID}/tags")
    public ResponseEntity<Content> saveTagToStory(@PathVariable int contentID,
                                                  @RequestBody String tag,
                                                  @RequestHeader("Authorization") String jwt) {
        Content oldContent = contentManager.getContent(contentID);
        Tag t = contentManager.getTagByText(tag);
        System.out.println("tag" + t);

        boolean exists = false;
        for (Tag ta : oldContent.getTags()) {
            if (ta.getTag().equals(tag)) {
                exists = true;
            }
        }
        if (!exists) {
            oldContent.addTag(t);
        }

        Content updatedContent = contentManager.createStory((Story) oldContent);
        String uuid = UUID.randomUUID().toString();
        eventManager.logEvent(jwt, Event.Operation.CREATED, t, uuid);
        eventManager.logEvent(jwt, Event.Operation.MODIFIED, oldContent, uuid);
        return new ResponseEntity<Content>(updatedContent, HttpStatus.OK);
    }


    /**
     * A rest API endpoint which receives content id and tag text from frontend and
     * it uses the tag text to get tag object from database. Then, the method uses content id to
     * get story object from database. Finally, the tag object has been removed from story object and database.
     *
     * @param contentID content id that is used to get the story object from database.
     * @param tag       a tag text that is used to get a tag object from database.
     * @param jwt       jwt the acting user JWT token.
     * @return it returns a content object and 200 OK response if the deletion was successful.
     */
    @PutMapping(path = "{contentID}/tags/remove")
    public ResponseEntity<Content> removeTagFromStory(@PathVariable int contentID,
                                                      @RequestBody String tag,
                                                      @RequestHeader("Authorization") String jwt) {
        Content oldContent = contentManager.getContent(contentID);
        Tag t = contentManager.getTagByText(tag);

        boolean exists = false;
        Tag tagToRemove = null;
        for (Tag ta : oldContent.getTags()) {
            if (ta.getTag().equals(tag)) {
                exists = true;
                tagToRemove = ta;
            }
        }

        if (exists) {
            oldContent.removeTag(tagToRemove);
        }

        Content updatedContent = contentManager.createStory((Story) oldContent);

        eventManager.logEvent(jwt, Event.Operation.MODIFIED, updatedContent);
        return new ResponseEntity<Content>(updatedContent, HttpStatus.OK);
    }

    /**
     * Used to delete any one Content object, by Id. Also used to discard a Story in the draft state.
     *
     * @param contentId the content id of the Content object to delete
     * @return 200 OK response if the deletion was successful
     */
    @DeleteMapping(path = "{contentId}")
    public ResponseEntity<Void> deleteContent(@PathVariable int contentId,
                                              @RequestHeader("Authorization") String jwt) {
        try {
            Content content = contentManager.getContent(contentId);
            contentManager.deleteContent(contentId);
            eventManager.logEvent(jwt, Event.Operation.DELETED, content);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }
    }


    /**
     * Used to edit a ContentElement by setting the updating the attributes to the ContentElement passed.
     *
     * @return true if the element was successfully archived, otherwise false
     */
    @PutMapping(path = "{contentID}/mappoints/{mapPointID}/elements/{elementId}/edit")
    public ResponseEntity<Story> editContentElement(@PathVariable int elementId,
                                                    @PathVariable String mapPointID,
                                                    @PathVariable String contentID,
                                                    @RequestBody ContentElement element,
                                                    @RequestHeader("Authorization") String jwt) {
        System.out.println(element instanceof Text);
        ContentElement edited = contentElementManager.editContentElement(elementId, element);

        if (edited != null) {
            Story updatedStory = saveElementToStory(edited, Integer.parseInt(mapPointID), contentID);
            if (updatedStory != null) {
                eventManager.logEvent(jwt, Event.Operation.MODIFIED, updatedStory);
                return new ResponseEntity<>(updatedStory, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } else {
            return new ResponseEntity(new ApiResponse(false, "Provided element ID does not exist."), HttpStatus.BAD_REQUEST);
        }
    }


    /**
     * Used to edit a Text ContentElement by setting the updating the attributes to the ContentElement passed.
     *
     * @return true if the element was successfully archived, otherwise false
     */
    @PutMapping(path = "{contentID}/mappoints/{mapPointID}/elements/text/{elementId}/edit")
    public ResponseEntity<Story> editTextContentElement(@PathVariable int elementId,
                                                        @PathVariable String mapPointID,
                                                        @PathVariable String contentID,
                                                        @RequestBody Text element,
                                                        @RequestHeader("Authorization") String jwt) {
        System.out.println(element instanceof Text);
        ContentElement edited = contentElementManager.editTextContentElement(elementId, element);

        if (edited != null) {
            Story updatedStory = saveElementToStory(edited, Integer.parseInt(mapPointID), contentID);
            if (updatedStory != null) {
                eventManager.logEvent(jwt, Event.Operation.MODIFIED, updatedStory);
                return new ResponseEntity<>(updatedStory, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } else {
            return new ResponseEntity(new ApiResponse(false, "Provided element ID does not exist."), HttpStatus.BAD_REQUEST);
        }
    }


    /**
     * Used to archive a ContentElement by setting the state attribute to "archived".
     *
     * @param elementId the id of the ContentElement to be archived
     * @return true if the element was successfully archived, otherwise false
     */
    @PutMapping(path = "{contentID}/mappoints/{mapPointID}/elements/{elementId}/{action}")
    public ResponseEntity<Story> archiveContentElement(@PathVariable int elementId,
                                                       @PathVariable String mapPointID,
                                                       @PathVariable String contentID,
                                                       @PathVariable String action,
                                                       @RequestHeader("Authorization") String jwt) {
        ContentElement edited = contentElementManager.archiveContentElement(elementId, action);

        if (edited != null) {
            Story updatedStory = saveElementToStory(edited, Integer.parseInt(mapPointID), contentID);
            if (updatedStory != null) {
                eventManager.logEvent(jwt, Event.Operation.MODIFIED, updatedStory);
                return new ResponseEntity<>(updatedStory, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } else {
            return new ResponseEntity(new ApiResponse(false, "Provided element ID does not exist."), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Used to edit an Image ContentElement by setting the updating the attributes to the ContentElement passed.
     *
     * @return true if the element was successfully archived, otherwise false
     */
    @PutMapping(path = "{contentID}/mappoints/{mapPointID}/elements/images/{elementId}/edit")
    public ResponseEntity<Story> editImageContentElement(@PathVariable int elementId,
                                                         @PathVariable String mapPointID,
                                                         @PathVariable String contentID,
                                                         @RequestBody Image element,
                                                         @RequestHeader("Authorization") String jwt) {
        ContentElement edited = contentElementManager.editImageContentElement(elementId, element);

        if (edited != null) {
            Story updatedStory = saveElementToStory(edited, Integer.parseInt(mapPointID), contentID);
            if (updatedStory != null) {
                eventManager.logEvent(jwt, Event.Operation.MODIFIED, updatedStory);
                return new ResponseEntity<>(updatedStory, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } else {
            return new ResponseEntity(new ApiResponse(false, "Provided element ID does not exist."), HttpStatus.BAD_REQUEST);
        }
    }


    /**
     * A rest API endpoint which receives a map point object from frontend and
     * it updated this map point and send it back to frontend.
     *
     * @param mapPoint a map point object that is updated.
     * @param jwt      c
     * @return it returns a map point object and 200 ok code if the updating process was successful otherwise
     * it returns 204 No Content code if the updating process was unsuccessful.
     */
    @PutMapping(path = "/mappoints")
    public ResponseEntity<MapPoint> updateMapPoint(@Valid @RequestBody MapPoint mapPoint,
                                                   @RequestHeader("Authorization") String jwt) {
        System.out.println("new: " + mapPoint);
        MapPoint oldMapPoint = contentElementManager.getMapPoint(mapPoint.getId());
        System.out.println("old: " + oldMapPoint);
        MapPoint updatedMapPoint = contentElementManager.editMapPoint(oldMapPoint, mapPoint);
        if (updatedMapPoint != null) {
            eventManager.logEvent(jwt, Event.Operation.MODIFIED, updatedMapPoint);
            return new ResponseEntity<MapPoint>(updatedMapPoint, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    /**
     * A rest API endpoint which retrieves all of the tag objects that are saved into the database.
     *
     * @return it returns a list of tag objects and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "tags")
    public ResponseEntity<List<Tag>> getAllTags() {
        List<Tag> tags = contentManager.getAllTags();

        if (tags != null) {

            return new ResponseEntity<>(tags, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    /**
     * A rest API endpoint which receives content id and tag object from frontend and
     * it uses the content id to get the story object from database. then, the method adds the
     * tag object into that story object.
     *
     * @param contentID content id that is used to get the story object from database.
     * @param tag       a tag object that is added to the story object.
     * @param jwt       the acting user JWT token.
     * @return it returns a story object and 200 ok code if the creating process was successful otherwise
     * it returns 204 No Content code if the creating process was unsuccessful.
     */
    @PostMapping(path = "{contentID}/tags")
    public ResponseEntity<Story> createTag(@PathVariable int contentID,
                                           @RequestBody Tag tag,
                                           @RequestHeader("Authorization") String jwt) {
        Story story = (Story) contentManager.getContent(contentID);
        if (contentManager.getTagByText(tag.getTag()) == null) {
            Tag savedTag = contentManager.createTag(tag);

            if (savedTag != null) {

                if (story != null) {
                    //apply saved tag to current story immediately
                    story.getTags().add(savedTag);
                    Story updatedStory = (Story) contentManager.createStory(story);
                    String uuid = UUID.randomUUID().toString();
                    eventManager.logEvent(jwt, Event.Operation.CREATED, savedTag, uuid);
                    eventManager.logEvent(jwt, Event.Operation.MODIFIED, updatedStory, uuid);
                    return new ResponseEntity<>(updatedStory, HttpStatus.OK);
                }
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } else {
            return new ResponseEntity<>(story, HttpStatus.NO_CONTENT);
        }
    }


    /**
     * A rest API endpoint which receives content id and map point id from frontend and
     * it uses the map point id to get map point object from database and then it removes it.
     *
     * @param contentID  content id that is used to get the updated story object from database.
     * @param mapPointID map point id that is used to get the map point object from database.
     * @param jwt        the acting user JWT token.
     * @return it returns a story object and 200 ok code if the deleting process was successful otherwise
     * it returns 204 No Content code if the deleting process was unsuccessful.
     */
    @DeleteMapping(path = "{contentID}/mappoints/{mapPointID}")
    public ResponseEntity<Story> deleteMapPoint(@PathVariable int contentID,
                                                @PathVariable int mapPointID,
                                                @RequestHeader("Authorization") String jwt) {
        MapPoint mapPoint = contentElementManager.getMapPoint(mapPointID);
        if (mapPoint != null) {

            Story updatedStory = null;
            contentElementManager.deleteMapPoint(mapPoint);
            updatedStory = (Story) contentManager.getContent(contentID);
            eventManager.logEvent(jwt, Event.Operation.DELETED, mapPoint);
            eventManager.logEvent(jwt, Event.Operation.MODIFIED, updatedStory);
            return new ResponseEntity<>(updatedStory, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }


    /**
     * A rest API endpoint which receives element id, map point id, and content id from frontend and
     * it deletes the content element from database.
     *
     * @param elementID  element id that is used to get that specific content element from database.
     * @param mapPointID map point id that is used to check if the uploaded content element is already exists or not.
     * @param contentID  content id that is used to get the story object from database.
     * @param jwt        the acting user JWT token.
     * @return it returns a story object and 200 ok code if the deleting process was successful otherwise
     * it returns 204 No Content code if the deleting process was unsuccessful.
     */
    @DeleteMapping(path = "{contentID}/mappoints/{mapPointID}/elements/{elementID}")
    public ResponseEntity<Story> deleteContentElement(@PathVariable int elementID,
                                                      @PathVariable String mapPointID,
                                                      @PathVariable String contentID,
                                                      @RequestHeader("Authorization") String jwt) {
        Optional<ContentElement> contentElement = contentElementManager.getContentElement(elementID);

        if (contentElement.isPresent()) {
            ContentElement element = contentElement.get();
            Story updatedStory = removeElementFromStory(element, Integer.parseInt(mapPointID), contentID);

            if (contentElementManager.deleteContentElement(element)) {
                eventManager.logEvent(jwt, Event.Operation.DELETED, element);
                eventManager.logEvent(jwt, Event.Operation.MODIFIED, updatedStory);
                return new ResponseEntity<Story>(updatedStory, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

    }


    /**
     * A method which gets element, map point id, and content id as it's parameters
     * and it uses the map point id to get the map point object from database and then it checks if the
     * the content element already exists under that map point. if it is, the method uses the content id to get
     * the story object from database. finally, method removes the content element form database.
     *
     * @param element    content element that is removed from database.
     * @param mapPointID map point id that is used to check if the uploaded content element is already exists or not.
     * @param contentID  content id that is used to get the story object from database.
     * @return it return a story object that is removed from story object and database recently.
     */
    public Story removeElementFromStory(ContentElement element, int mapPointID, String contentID) {
        MapPoint mapPoint = contentElementManager.getMapPoint(mapPointID);

        //Check if element exist in map point
        int exists = -1;
        for (int i = 0; i < mapPoint.getContentElement().size(); i++) {
            if (element.getId() == mapPoint.getContentElement().get(i).getId()) {
                exists = i;
            }
        }

        MapPoint updatedMapPoint = null;
        if (exists != -1) {
            mapPoint.getContentElement().remove(exists);
            updatedMapPoint = contentElementManager.createMapPoint(mapPoint);
        }

        Story story = (Story) contentManager.getContent(Integer.parseInt(contentID));

        List<MapPoint> mapPoints = story.getMapPoints();
        Language elementLanguage = contentElementManager.getLanguage(element.getLanguage());

        int numLangs = 0;
        int found = -1;
        for (int i = 0; i < mapPoints.size(); i++) {
            //look for matching map point
            if (mapPointID == mapPoints.get(i).getId()) {
                found = i;
            }

            //search for language to remove if there isn't more than one
            MapPoint m = mapPoints.get(i);
            for (int y = 0; y < m.getContentElement().size(); y++) {
                if (m.getContentElement().get(y).getLanguage().equals(elementLanguage.getCode())) {
                    numLangs++;
                }

            }
        }

        if (numLangs == 1) {
            try {
                story.getLanguages().remove(elementLanguage);
            } catch (Exception e) {
            }
        }

        //Save map point to the current Story
        System.out.println("found: " + found);
        Story finalStory = null;
        if (found != -1) {
            story.getMapPoints().set(found, updatedMapPoint);
            finalStory = (Story) contentManager.createStory(story);
        } else {
            story.getMapPoints().add(updatedMapPoint);
            finalStory = (Story) contentManager.createStory(story);
        }

        return finalStory;
    }


    /**
     * A method which gets a content element, map point id, and content id as it's parameters
     * and it uses the map point id to get the map point object from database. Then, the method checks if the
     * uploaded content element is already exists under map point object or not. if it is not, the method uses the content id
     * to get the story object from database and saves the uploaded content element under that map point object and then saves that map point under story object.
     *
     * @param element    content element that is saved under a specific story in database.
     * @param mapPointID map point id that is used to check if the uploaded content element is already exists or not.
     * @param contentID  content id that is used to get the story object from database.
     * @return it return a story object which includes the recently added content element.
     */
    public Story saveElementToStory(ContentElement element, int mapPointID, String contentID) {
        //Save text element to map point
        MapPoint mapPoint = contentElementManager.getMapPoint(mapPointID);

        //Check if element exist in map point
        int exists = -1;
        for (int i = 0; i < mapPoint.getContentElement().size(); i++) {
            if (element.getId() == mapPoint.getContentElement().get(i).getId()) {
                exists = i;
            }
        }

        MapPoint updatedMapPoint;
        if (exists != -1) {
            mapPoint.getContentElement().set(exists, element);
            updatedMapPoint = contentElementManager.createMapPoint(mapPoint);
        } else {
            mapPoint.getContentElement().add(element);
            updatedMapPoint = contentElementManager.createMapPoint(mapPoint);
        }

        Story story = (Story) contentManager.getContent(Integer.parseInt(contentID));


        Language language = contentElementManager.getLanguage(element.getLanguage());
        if (language != null) {
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
            story.getMapPoints().set(found, updatedMapPoint);
            finalStory = (Story) contentManager.createStory(story);
        } else {
            story.getMapPoints().add(updatedMapPoint);
            finalStory = (Story) contentManager.createStory(story);
        }

        return finalStory;
    }


    /**
     * A rest API endpoint which receives a map point object and a story id from frontend and
     * saves the map point object under that story id in the database.
     *
     * @param mapPoint map point object that is saved into the database.
     * @param storyID  a story id that is used to save the map point object under that.
     * @param jwt      the acting user JWT token.
     * @return it returns a map point object and 200 ok code if the saving process was successful otherwise
     * it returns 204 No Content code if the saving process was unsuccessful.
     */
    @PostMapping(path = "stories/{storyID}/mapPoints")
    public ResponseEntity<MapPoint> createMapPoint(@Valid @RequestBody MapPoint mapPoint,
                                                   @PathVariable String storyID,
                                                   @RequestHeader("Authorization") String jwt) {
        Story story = (Story) contentManager.getContent(Integer.parseInt(storyID));
        mapPoint.setStory(story);
        MapPoint savedMapPoint = contentElementManager.createMapPoint(mapPoint);
        System.out.println(savedMapPoint);
        int id = Integer.parseInt(storyID);
        int mapid = savedMapPoint.getId();
        try {
            Path newFilePath = Paths.get("src/main/webapp/uploads" + File.separator + "story" + id + File.separator + "mapPoint" + mapid);
            System.out.println("newFilePath: " + newFilePath);
            Path newDir = Files.createDirectory(newFilePath);
            System.out.println("newDir: " + newDir);
            //Files.createFile(newFilePath);
//            path = new File(basePath+ File.separator + "story"+id + File.separator + "mapPoint"+mapid);
//            if (!path.exists())
//                path.mkdirs();
            eventManager.logEvent(jwt, Event.Operation.CREATED, savedMapPoint);
            return new ResponseEntity<MapPoint>(savedMapPoint, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<MapPoint>(HttpStatus.NO_CONTENT);
        }

        //return new ResponseEntity<>(savedMapPoint, HttpStatus.OK);
    }


    /**
     * A rest API endpoint which receives a story object from frontend and saves it to the database.
     *
     * @param story story object that is saved to the database.
     * @param jwt   the acting user JWT token.
     * @return it returns a content object and 200 ok code if the saving process was successful otherwise
     * it returns 204 No Content code if the saving process was unsuccessful.
     */
    @PostMapping(path = "/stories")
    public ResponseEntity<Content> createStory(@Valid @RequestBody Story story,
                                               @RequestHeader("Authorization") String jwt) {
        Optional<User> userOptional = userManager.getUserById(story.getCreator());
        if (userOptional.isPresent()) {
            story.setCreatorUser(userOptional.get());

            String countryFull = contentManager.getCountryOfOriginFull(story);

            if (countryFull != null) {
                story.setCountryFull(countryFull);
            }

            Content createStory = contentManager.createStory(story);
            //boolean path = new File("src/main/resources/uploads/Story"+ createStory.getContentID()).mkdirs();
            //boolean f = new File("C:\\asylumproject\\src\\main\\webapp\\uploads"+ "\\Story"+createStory.getContentID()).mkdirs();
            int id = createStory.getContentID();
            try {
                //Path newFilePath = Paths.get(basePath + File.separator + "story" + id);
                Path newFilePath = Paths.get("src/main/webapp/uploads" + File.separator + "story" + id);
                //Files.createFile(newFilePath);
                Files.createDirectory(newFilePath);
                //File path = new File(basePath+ File.separator + "story"+id);
                //            if (!path.exists())
                //                path.mkdirs();
                System.out.println(createStory.getCreatorUser().getUsername());
                eventManager.logEvent(jwt, Event.Operation.CREATED, createStory);
                return new ResponseEntity<Content>(createStory, HttpStatus.OK);

            } catch (Exception e) {
                return new ResponseEntity<Content>(HttpStatus.NO_CONTENT);
            }
        } else
            return new ResponseEntity<Content>(HttpStatus.BAD_REQUEST);
    }


    /**
     * A rest API endpoint which retrieves a list of all Contents elements to frontend.
     *
     * @return it returns a list of all contents elements and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "/elements")
    public ResponseEntity<List<ContentElement>> getContentElements() {
        List<ContentElement> elements = contentElementManager.sendAvailableElements();
        return new ResponseEntity<List<ContentElement>>(elements, HttpStatus.OK);
    }


    /**
     * A rest API endpoint which retrieves a list of all Contents.
     *
     * @return it returns a list of all contents and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping
    public ResponseEntity<List<Content>> getContent() {
        List<Content> content = contentManager.listContent();
        if (content != null) {
            return new ResponseEntity<List<Content>>(content, HttpStatus.OK);
        } else {
            return new ResponseEntity<List<Content>>(HttpStatus.NO_CONTENT);
        }
    }


    /**
     * A rest API endpoint which retrieves all kind of stories from database regardless of their states.
     *
     * @return it returns a list of all stories and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "/stories")
    public ResponseEntity<List<Story>> getStories() {
        List<Story> stories = contentManager.listStories();
        if (stories != null) {
            return new ResponseEntity<List<Story>>(stories, HttpStatus.OK);
        } else {
            return new ResponseEntity<List<Story>>(HttpStatus.NO_CONTENT);
        }
    }

    /**
     * A rest API endpoint which retrieves all the stories that their state marked as published, from database.
     *
     * @return it returns a list of published stories and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "/stories/published")
    public ResponseEntity<List<Story>> getPublishedStories() {
        List<Story> stories = contentManager.listPublishedStories();
        if (stories != null) {
            return new ResponseEntity<List<Story>>(stories, HttpStatus.OK);
        } else {
            return new ResponseEntity<List<Story>>(HttpStatus.NO_CONTENT);
        }
    }


    /**
     * A rest API endpoint which retrieves all the stories that their state marked as archived, from database.
     *
     * @return it returns a list of archived stories and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "/stories/archived")
    public ResponseEntity<List<Story>> getArchivedStories() {
        List<Story> stories = contentManager.listArchivedStories();
        if (stories != null) {
            return new ResponseEntity<List<Story>>(stories, HttpStatus.OK);
        } else {
            return new ResponseEntity<List<Story>>(HttpStatus.NO_CONTENT);
        }
    }


    /**
     * A rest API endpoint which retrieves all map points, under the specific story id
     * that is sent from frontend, from database.
     *
     * @param contentID the id that is used to retrieve all the map points under that.
     * @return it returns a list of map points under story id and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "/{contentID}/mappoints")
    public ResponseEntity<List<MapPoint>> getMappointsByStoryID(@PathVariable String contentID) {
        Story story = (Story) contentManager.getContent(Integer.parseInt(contentID));
        List<MapPoint> mapPoints = contentElementManager.getAllMapPointsByStory(story);
        if (mapPoints != null) {
            return new ResponseEntity<List<MapPoint>>(mapPoints, HttpStatus.OK);
        } else {
            return new ResponseEntity<List<MapPoint>>(HttpStatus.NO_CONTENT);
        }
    }


    /**
     * A rest API endpoint which retrieves all map points from database.
     *
     * @return it returns a list of map points and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "/mappoints")
    public ResponseEntity<List<MapPoint>> getMappoints() {
        List<MapPoint> mapPoints = (List<MapPoint>) contentElementManager.getAllMapPoints();
        if (mapPoints != null) {

            return new ResponseEntity<List<MapPoint>>(mapPoints, HttpStatus.OK);
        } else {
            return new ResponseEntity<List<MapPoint>>(HttpStatus.NO_CONTENT);
        }
    }


    /**
     * A rest API endpoint which receives content element id from frontend and
     * uses it to the ContentElement object from database.
     *
     * @param contentElementID the id that is used to retrieve the ContentElement object from database.
     * @return it returns a content element object and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "elements/{contentElementID}")
    public ResponseEntity<ContentElement> getContentElement(@PathVariable int contentElementID) {
        Optional<ContentElement> contentElement = contentElementManager.getContentElement(contentElementID);
        if (contentElement.isPresent()) {
            return new ResponseEntity<ContentElement>(contentElement.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<ContentElement>(HttpStatus.BAD_REQUEST);
        }
    }


    /**
     * A rest API endpoint which receives map point id from frontend and
     * uses it to retrieve the MapPoint object from database.
     *
     * @param mapPointID map point id that is used to retrieve the MapPoint object from database.
     * @return it returns a map point and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "/mappoints/{mapPointID}")
    public ResponseEntity<MapPoint> getMapPoint(@PathVariable int mapPointID) {
        MapPoint mapPoint = contentElementManager.getMapPoint(mapPointID);
        if (mapPoint != null) {
            return new ResponseEntity<MapPoint>(mapPoint, HttpStatus.OK);
        }
        return new ResponseEntity<MapPoint>(HttpStatus.NO_CONTENT);
    }

    /**
     * A rest API endpoint which receives map point id from frontend and
     * uses it to retrieve a list of archived content elements that are saved under this id, regardless of the file type.
     *
     * @param mapPointID map point id that is used to retrieve the MapPoint object from database.
     * @return it returns a list of content elements that are archived and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "/mappoints/{mapPointID}/elements/archived")
    public ResponseEntity<List<ContentElement>> getArchived(@PathVariable String mapPointID) {
        int mappointID = Integer.parseInt(mapPointID);
        MapPoint mapPoint = contentElementManager.getMapPoint(mappointID);
        List<ContentElement> archivedElements = contentElementManager.getArchivedElements(mapPoint);
        if (archivedElements != null) {
            return new ResponseEntity<List<ContentElement>>(archivedElements, HttpStatus.OK);
        } else {
            return new ResponseEntity<List<ContentElement>>(HttpStatus.NO_CONTENT);
        }
    }

    /**
     * A rest API endpoint which receives map point id from frontend and
     * uses it to retrieve a list of all content elements that are saved under this id, regardless of the file type.
     *
     * @param mapPointID map point id that is used to retrieve the MapPoint object from database.
     * @return it returns a list of content elements and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "/mappoints/{mapPointID}/elements")
    public ResponseEntity<List<ContentElement>> getAllElements(@PathVariable String mapPointID) {
        int mappointID = Integer.parseInt(mapPointID);
        MapPoint mapPoint = contentElementManager.getMapPoint(mappointID);
        List<ContentElement> allElements = contentElementManager.getAll(mapPoint);
        if (allElements != null) {
            return new ResponseEntity<List<ContentElement>>(allElements, HttpStatus.OK);
        } else {
            return new ResponseEntity<List<ContentElement>>(HttpStatus.NO_CONTENT);
        }
    }


    /**
     * A rest API endpoint which receives map point id from frontend and
     * uses it to retrieve a list of image content elements that are saved under this id.
     *
     * @param mapPointID map point id that is used to retrieve the MapPoint object from database.
     * @return it returns a list of images and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "/mappoints/{mapPointID}/elements/images")
    public ResponseEntity<List<ContentElement>> getImageElements(@PathVariable String mapPointID) {
        int mappointID = Integer.parseInt(mapPointID);
        MapPoint mapPoint = contentElementManager.getMapPoint(mappointID);
        List<ContentElement> imageList = contentElementManager.getImages(mapPoint);
        if (imageList != null) {
            return new ResponseEntity<List<ContentElement>>(imageList, HttpStatus.OK);
        } else {
            return new ResponseEntity<List<ContentElement>>(HttpStatus.NO_CONTENT);
        }
    }


    /**
     * A rest API endpoint which receives map point id from frontend and
     * uses it to retrieve a list of audio content elements that are saved under this id.
     *
     * @param mapPointID map point id that is used to retrieve the MapPoint object from database.
     * @return it returns a list of audios and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "/mappoints/{mapPointID}/elements/audios")
    public ResponseEntity<List<ContentElement>> getAudioElements(@PathVariable String mapPointID) {
        int mappointID = Integer.parseInt(mapPointID);
        MapPoint mapPoint = contentElementManager.getMapPoint(mappointID);
        List<ContentElement> audioList = contentElementManager.getAudios(mapPoint);
        if (audioList != null) {
            return new ResponseEntity<List<ContentElement>>(audioList, HttpStatus.OK);
        } else {
            return new ResponseEntity<List<ContentElement>>(HttpStatus.NO_CONTENT);
        }
    }


    /**
     * A rest API endpoint which receives map point id from frontend and
     * uses it to retrieve a list of video content elements that are saved under this id.
     *
     * @param mapPointID map point id that is used to retrieve the MapPoint object from database.
     * @return it returns a list of videos and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "/mappoints/{mapPointID}/elements/videos")
    public ResponseEntity<List<ContentElement>> getVideoElements(@PathVariable String mapPointID) {
        int mappointID = Integer.parseInt(mapPointID);
        MapPoint mapPoint = contentElementManager.getMapPoint(mappointID);
        List<ContentElement> videoList = contentElementManager.getVideos(mapPoint);
        if (videoList != null) {
            return new ResponseEntity<List<ContentElement>>(videoList, HttpStatus.OK);
        } else {
            return new ResponseEntity<List<ContentElement>>(HttpStatus.NO_CONTENT);
        }
    }


    /**
     * A rest API endpoint which receives map point id from frontend and
     * uses it to retrieve a list of text content elements that are saved under this id.
     *
     * @param mapPointID map point id that is used to retrieve the MapPoint object from database.
     * @return it returns a list texts and 200 ok code if the retrieving process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "/mappoints/{mapPointID}/elements/texts")
    public ResponseEntity<List<ContentElement>> getTextElements(@PathVariable String mapPointID) {
        int mappointID = Integer.parseInt(mapPointID);
        MapPoint mapPoint = contentElementManager.getMapPoint(mappointID);
        List<ContentElement> textList = contentElementManager.getTexts(mapPoint);
        if (textList != null) {
            return new ResponseEntity<List<ContentElement>>(textList, HttpStatus.OK);
        } else {
            return new ResponseEntity<List<ContentElement>>(HttpStatus.NO_CONTENT);
        }
    }

    /**
     * A rest API endpoint which receives text object, map point id, and content id from frontend
     * and saves it to the database under received content id and map point id.
     *
     * @param text       a text object which includes text contents and it is sent from frontend.
     * @param mapPointID map point id to save the text object under that.
     * @param contentID  content id to save the text object under that.
     * @param jwt        Log an event with details about the acting user, the type of operation and the item affected by the event.
     * @return it returns a story object and 200 ok code if the saving process was successful otherwise
     * it returns 204 No Content code if the saving process was unsuccessful.
     */
    @PostMapping(path = "{contentID}/mappoints/{mapPointID}/elements/text")
    public ResponseEntity<Story> uploadText(@RequestBody Text text,
                                            @PathVariable String mapPointID,
                                            @PathVariable String contentID,
                                            @RequestHeader("Authorization") String jwt) {
        //check if filename already exists on the server
        if (contentElementManager.fileNameExists(text.getFilePath(), mapPointID)) {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        } else {

            int mappointID = Integer.parseInt(mapPointID);
            ContentElement uploadedText = contentElementManager.createTextElement(text.getLanguage(),
                    text.getFilePath(), text.getFileSize(), text.getDescription(), text.getFileType(),
                    text.getLength(), text.getState(), text.getContentType(), mappointID);

            Story updatedStory = saveElementToStory(uploadedText, mappointID, contentID);

            if (updatedStory != null) {
                eventManager.logEvent(jwt, Event.Operation.CREATED, uploadedText);
                return new ResponseEntity<Story>(updatedStory, HttpStatus.OK);
            } else {
                return new ResponseEntity<Story>(HttpStatus.NO_CONTENT);
            }
        }
    }


    /**
     * A rest API endpoint which receives the content id , map point id , and file name from
     * frontend and it uses these information to get the URL of the saved video file under webapp/uploads folder.
     *
     * @param contentID  the content id that is used to find specified folder.
     * @param mapPointID the map point id that is used to find the specified folder.
     * @param fileName   the file name that is used to find the specified folder.
     * @return it returns a resource object which includes URL of videos and 200 ok code if the retrieving  process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "{contentID}/mapPoints/{mapPointID}/videos/{fileName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getFolderVideos(@PathVariable int contentID, @PathVariable int mapPointID, @PathVariable String fileName) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("file:" + basePath + File.separator + "story" + contentID + File.separator + "mapPoint" + mapPointID + File.separator + "video" + File.separator + fileName);

        if (resource.exists()) {
            return new ResponseEntity<Resource>(resource, HttpStatus.OK);
        }else{
            return new ResponseEntity<Resource>(HttpStatus.NO_CONTENT);
        }
    }

    /**
     * A rest API endpoint which receives the content id , map point id , and file name from
     * frontend and it uses these information to get the URL of the saved audio file under webapp/uploads folder.
     *
     * @param contentID  the content id that is used to find specified folder.
     * @param mapPointID the map point id that is used to find the specified folder.
     * @param fileName   the file name that is used to find the specified folder.
     * @return it returns a resource object which includes URL of audios and 200 ok code if the retrieving  process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "{contentID}/mapPoints/{mapPointID}/audio/{fileName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getFolderAudio(@PathVariable int contentID, @PathVariable int mapPointID, @PathVariable String fileName) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("file:" + basePath + File.separator + "story" + contentID + File.separator + "mapPoint" + mapPointID + File.separator + "audio" + File.separator + fileName);
        if (resource.exists()) {
            return new ResponseEntity<Resource>(resource, HttpStatus.OK);
        }else{
            return new ResponseEntity<Resource>(HttpStatus.NO_CONTENT);
        }
    }

    /**
     * A rest API endpoint which receives the content id , map point id , and file name from
     * frontend and it uses these information to get the URL of the saved image file under webapp/uploads folder.
     *
     * @param contentID  the content id that is used to find specified folder.
     * @param mapPointID the map point id that is used to find the specified folder.
     * @param fileName   the file name that is used to find the specified folder.
     * @return it returns a resource object which includes URL of images and 200 ok code if the retrieving  process was successful otherwise
     * it returns 204 No Content code if the retrieving process was unsuccessful.
     */
    @GetMapping(path = "{contentID}/mapPoints/{mapPointID}/images/{fileName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getFolderImages(@PathVariable int contentID, @PathVariable int mapPointID, @PathVariable String fileName) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("file:" + basePath + File.separator + "story" + contentID + File.separator + "mapPoint" + mapPointID + File.separator + "image" + File.separator + fileName);

        /*
        ClassLoader cl = this.getClass().getClassLoader();
        InputStream inputStream = cl.getResourceAsStream(basePath + File.separator + "story" + contentID + File.separator + "mapPoint" + mapPointID + File.separator + "image" + File.separator + fileName);
        System.out.println("inputstream: " + inputStream);
        if (inputStream != null){
            inputStream.
        }

         */
        if (resource.exists()) {
            return new ResponseEntity<Resource>(resource, HttpStatus.OK);
        }else{
            return new ResponseEntity<Resource>(HttpStatus.NO_CONTENT);
        }
    }

    /**
     * A rest API endpoint which receives the content id from frontend and
     * it uses this id to get the story object from database.
     *
     * @param id the content id that is used to get story object from database.
     * @return it returns a content object and 200 ok code if the getting process was successful otherwise
     * it returns 204 No Content code if the getting process was unsuccessful.
     */
    @GetMapping(path = "/stories/{id}")
    public ResponseEntity<Content> getStory(@PathVariable String id) {
        int contentID = Integer.parseInt(id);
        Content story = contentManager.getContent(contentID);
        if (story != null) {
            return new ResponseEntity<Content>(story, HttpStatus.OK);
        } else {
            return new ResponseEntity<Content>(HttpStatus.NO_CONTENT);
        }
    }


    /**
     * A rest API endpoint which receives the story object from frontend and
     * saves it to the database.
     *
     * @param story story object that has been sent from front end and
     *              it will saved to the database.
     * @return it returns a content object and 200 ok code if the saving process was successful otherwise
     * it returns 204 No Content code if the saving process was unsuccessful.
     */
    @PutMapping(path = "/stories/{id}/mappoints/{mapPointID}")
    public ResponseEntity<Content> saveMapPointToStory(@RequestBody Story story) {
        Content newStory = contentManager.createStory(story);
        if (newStory != null) {
            return new ResponseEntity<Content>(newStory, HttpStatus.OK);
        } else {
            return new ResponseEntity<Content>(HttpStatus.NO_CONTENT);
        }
    }


    /**
     * A rest API endpoint which receives the uploaded file with its details from frontend and
     * saves the file data to the database, and the actual file to AWS storage.
     *
     * @param file        A representation of an uploaded file received in a multipart request.The file contents are either stored in memory or temporarily on disk.
     * @param request     to provide request information for HTTP servlets. The servlet container creates an HttpServletRequest
     *                    object and passes it as an argument to the servlet's service methods.
     * @param mapPointID  id which will be used to save the uploaded file under that.
     * @param language    the language in which the content element has been uploaded.
     * @param imgWidth    the width size of uploaded image file.
     * @param imgHeight   the height size of uploaded image file.
     * @param length      the length of uploaded audio or video content elements.
     * @param imgCaption  the caption of uploaded image file.
     * @param description the description that has been uploaded with
     * @param contentID   the id that will used to store the uploaded content elements under that.
     * @param jwt
     * @return it returns a story object and 200 ok code if the upload was successful otherwise
     * it returns 204 No Content code if the upload was unsuccessful OR
     * it returns 400 Bad Request code if the uploaded file's type was invalid.
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Story> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request,
                                        @RequestParam String mapPointID,
                                        @RequestParam String language, @RequestParam int imgWidth,
                                        @RequestParam int imgHeight, @RequestParam String length,
                                        @RequestParam String imgCaption, @RequestParam String description,
                                        @RequestParam int contentID,
                                        @RequestHeader("Authorization") String jwt) {

        //check if filename already exists on the server
        if (contentElementManager.fileNameExists(file.getOriginalFilename(), mapPointID)) {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        } else {

            double length1 = Double.parseDouble(length);
            int mappointID = Integer.parseInt(mapPointID);
            String fileType = file.getContentType();
            Story updatedStory = null;

            if (fileType.substring(0, 5).equals("image")) {
                updatedStory = contentElementManager.createImageElement(language, file.getOriginalFilename(), file.getSize(), description, "image", imgWidth, imgHeight, imgCaption, mappointID, file.getContentType(), contentID);
                try {
                    //String fileName = file.getOriginalFilename();
                    //Path newFilePath = Paths.get(basePath + File.separator + "story" + contentID + File.separator + "mapPoint" + mappointID);
                    String filePath = "story" + contentID + "/mapPoint" + mapPointID + "/image";
                    boolean fileSaved = contentElementManager.saveFile(file, filePath);

                    if (updatedStory != null && fileSaved) {
                        return new ResponseEntity<Story>(updatedStory, HttpStatus.OK);
                    } else {
                        return new ResponseEntity<Story>(HttpStatus.NO_CONTENT);
                    }
                } catch (Exception e) {
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                }

            } else if (fileType.substring(0, 5).equals("video")) {
                updatedStory = contentElementManager.createVideoElement(language, file.getOriginalFilename(), file.getSize(), description, "video", length1, mappointID, file.getContentType(), contentID);
                try {
                    //String fileName = file.getOriginalFilename();
                    //Path newFilePath = Paths.get(basePath + File.separator + "story" + contentID + File.separator + "mapPoint" + mappointID);
                    String filePath = "story" + contentID + "/mapPoint" + mapPointID + "/video";
                    boolean fileSaved = contentElementManager.saveFile(file, filePath);

                    if (updatedStory != null && fileSaved) {
                        return new ResponseEntity<Story>(updatedStory, HttpStatus.OK);
                    } else {
                        return new ResponseEntity<Story>(HttpStatus.NO_CONTENT);
                    }
                } catch (Exception e) {
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                }

            } else if (fileType.substring(0, 5).equals("audio")) {
                updatedStory = contentElementManager.createAudioElement(language, file.getOriginalFilename(), file.getSize(), description, "audio", length1, mappointID, file.getContentType(), contentID);
                try {
                    //String fileName = file.getOriginalFilename();
                    //Path newFilePath = Paths.get(basePath + File.separator + "story" + contentID + File.separator + "mapPoint" + mappointID);
                    //saveFile(file.getInputStream(), newFilePath, fileType, fileName);
                    String filePath = "story" + contentID + "/mapPoint" + mapPointID + "/audio";
                    boolean fileSaved = contentElementManager.saveFile(file, filePath);

                    if (updatedStory != null && fileSaved) {
                        return new ResponseEntity<Story>(updatedStory, HttpStatus.OK);
                    } else {
                        return new ResponseEntity<Story>(HttpStatus.NO_CONTENT);
                    }
                } catch (Exception e) {
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                }
            } else {

                return new ResponseEntity(new ApiResponse(false, "invalid file type"), HttpStatus.BAD_REQUEST);
            }
        }
    }



    /**
     * NOT USED CURRENTLY (use for local file saving)
     * Method which saves the uploaded files into file system.
     * uploaded file could be image, audio, and video.
     * All type of files will be saved under webapp/uploads folder.
     *
     * @param inputStream an InputStream to read the contents of the file from.
     * @param path        path of the uploaded file.
     * @param fileType    type of the uploaded file.
     * @param fileName    name of the uploaded file.
     */
    /*
    private void saveFile(InputStream inputStream, Path path, String fileType, String fileName) {
        if (fileType.substring(0, 5).equals("image")) {
            try {
                File f = new File(path + File.separator + "image");
                if (!f.exists())
                    f.mkdirs();
                File i = new File(f.getAbsolutePath() + File.separator + fileName);
                OutputStream outputStream = new FileOutputStream(i);
                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
                outputStream.flush();
                outputStream.close();
                System.out.println("file exists?: " + i.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (fileType.substring(0, 5).equals("video")) {
            try {
                File f = new File(path + File.separator + "video");
                if (!f.exists())
                    f.mkdirs();

                File i = new File(f.getAbsolutePath() + File.separator + fileName);
                OutputStream outputStream = new FileOutputStream(i);
                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (fileType.substring(0, 5).equals("audio")) {
            try {
                File f = new File(path + File.separator + "audio");
                if (!f.exists())
                    f.mkdirs();
                File i = new File(f.getAbsolutePath() + File.separator + fileName);
                OutputStream outputStream = new FileOutputStream(i);
                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    */
    /**
     * Method needed for ResourceLoaderAware class.
     *
     * @param resourceLoader an object of ResourceLoader class.
     */
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
