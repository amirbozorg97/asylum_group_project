package com.asylumproject.asylumproject.broker;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.*;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.asylumproject.asylumproject.dataaccess.ContentElementDB;
import com.asylumproject.asylumproject.dataaccess.LanguageDB;
import com.asylumproject.asylumproject.dataaccess.MapPointDB;
import com.asylumproject.asylumproject.problemdomain.Language;
import com.asylumproject.asylumproject.problemdomain.Story;
import com.asylumproject.asylumproject.reports.PieReportData;
import com.asylumproject.asylumproject.reports.ReportElement;
import com.asylumproject.asylumproject.problemdomain.ContentElement;
import com.asylumproject.asylumproject.problemdomain.MapPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.persistence.Tuple;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ContentElementBroker {

    private ContentElementDB contentElementDB;
    private LanguageDB languageDB;
    private MapPointDB mapPointDB;

    private AmazonS3 s3client;

    @Value("${aws.endpointUrl}")
    private String endpointUrl;
    @Value("${aws.bucketName}")
    private String bucketName;
    @Value("${aws.accessKey}")
    private String accessKey;
    @Value("${aws.secretKey}")
    private String secretKey;

    /**
     * a constructor for ContentElementBrokerclass.
     *
     * @param contentElementDB an object of ContentElementDB class.
     * @param mapPointDB an object of MapPointDB class.
     * @param languageDB an object of LanguageDB class.
     */
    @Autowired
    public ContentElementBroker(ContentElementDB contentElementDB, MapPointDB mapPointDB, LanguageDB languageDB){
        this.contentElementDB = contentElementDB;
        this.mapPointDB = mapPointDB;
        this.languageDB = languageDB;
    }

    /**
     * Used to initialize the AWS connection.
     */
    @PostConstruct
    private void initializeAmazon() {
        System.out.println("access key: " + this.accessKey);
        System.out.println("secret key: " + this.secretKey);
        BasicAWSCredentials credentials = new BasicAWSCredentials(this.accessKey, this.secretKey);
        this.s3client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials))
                                                        .withRegion(Regions.US_EAST_2)
                                                        .build();
    }

    /**
     * save passed content element to the database.
     *
     * @param element the content element object that is saved into the database.
     *
     * @return it returns a content element object to the ContentElementManager class.
     */
    public ContentElement createContentElement(ContentElement element) {
        return contentElementDB.save(element);
    }

    /**
     * Modify/update the information of an existing Content element.
     * @param element the Content element to update.
     * @return the Content element object updated.
     */
    public ContentElement editContentElement(ContentElement element) {
        return contentElementDB.save(element);
    }

    /**
     * Change the 'active' attribute of a Content element to true.
     * @param element the Content element to enable.
     * @return the Content element enabled.
     */
    public ContentElement enableContentElement(ContentElement element) {
        return null;
    }

    /**
     * Change the 'active' attribute of a Content element to false.
     * @param element the Content element to disable.
     * @return the Content element disabled.
     */
    public ContentElement disableContentElement(ContentElement element) {
        return null;
    }

    /**
     * Retrieve a collection of Content elements related to a content object.
     * @param mapPoint the content ID to retrieve related content elements.
     * @return a collection of Content elements related to the provided contentID.
     */
