import com.kanishka.virustotal.dto.FileScanReport;
import com.kanishka.virustotal.dto.ScanInfo;
import com.kanishka.virustotal.exception.APIKeyNotFoundException;
import com.kanishka.virustotal.exception.QuotaExceededException;
import com.kanishka.virustotal.exception.UnauthorizedAccessException;
import com.kanishka.virustotalv2.VirusTotalConfig;
import com.kanishka.virustotalv2.VirustotalPublicV2;
import com.kanishka.virustotalv2.VirustotalPublicV2Impl;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class FileSystem implements Serializable {
    public static final long serialVersionUID = 42L;


    private static final File FILE_SYSTEM_ROOT = new File("FILE_SYSTEM_ROOT");
    public static String FILE_SYSTEM_ROOT_PATH = null;

    private final String FILE_SYSTEM_NAME;
    private final String FILE_SYSTEM_PASSWORD;

    private final File ROOT_DIR;
    private final String ROOT_DIR_PATH;
    private File currentDir;

    public FileSystem(String username, String password) {
        if (FILE_SYSTEM_ROOT_PATH == null)
            except("UNABLE TO CREATE A USER DIRECTORY: file system root does not exist");

        this.FILE_SYSTEM_NAME = username;
        this.FILE_SYSTEM_PASSWORD = password;
        this.ROOT_DIR = new File(FILE_SYSTEM_ROOT_PATH + "/" + username);


        if (!this.ROOT_DIR.exists() && !this.ROOT_DIR.mkdir())
            except("UNABLE TO CREATE A USER DIRECTORY: failed to create a user directory: " + ROOT_DIR.getAbsolutePath());

        this.ROOT_DIR_PATH = ROOT_DIR.getAbsolutePath();
        this.currentDir = ROOT_DIR;
        log("Creating directory for " + username + " done in: " + ROOT_DIR_PATH);
        log("Creating new file system for " + username + " done");
    }

    public static void createRoot() {
        if (!FILE_SYSTEM_ROOT.isDirectory()) {
            FILE_SYSTEM_ROOT_PATH = FILE_SYSTEM_ROOT.getAbsolutePath();
            if (!FILE_SYSTEM_ROOT.mkdirs())
                except("Failed to create file system root");
            else
                log("File system root created: " + FileSystem.FILE_SYSTEM_ROOT_PATH);
            return;
        }

        FILE_SYSTEM_ROOT_PATH = FILE_SYSTEM_ROOT.getAbsolutePath();
        log("File system root exists");
    }

    public static void log(String log) {
        System.out.println("[FILE SYSTEM]" + log);
    }

    public static void except(String cause) {
        System.err.println("[FILE SYSTEM]" + cause);
        throw new RuntimeException("[FILE SYSTEM]" + cause);
    }

    private static String pathFromFileSystemRoot(File file) throws IndexOutOfBoundsException {
        String path = file.getAbsoluteFile().getAbsolutePath().replaceAll("\"", "");
        if (!path.endsWith("\\"))
            path += "\\";
        int index = path.indexOf(FILE_SYSTEM_ROOT_PATH);
        if (index == -1)
            return null;
        //                     replacing file system root path,            removing last\
        if (index + FILE_SYSTEM_ROOT_PATH.length() + 1 >= path.length())
            return "<You don't have access to file system root dir!!!";

        path = (path.substring(index + FILE_SYSTEM_ROOT_PATH.length() + 1, path.length() - 1));
        log("Path from fs root for " + file.getAbsolutePath() + " is " + path);
        path = path.replaceAll("\\\\\\.\\.", "");
        return path;
    }

    public static FileSystem getByName(List<FileSystem> fileSystemList, String name) {
        for (FileSystem fileSystem : fileSystemList) {
            if (fileSystem.FILE_SYSTEM_NAME.equals(name))
                return fileSystem;
        }
        return null;
    }

    public static boolean saveToFile(List<FileSystem> fileSystems, List<FileSystemUser> fileSystemUsers, List<String> bannedUsers, File fileSystemFile, File bannedUsersFile) {
        try {
            log("Creating new file for file system saving: " + fileSystemFile.createNewFile());
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileSystemFile));
            oos.writeObject(fileSystems);
            oos.writeObject(fileSystemUsers);
            oos.flush();
            log("Saved file system list to " + fileSystemFile.getAbsolutePath());
            log("Saving banned users to: " + fileSystemFile.getAbsolutePath());
            oos = new ObjectOutputStream(new FileOutputStream(bannedUsersFile.getAbsolutePath()));
            oos.writeObject(bannedUsers);
            oos.flush();
            log("Saved banned users to " + bannedUsersFile.getAbsolutePath());
            oos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean loadFromFile(List<FileSystem> fileSystems, List<FileSystemUser> fileSystemUsers, List<String> bannedUsers, File fileSystemFile, File bannedUsersFile) {
        try {
            log("Loading file systems info from: " + fileSystemFile.getAbsolutePath());
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileSystemFile));
            fileSystems.addAll((List<FileSystem>) ois.readObject());
            fileSystemUsers.addAll((List<FileSystemUser>) ois.readObject());
            log("Loaded " + fileSystems.size() + " file systems from " + fileSystemFile.getAbsolutePath());
            if (bannedUsersFile.length() > 0) {
                log("Loading banned users from: " + fileSystemFile.getAbsolutePath());
                ois = new ObjectInputStream(new FileInputStream(bannedUsersFile.getAbsolutePath()));
                bannedUsers.addAll((List<String>) ois.readObject());
                log("Loaded " + bannedUsers.size() + " banned users from " + bannedUsersFile.getAbsolutePath());
            }
            ois.close();
            return true;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static int countChars(String str, char c) {
        int cnt = 0;

        for (int i = 0; i < str.length(); i++)
            cnt += str.charAt(i) == c ? 1 : 0;
        return cnt;
    }

    public static boolean scanFile(File toScan) {
        try {
            VirusTotalConfig.getConfigInstance().setVirusTotalAPIKey("e5ab0771c7be4c3d10587f99f36630826482ed0523c5e014da489ac94b2c2bee");
            VirustotalPublicV2 virusTotalRef = new VirustotalPublicV2Impl();

            ScanInfo scanInformation = virusTotalRef.scanFile(toScan);

            System.out.println("___SCAN INFORMATION___");
            System.out.println("MD5 :\t" + scanInformation.getMd5());
            System.out.println("Perma Link :\t" + scanInformation.getPermalink());
            System.out.println("Resource :\t" + scanInformation.getResource());
            System.out.println("Scan Id :\t" + scanInformation.getScanId());
            System.out.println("___SCAN DONE___");
            //waiting until report is done

            FileScanReport report = virusTotalRef.getScanReport(scanInformation.getResource());
            //waiting until report is done
            System.out.println("Waiting for a report...");
            while (report.getPositives() == null) {
                report = virusTotalRef.getScanReport(scanInformation.getResource());
                Thread.sleep(5000);
            }
            System.out.println("Report arrived:");
            System.out.println("MD5 :\t" + report.getMd5());
            System.out.println("Perma link :\t" + report.getPermalink());
            System.out.println("Scan Date :\t" + report.getScanDate());
            System.out.println("Scan Id :\t" + report.getScanId());
            System.out.println("Response Code :\t" + report.getResponseCode());

            System.out.println("Positives :\t" + report.getPositives());
            System.out.println("Total :\t" + report.getTotal());
            if (report.getPositives() > report.getTotal() * 0.05) { //bigger then 5% of total scans
                log("FILE " + toScan.getAbsolutePath() + " IS A VIRUS!!!");
                log("DELETING FILE: " + toScan.getAbsolutePath());
                log("DELETING FILE: " + (toScan.delete() ? "success" : "failed"));
                return true;
            }
            return false;
        } catch (APIKeyNotFoundException ex) {
            System.err.println("API Key not found! " + ex.getMessage());
        } catch (UnsupportedEncodingException ex) {
            System.err.println("Unsupported Encoding Format!" + ex.getMessage());
        } catch (UnauthorizedAccessException ex) {
            System.err.println("Invalid API Key " + ex.getMessage());
        } catch (QuotaExceededException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return true; //return true and stop if scan failed
    }

    public String getName() {
        return FILE_SYSTEM_NAME;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileSystem that)) return false;
        return FILE_SYSTEM_NAME.equals(that.FILE_SYSTEM_NAME) && FILE_SYSTEM_PASSWORD.equals(that.FILE_SYSTEM_PASSWORD) && ROOT_DIR.equals(that.ROOT_DIR) && ROOT_DIR_PATH.equals(that.ROOT_DIR_PATH);
    }

    @Override
    public int hashCode() {
        return Objects.hash(FILE_SYSTEM_NAME, FILE_SYSTEM_PASSWORD, ROOT_DIR, ROOT_DIR_PATH);
    }

    @Override
    public String toString() {
        return "FileSystem{" +
                "FILE_SYSTEM_NAME='" + FILE_SYSTEM_NAME + '\'' +
                ", FILE_SYSTEM_PASSWORD='" + FILE_SYSTEM_PASSWORD + '\'' +
                ", ROOT_DIR_PATH='" + ROOT_DIR_PATH + '\'' +
                '}';
    }

    public String getPassword() {
        return FILE_SYSTEM_PASSWORD;
    }

    private File getByPath(String path) {
        if (path.startsWith(FILE_SYSTEM_NAME))
            return new File(FILE_SYSTEM_ROOT_PATH + "/" + path.replaceAll("\"", ""));
        else
            return new File(FILE_SYSTEM_ROOT_PATH + "/" + pathFromFileSystemRoot(currentDir) + "/" + path.replaceAll("\"", ""));
    }

    public String dirInfo() {
        StringBuffer res = new StringBuffer();
        File[] files = currentDir.listFiles();
        if (files == null || files.length == 0)
            return getCurrentDirLocalPath() + ">\n" + "This directory is empty";

        Date lastMod;
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy  HH:mm:ss");
        res.append(getCurrentDirLocalPath()).append(">\n");
        for (File file : files) {
            lastMod = new Date(file.lastModified());
            sdf.format(lastMod, res, new FieldPosition(0));

            if (file.isDirectory()) {
                res.append("    <DIR>         ");
            } else {
                res.append("             ").append(String.format("%8d", file.length())).append(" ");
            }
            res.append(file.getName());
            res.append('\n');
        }
        return res.toString();
    }

    //returns true if current dir was changed
    public String cd(String path) {
        File toChange;
        if (path.startsWith(FILE_SYSTEM_NAME)) { //absolute path
            toChange = new File(FILE_SYSTEM_ROOT_PATH + "/" + path.replaceAll("\"", ""));
        } else { //local path (.. handling and so on)
            if (path.endsWith("..") && !path.contains("\\")) {
                String pathUp = pathFromFileSystemRoot(currentDir.getParentFile());
                if (pathUp != null && pathUp.contains("<You don't have access to file system root dir!!!"))
                    return pathUp.substring(1); //"deleting <"
                toChange = new File(FILE_SYSTEM_ROOT_PATH + "/" + pathUp);
            } else if (path.trim().equals("."))
                toChange = currentDir;
            else if (countChars(path, '.') > 2)
                return "Invalid path";
            else
                toChange = new File(FILE_SYSTEM_ROOT_PATH + "/" + getCurrentDirLocalPath() + "/" + path.replaceAll("\"", ""));
        }

        if (toChange.isDirectory()) {
            log("User " + FILE_SYSTEM_NAME + " changed dir from " + currentDir.getAbsolutePath() + " to " + FILE_SYSTEM_ROOT_PATH + "/" + path);
            currentDir = toChange;
            return getCurrentDirLocalPath() + ">";
        } else {
            log("User " + FILE_SYSTEM_NAME + " didn't change dir from " + currentDir.getAbsolutePath() + " to " + FILE_SYSTEM_ROOT_PATH + "/" + path);
            return "Dir " + path + " does not exist";
        }
    }

    public String getFile(Message inMess, String path) throws TelegramApiException {
        path = path.trim();
        File toGet = getByPath(path);

        if (!toGet.exists())
            return "File " + pathFromFileSystemRoot(toGet) + " doesn't exist";
        if (!toGet.isFile())
            return "Can not get a directory: " + pathFromFileSystemRoot(toGet);

        SendDocument document = new SendDocument(String.valueOf(inMess.getChatId()), new InputFile(toGet));
        document.setCaption(toGet.getName() + ":");
        log("Sending file: " + (Main.bot.execute(document) != null ? "Success" : "failure"));
        return "";
    }

    public void getAllFiles(Message inMess) throws TelegramApiException {
        for (int i = 0; i < currentDir.listFiles().length; i++) {
            if (currentDir.listFiles()[i].isFile())
                getFile(inMess, currentDir.listFiles()[i].getName());
        }
    }

    public boolean createDir(String dirPath) {
        return getByPath(dirPath).mkdirs();
    }

    public int rename(String from, String to) {
        File toRename = getByPath(from);
        File newName = getByPath(to);
        if (!toRename.exists())
            return -100; //file doesn't exist
        else if (!toRename.renameTo(newName)) {
            if (toRename.getParentFile().equals(newName.getParentFile()))
                return -1; //failed to rename
            else
                return -2; //failed to move
        } else if (toRename.getParentFile().equals(newName.getParentFile()))
            return 1; //file renamed
        else
            return 2; // file moved
    }

    public String deleteFile(String deletePath) {
        File del = getByPath(deletePath);
        if (!del.exists())
            return "File " + pathFromFileSystemRoot(del) + " doesn't exist";
        boolean dir = del.isDirectory();
        return del.delete() ? (dir ? "Dir " : "File ") + pathFromFileSystemRoot(del) + " deleted" : "Failed to delete " + pathFromFileSystemRoot(del);
    }

    public String downloadDocument(Message inMess, String fileName) throws TelegramApiException {
        if (inMess.hasDocument()) {
            log("Downloading document: " + inMess.getDocument().getFileName());
            //generating properties
            String doc_id = inMess.getDocument().getFileId();

            //downloading
            GetFile getFile = new GetFile();
            getFile.setFileId(doc_id);
            org.telegram.telegrambots.meta.api.objects.File file = Main.bot.execute(getFile);
            if (fileName == null) {
                fileName = inMess.getDocument().getFileName();
            }
            Main.bot.downloadFile(file, new File(currentDir.getAbsolutePath() + "/" + fileName));
            log("Downloaded " + inMess.getDocument().getFileName() + " in " + currentDir.getAbsolutePath() + "/" + fileName);
            //archive checks
            long signature = 0;
            try {
                RandomAccessFile checkFile = new RandomAccessFile(new File(currentDir.getAbsolutePath() + "/" + fileName), "r");
                signature = checkFile.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //zip file detection
            if (signature == 0x504B0304 || signature == 0x504B0506 || signature == 0x504B0708)
                return "Archives are not supported because of security reasons";

            if (scanFile(new File(currentDir.getAbsolutePath() + "/" + fileName)))
                return "THIS FILE IS A VIRUS!!! YOU ARE BANNED! Text @living_fish, if it was a mistake";
            return "File successfully uploaded to " + pathFromFileSystemRoot(new File(currentDir.getAbsolutePath() + "/" + fileName));
        }
        return "";
    }

    public String getCurrentDirLocalPath() {
        return pathFromFileSystemRoot(currentDir.getAbsoluteFile());
    }
}
