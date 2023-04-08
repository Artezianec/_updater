import java.io.*;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main extends JFrame {

    private static final long serialVersionUID = 1L;

    private String server = "localhost";
    private int port = 21;
    private String user = "user";
    private String pass = "pass";
    private static FTPClient ftpClient = null;
    private boolean connected = false;
    private String localDir = "local_dir";
    private String remoteDir = "remote_dir";
    private Properties props = null;
    private File propFile = null;
    private JTextArea textArea;
    private JButton button;

    private boolean connect() {
        try {
            ftpClient = new FTPClient();
            ftpClient.connect(server, port);
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                return false;
            }
            connected = ftpClient.login(user, pass);
            if (!connected) {
                ftpClient.disconnect();
                return false;
            }
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            return true;
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void disconnect() {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
                connected = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<String> listFiles(String dir) {
        List<String> fileList = new ArrayList<String>();
        try {
            FTPFile[] ftpFiles = ftpClient.listFiles(dir);
            for (FTPFile ftpFile : ftpFiles) {
                if (ftpFile.isFile()) {
                    fileList.add(ftpFile.getName());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileList;
    }

    private boolean downloadFile(String remoteFile, String localFile) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(localFile);
            if (ftpClient.retrieveFile(remoteFile, fos)) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public static void compareFiles(File[] localFiles, FTPFile[] ftpFiles) {
        List<String> filesToUpdate = new ArrayList<String>();
        for (File localFile : localFiles) {
            for (FTPFile ftpFile : ftpFiles) {
                if (localFile.getName().equals(ftpFile.getName()) && localFile.lastModified() < ftpFile.getTimestamp().getTimeInMillis()) {
                    filesToUpdate.add(localFile.getName());
                }
            }
        }
        if (filesToUpdate.size() > 0) {
            String message = "The following files need to be updated: " + String.join(", ", filesToUpdate);
            updateFiles(localFiles, ftpFiles, message);
        } else {
            System.out.println("All files are up to date.");
        }
    }

    public static void updateFiles(File[] localFiles, FTPFile[] ftpFiles, String message) {
        int reply;
        try {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            for (FTPFile ftpFile : ftpFiles) {
                for (File localFile : localFiles) {
                    if (ftpFile.getName().equals(localFile.getName()) && ftpFile.getTimestamp().getTimeInMillis() > localFile.lastModified()) {
                        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localFile));
                        ftpClient.retrieveFile(ftpFile.getName(), outputStream);
                        outputStream.close();
                    }
                }
            }
            System.out.println("Files updated successfully.");
            System.out.println("Starting application...");
            runApplication();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void runApplication() {
        try {
            Runtime.getRuntime().exec("java -jar myApplication.jar");
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

}