//    public List<ContentElement> getContentElements(MapPoint mapPoint) {
//        return contentElementDB.findAllByFileTypeAndMappoint("image", mapPoint);
//    }


    /**
     * retrieve archived content elements from database.
     *
     * @param mapPoint the map point object that is used to get archived content elements from database.
     *
     * @return it returns a list of archived content elements to the ContentElementManager class.
     */
    public List<ContentElement> getArchivedElements(MapPoint mapPoint){
        return contentElementDB.findAllByMappointAndStateAndDeletedFalse(mapPoint, "archived");
    }


    /**
     * set a content element as archived in database.
     *
     * @param elementID the id that is used to get content element from database.
     * @param action the action that is passed by the user to change the state of content element.
     *
     * @return it returns a content element object to the ContentElementManager class.
     */
    public ContentElement setArchivedElement(int elementID, String action){
       Optional<ContentElement> element = contentElementDB.findById(elementID);
        if(element.isPresent()){
            ContentElement archived = element.get();
            if (action.equals("archive")){
                archived.setState("ARCHIVED");
            }else{
                archived.setState("DRAFT");
            }
            return contentElementDB.save(archived);
        }else{
            return null;
        }

    }


    /**
     * retrieve text content elements from database that are under passed map point object.
     *
     * @param mapPoint the map point object that is used to get the text elements from database.
     *
     * @return it returns a list of text content elements to the ContentElementManager class.
     */
    public List<ContentElement> getTexts(MapPoint mapPoint){
        return contentElementDB.findAllByFileTypeAndMappointAndDeletedFalse("text", mapPoint);
    }


    /**
     * retrieve all content elements from database that are under passed map point object.
     *
     * @param mapPoint the map point object that is used to get the all elements from database.
     *
     * @return it returns a list of all content elements to the ContentElementManager class.
     */
    public List<ContentElement> getAll(MapPoint mapPoint){
        return contentElementDB.findAllByMappointAndDeletedFalse(mapPoint);
    }

    /**
     * retrieve image content elements from database that are under passed map point object.
     *
     * @param mapPoint the map point object that is used to get the image elements from database.
     *
     * @return it returns a list of image content elements to the ContentElementManager class.
     */
    public List<ContentElement> getImages(MapPoint mapPoint)
    {
        return contentElementDB.findAllByFileTypeAndMappointAndDeletedFalse("image", mapPoint);
    }


    /**
     * retrieve audio content elements from database that are under passed map point object.
     *
     * @param mapPoint the map point object that is used to get the audio elements from database.
     *
     * @return it returns a list of audio content elements to the ContentElementManager class.
     */
    public List<ContentElement> getAudios(MapPoint mapPoint)
    {
        return contentElementDB.findAllByFileTypeAndMappointAndDeletedFalse("audio", mapPoint);
    }


    /**
     * retrieve video content elements from database that are under passed map point object.
     *
     * @param mapPoint the map point object that is used to get the video elements from database.
     *
     * @return it returns a list of video content elements to the ContentElementManager class.
     */
    public List<ContentElement> getVideos(MapPoint mapPoint)
    {
        return contentElementDB.findAllByFileTypeAndMappointAndDeletedFalse("video", mapPoint);
    }


    /**
     * remove passed content elements from database.
     *
     * @param contentElement the content element object that is removed from database.
     *
     * @return it returns true if the deletion process was successful.
     */
    public boolean deleteContentElement(ContentElement contentElement){
        try{
            contentElementDB.delete(contentElement);
            return true;
        }catch(IllegalArgumentException ex){
            return  false;
        }
    }


    /**
     * retrieve language object from database based on passed code.
     *
     * @param code the code that is used to get language from database.
     *
     * @return it returns a language object to the ContentElementManager class.
     */
    public Language getLanguage(String code){
        System.out.println(code);
        return languageDB.findByCode(code);
    }


    /**
     * save map point object to the database.
     *
     * @param mapPoint map point object that is saved to the database.
     *
     * @return it returns a map point object to the ContentElementManager class.
     */
    public MapPoint createMapPoint(MapPoint mapPoint){
        return mapPointDB.save(mapPoint);
    }


    /**
     * retrieve map point object from database based on map point id.
     *
     * @param mapPointID map point id that is used to get map point object from database.
     *
     * @return it returns a map point object to the ContentElementManager class.
     */
    public MapPoint getMapPoint(int mapPointID){
        return mapPointDB.findById(mapPointID);
    }

    /**
     * Retrieve a Content element based on it's element ID.
     * @param elementID the elementID to search for and retrieve it's corresponding ContentElement.
     * @return the ContentElement that matches the provided elementID.
     */
    public Optional<ContentElement> getContentElement(int elementID) {
        return contentElementDB.findById(elementID);
    }


    /**
     *
     *
     * @return
     */
    public List<ReportElement> getReportExistingElements() {
        return contentElementDB.findContentsReport();
    }

    public List<Tuple> getStorageReport() {
        return contentElementDB.findStorageReport();
    }


    /**
     * edit passed map point object.
     *
     * @param mapPoint the map point object that is updated.
     *
     * @return it returns a updated map point object to the ContentElementManager class.
     */
    public MapPoint editMapPoint(MapPoint mapPoint) {
        return mapPointDB.save(mapPoint);
    }

    /**
     * retrieve all of the map point objects from database based on story object.
     *
     * @param story the story object that is used to get all map points under that.
     *
     * @return it returns a list of map point objects to the ContentElementManager class.
     */
    public List<MapPoint> getAllByStory(Story story){
        return mapPointDB.findAllByStoryAndDeletedFalse(story);
    }


    /**
     * retrieve all of the map point objects from database.
     *
     * @return it returns a list of map point objects to the ContentElementManager class.
     */
    public List<MapPoint> getAllMapPoints() {
        return (List<MapPoint>) mapPointDB.findAll();
    }


    /**
     * remove passed map point object from database.
     *
     * @param mapPoint the map point object that is removed from database.
     */
    public void deleteMapPoint(MapPoint mapPoint) {
        mapPointDB.delete(mapPoint);
    }


    /**
     * check to see if the passed file path already exists or not.
     *
     * @param filePath the file path that is checked.
     *
     * @return it returns true if the passed file exists in the database.
     */
    public boolean checkFileNameExists(String filePath, String mapPointID) {
        return contentElementDB.existsByFilePathAndMappointId(filePath, Integer.parseInt(mapPointID));
    }

    public boolean saveFile(MultipartFile multipartFile, String filePath) throws IOException {
            File file = convertMultiPartToFile(multipartFile);
            System.out.println("converted file: " + file);
            uploadFileTos3bucket(filePath + "/" + multipartFile.getOriginalFilename(), file);
            file.delete();
            return true;
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    private void uploadFileTos3bucket(String filePath, File file) {
        try {
            System.out.println("filepath: " + filePath);
            System.out.println("S3 account owner: " + s3client.getS3AccountOwner());
            s3client.putObject(new PutObjectRequest(bucketName, filePath, file)
                    .withCannedAcl(CannedAccessControlList.PublicRead));
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
