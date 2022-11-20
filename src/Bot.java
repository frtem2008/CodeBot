//Main bot class for messaging and actions
// TODO: 15.11.2022 MODIFY /HELP TO NEW LOGIN MECHANICS

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class Bot extends TelegramLongPollingBot {
    private static boolean logMessagesToCreator = false;
    private final List<FileSystem> fileSystems;
    private final List<FileSystemUser> fileSystemUsers;

    private final List<String> bannedUsersIds;

    private final File fileSystemSaveFile;
    private final File fileBannedUsersIds;

    public Bot() {
        //creating all needed objects
        FileSystem.createRoot();

        fileSystems = new ArrayList<>();
        fileSystemUsers = new ArrayList<>();
        bannedUsersIds = new ArrayList<>();
        fileSystemSaveFile = new File("./FILESYSTEMS.dat");
        fileBannedUsersIds = new File("./BANNEDUSERS.dat");

        try {
            System.out.println("File for file systems storage " + (fileSystemSaveFile.createNewFile() ? "created" : "exists"));
            System.out.println("File for file banned users storage " + (fileBannedUsersIds.createNewFile() ? "created" : "exists"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (fileSystemSaveFile.length() > 0)
            if (!FileSystem.loadFromFile(fileSystems, fileSystemUsers, bannedUsersIds, fileSystemSaveFile, fileBannedUsersIds))
                System.exit(-1);


        for (FileSystem fileSystem : fileSystems) {
            System.out.println(fileSystem);
        }

        new Thread(this::console).start();

        System.out.println("BOT STARTED");
    }

    private void console() {
        Scanner s = new Scanner(System.in);
        System.out.println("[CONSOLE]Console thread started");
        String command;

        while (true) {
            switch (command = s.nextLine()) {
                case "/enablelogmessages" -> {
                    logMessagesToCreator = true;
                    System.out.println("[CONSOLE]Logging messages to @living_fish enabled");
                }
                case "/disablelogmessages" -> {
                    logMessagesToCreator = false;
                    System.out.println("[CONSOLE]Logging messages to @living_fish disabled");
                }
                case "/systems", "/filesystems" -> {
                    if (fileSystems.size() == 0)
                        System.out.println("[CONSOLE]No filesystems yet");
                    for (FileSystem fileSystem : fileSystems) {
                        System.out.println("[CONSOLE]" + fileSystem);
                    }
                    System.out.println();
                    if (fileSystemUsers.size() == 0)
                        System.out.println("[CONSOLE]No filesystem users yet");
                    for (FileSystemUser fileSystemUser : fileSystemUsers) {
                        System.out.println("[CONSOLE]" + "UserID: " + fileSystemUser);
                    }
                }
                case "/banlist", "bannedusers" -> {
                    if (bannedUsersIds.size() == 0)
                        System.out.println("[CONSOLE]No banned users yet");
                    for (String bannedUsersId : bannedUsersIds)
                        System.out.println("[CONSOLE]" + bannedUsersId);
                }
                case "/reloadfilesys" -> {
                    if (fileSystemSaveFile.length() > 0)
                        if (!FileSystem.loadFromFile(fileSystems, fileSystemUsers, bannedUsersIds, fileSystemSaveFile, fileBannedUsersIds))
                            System.exit(-1);
                    System.out.println("[CONSOLE]Filesystems reloaded");
                }
                case "/exit" -> {
                    System.out.println("[CONSOLE]Print yes to confirm");
                    if (s.nextLine().equals("yes"))
                        System.exit(0);
                    else
                        System.out.println("[CONSOLE]Exiting cancelled");
                }
                default -> {
                    System.out.println("COMMAND: " + command);
                    if (command.startsWith("/ban")) {
                        if (command.split(" ").length != 2) {
                            System.out.println("[CONSOLE]/Ban - wrong usage (expected one argument - id of a user to ban)");
                            continue;
                        }
                        if (bannedUsersIds.contains(command.split(" ")[1])) {
                            System.out.println("[CONSOLE]User " + command.split(" ")[1] + " is already banned");
                            continue;
                        }
                        System.out.println("[CONSOLE]Adding " + command.split(" ")[1] + " to a list of banned users...");
                        bannedUsersIds.add(command.split(" ")[1]);
                        System.out.println("[CONSOLE]Added " + command.split(" ")[1] + " to a list of banned users");
                    } else if (command.startsWith("/pardon") || command.startsWith("/unban")) {
                        if (command.split(" ").length != 2) {
                            System.out.println("[CONSOLE]/Pardon - wrong usage (expected one argument - id of a user to pardon)");
                            continue;
                        }
                        if (!bannedUsersIds.contains(command.split(" ")[1])) {
                            System.out.println("[CONSOLE]User " + command.split(" ")[1] + " is not banned");
                            continue;
                        }
                        System.out.println("[CONSOLE]Removing " + command.split(" ")[1] + " from a list of banned users...");
                        bannedUsersIds.remove(command.split(" ")[1]);
                        System.out.println("[CONSOLE]Removed " + command.split(" ")[1] + " from a list of banned users");
                    } else
                        System.out.println("Unknown command");
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "DimaCodeBot";
    }

    @Override
    public String getBotToken() {
        //getting bot configs, depending on testing mode
        return "BOT_TOKEN";
    }

    //main message handling method
    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                //received message
                Message inMess = update.getMessage();
                //Chat id to send the message
                String chatId = inMess.getChatId().toString();
                if (bannedUsersIds.contains(chatId)) {
                    SendMessage banned = new SendMessage(chatId, "YOU ARE BANNED! Text @living_fish, if it was a mistake");
                    execute(banned);
                    return;
                }
                SendMessage inMessageResend;
                if (logMessagesToCreator) {
                    inMessageResend = new SendMessage("1280356300", "FORWARDED MESSAGE FROM: " + inMess.getFrom().getUserName() + "(" + inMess.getFrom().getFirstName() + " " + inMess.getFrom().getLastName() + "):\n__________________________________________\n" + inMess.getText());
                    execute(inMessageResend);
                }
                //getting an answer for the message
                SendMessage response = parseMessage(inMess);

                System.out.println("Chat id:" + chatId + ", User: " + inMess.getFrom().getUserName());
                //responding only if we have an answer to the message
                //TEXT CAN BE NULL IGNORE TELEGRAM BOT API WARNINGS!!!
                if (response != null && !response.getText().isBlank()) {
                    response.setChatId(chatId);
                    System.out.println("Message: " + response.getText());
                    sendMessage(response);

                    if (logMessagesToCreator) {
                        response.setText("FORWARDED MESSAGE FROM[BOT ANSWER TO]: " + inMess.getFrom().getUserName() + "(" + inMess.getFrom().getFirstName() + " " + inMess.getFrom().getLastName() + "):\n__________________________________________\n" + response.getText());
                        response.setChatId("1280356300");
                        execute(response);
                    }
                }
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /* sending multiple messages properly */
    private void sendMessage(SendMessage toSend) throws TelegramApiException {
        if (toSend.getText().length() > 4096) {
            ArrayList<String> messages = new ArrayList<>();
            String msg = toSend.getText();

            int i = 0;
            while (i < msg.length()) {
                if (i < msg.length() - 4097) {
                    messages.add(msg.substring(i, i + 4096));
                    i += 4096;
                } else {
                    messages.add(msg.substring(i));
                    break;
                }
            }
            for (String s : messages)
                execute(new SendMessage(toSend.getChatId(), s));
        } else
            execute(toSend);
    }

    //parsing message method
    public SendMessage parseMessage(Message inMess) throws TelegramApiException {
        //a message the bot will send as answer to inMess
        SendMessage message = new SendMessage();
        String response = formResponse(inMess);

        //sending a non-blank message
        if (!response.isBlank())
            message.setText(response);
        else
            message.setText("");

        //saving changed file systems to disk
        FileSystem.saveToFile(fileSystems, fileSystemUsers, bannedUsersIds, fileSystemSaveFile, fileBannedUsersIds);
        System.out.println();

        return message;
    }

    private int containsByUser(List<FileSystemUser> users, User user) {
        for (int i = 0; i < users.size(); i++)
            if (users.get(i).getUser().equals(user))
                return i;

        return -1;
    }

    private String formResponse(Message inMess) {
        //text version of a received message for more clear code
        FileSystem sys;
        String textMsg = null;
        if (inMess.hasText())
            textMsg = inMess.getText();
        else if (inMess.hasDocument())
            textMsg = inMess.getCaption() == null ? "" : inMess.getCaption();
        //space split array of textMsg
        if (textMsg != null) {
            String[] splitSpace = textMsg.split(" ");
            User user = inMess.getFrom();
            String username = user.getUserName();

            if (splitSpace.length > 1 && !inMess.hasDocument()) //we have command and parameters and no file loading
                textMsg = textMsg.substring(0, textMsg.indexOf(' ')).toLowerCase(Locale.ROOT) + textMsg.substring(textMsg.indexOf(' '));

            if (textMsg.startsWith("/help")) {
                return """
                        commands when logged out:
                        /newfilesys <name> <pwd> <pwd> — new file system with name name and password pwd
                        /loadfilesys <name> <pwd> — load an existing file system
                        commands when logged in:
                        cd <path> — change active directory to path
                        mkdir <path> — create a directory and all subdirectories in path
                        ren "path1" "path2" — rename or move files or empty directories
                        del <path> — delete any file or empty directory
                        any document sent (exe,  txt, py, etc) if caption is empty — uploading to active directory, else - uploading to path, specified in caption\s
                        getfile <path> - get a file in path
                        /logout — log out from your file system
                        TODO:
                        cur — current directory path
                        """;
            } else if (textMsg.equals("/newfilesys")) {
                return "You must specify the password for your file system folder\nNOTE: YOU CAN NOT CHANGE YOUR PASSWORD!!!";
            } else if (textMsg.startsWith("/newfilesys")) {
                if (splitSpace.length != 4)
                    return "Invalid number of arguments, should be three";
                //"/newfilesys <name> <pwd> <pwd>"
                String name = splitSpace[1];
                String password1 = splitSpace[2];
                String password2 = splitSpace[3];
                if (password1.length() < 4 || password2.length() < 4)
                    return "Your password must contain at least 4 symbols";
                if (!password1.equals(password2))
                    return "Passwords are not identical";

                FileSystem newFileSystem = new FileSystem(name, password1);
                if (fileSystems.contains(newFileSystem))
                    return "This file system already exists";

                fileSystems.add(newFileSystem);
                fileSystemUsers.remove(containsByUser(fileSystemUsers, user));
                fileSystemUsers.add(new FileSystemUser(newFileSystem, user));

                return "File system registered, root is " + name + ", password is " + password1;

            } else if (textMsg.startsWith("/loadfilesys")) {
                //"/loadfilesys <name> <pwd>"
                if (splitSpace.length != 3)
                    return "Invalid number of arguments, should be one";

                String name = splitSpace[1];
                String password = splitSpace[2];
                FileSystem fileSystem = new FileSystem(name, password);
                System.out.println("FILE SYSTEM TO LOAD IS: " + fileSystem);
                for (int i = 0; i < fileSystems.size(); i++) {
                    System.out.println("FILE SYSTEMS " + i + " IS: " + fileSystems.get(i));
                }

                if (fileSystems.contains(fileSystem)) {
                    if (password.equals(fileSystem.getPassword())) {
                        FileSystemUser fsUser = new FileSystemUser(fileSystem, user);
                        int contains = containsByUser(fileSystemUsers, user);

                        if (contains != -1)
                            fileSystemUsers.get(contains).setFileSystem(fileSystem);
                        else
                            fileSystemUsers.add(fsUser);

                        return "Loaded file system " + fileSystem.getName();
                    } else
                        return "Wrong password";
                } else
                    return "This file system does not exist";
            } else {
                if (containsByUser(fileSystemUsers, user) == -1) {
                    return "You don't have a filesystem to operate!\nUse /newfilesys <password> <password> to create your file system, or /loadfilesys <password> to load an existing one";
                }

                sys = fileSystemUsers.get(containsByUser(fileSystemUsers, user)).getFileSystem();
                if (textMsg.equals("/logout"))
                    return "Logged out " + (fileSystemUsers.remove(fileSystemUsers.get(containsByUser(fileSystemUsers, user))) ? "successfully" : "failed");

                if (textMsg.startsWith("cd")) {
                    String path = textMsg.split(" ")[1];
                    return sys.cd(path);
                } else if (textMsg.equalsIgnoreCase("dir")) {
                    return sys.dirInfo();
                } else if (textMsg.startsWith("getfile")) {
                    try {
                        if (textMsg.startsWith("getfiles")) {
                            sys.getAllFiles(inMess);
                            return "";
                        } else {
                            if (textMsg.split("getfile").length > 1)
                                return sys.getFile(inMess, textMsg.split("getfile")[1]);
                            return "Invalid file name";
                        }
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                } else if (textMsg.startsWith("mkdir")) {
                    String dirPath = textMsg.split("mkdir")[1].trim();
                    return sys.createDir(dirPath) ? "Created a new dir" : "Failed to create a new dir";
                } else if (textMsg.startsWith("del")) {
                    String toDelete = textMsg.split("del")[1].trim();
                    return sys.deleteFile(toDelete);
                } else if (textMsg.startsWith("ren")) {
                    String[] quoteSplit = textMsg.split("\"");
                    if (quoteSplit.length < 4)
                        return "Invalid number of arguments - should be 2\nUsage: ren \"from\" \"to\"";

                    String from = textMsg.split("\"")[1].trim();
                    String to = textMsg.split("\"")[3].trim();
                    from = from.replaceAll("\"", "");
                    to = to.replaceAll("\"", "");
                    return switch (sys.rename(from, to)) {
                        case -100 -> from + " doesn't exist";
                        case -1 -> "Failed to rename " + from + " to " + to;
                        case -2 -> "Failed to move " + from + " to " + to;
                        case 1 -> from + " renamed to " + to;
                        case 2 -> from + " moved to " + to;
                        default -> throw new IllegalStateException("Unexpected value: " + sys.rename(from, to));
                    };
                } else if (inMess.hasDocument()) {
                    String fileName;
                    if (!textMsg.isBlank()) {
                        fileName = textMsg;
                    } else
                        fileName = null;
                    try {
                        String res = sys.downloadDocument(inMess, fileName);
                        if (res.contains("THIS FILE IS A VIRUS")) {
                            bannedUsersIds.add(String.valueOf(user.getId()));
                            System.out.println("Added " + user.getId() + " to list of banned users");
                            return res;
                        }
                        if (fileName == null || fileName.isBlank())
                            return res;
                        else
                            return fileName + " successfully uploaded to " + res;
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                        return "Failed to upload file";
                    }
                }
            }
        }

        return "Unknown command";
    }
}

class FileSystemUser implements Serializable {
    public static final long serialVersionUID = 30L;
    private FileSystem fileSystem;
    private User user;

    public FileSystemUser(FileSystem fileSystem, User user) {
        this.fileSystem = fileSystem;
        this.user = user;
    }

    @Override
    public String toString() {
        return "FileSystemUser{" +
                "fileSystem=" + fileSystem +
                ", user=" + user +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileSystemUser that)) return false;
        return Objects.equals(getFileSystem(), that.getFileSystem()) && Objects.equals(getUser(), that.getUser());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileSystem(), getUser());
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}