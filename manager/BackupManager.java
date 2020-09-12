package com.asylumproject.asylumproject.manager;

import com.asylumproject.asylumproject.problemdomain.User;
import com.asylumproject.asylumproject.reports.BackupRequest;
import com.asylumproject.asylumproject.reports.dropbox.DBoxManager;
import com.asylumproject.asylumproject.reports.responses.BackupGenerateResponse;
import com.smattme.MysqlExportService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

/**
 * This class handles all functionality related to backing up and restoring data on the website.
 */
@Service
public class BackupManager {

    private Properties properties = new Properties();

    /**
     * Setup database properties (connection string, username and password)
     * @param sendMail indication if an email copy was requested.
     * @param mailTo the destination email address.
     */
    private void settings(boolean sendMail, String mailTo) {
        //required properties for exporting of db
        properties.setProperty(MysqlExportService.JDBC_CONNECTION_STRING, "jdbc:mysql://ideapot.net:80/capstone");
        properties.setProperty(MysqlExportService.DB_USERNAME, "root");
        properties.setProperty(MysqlExportService.DB_PASSWORD, "example");

        properties.setProperty(MysqlExportService.PRESERVE_GENERATED_ZIP, "true");

        //set the outputs temp dir
        properties.setProperty(MysqlExportService.TEMP_DIR, new File("external").getPath());

        sendMail(sendMail, mailTo);
    }

    /**
     * Setup email properties to send a copy of the backup file.
     * @param sendMail indication if an email copy was requested.
     * @param mailTo the destination email address.
     */
    private void sendMail(boolean sendMail, String mailTo) {

        if(!sendMail) {
            properties.remove(MysqlExportService.EMAIL_HOST);
            properties.remove(MysqlExportService.EMAIL_PORT);
            properties.remove(MysqlExportService.EMAIL_USERNAME);
            properties.remove(MysqlExportService.EMAIL_PASSWORD);
            properties.remove(MysqlExportService.EMAIL_FROM);
            properties.remove(MysqlExportService.EMAIL_TO);
        }
        else {
            properties.setProperty(MysqlExportService.EMAIL_HOST, "smtp.gmail.com");
            properties.setProperty(MysqlExportService.EMAIL_PORT, "587");
            properties.setProperty(MysqlExportService.EMAIL_USERNAME, "asylum.project.2020@gmail.com");
            properties.setProperty(MysqlExportService.EMAIL_PASSWORD, "tdmqvgqelglelhge");
            properties.setProperty(MysqlExportService.EMAIL_FROM, "asylum.project.2020@gmail.com");
            properties.setProperty(MysqlExportService.EMAIL_TO, mailTo);
        }
    }

    /**
     * Generate a database backup file.
     * @param user the user requesting to backup the database.
     * @param backupRequest request indicating if a copy if the backup file sill be sent via email.
     * @return backup generate response of null if operation was not successful.
     */
    public BackupGenerateResponse generateBackupFile(User user, BackupRequest backupRequest) {
        settings(backupRequest.getSendMail(), user.getEmail());
        MysqlExportService mysqlExportService = new MysqlExportService(properties);
        try {
            mysqlExportService.export();
            File file = mysqlExportService.getGeneratedZipFile();
            String link = DBoxManager.uploadFile(DBoxManager.FileType.BACKUP, file, file.getName());
            // Erase local backup file
            mysqlExportService.clearTempFiles(false);
            return new BackupGenerateResponse(backupRequest.getSendMail(), link);
        } catch (IOException | SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Restore a database backup file from the cloud based storage service to the server.
     * @param path the path to the database backup file to restore.
     * @return true if operation was successful, otherwise false.
     */
    public boolean restoreDB(String path) {
        return DBoxManager.downloadFile(path) != null;
    }

}
