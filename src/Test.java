import java.io.File;
import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException {
        File daw = new File("\"E:\\Программы\\Idea Projects\\DimaCodeBot\\FILE_SYSTEM_ROOT\\living_fish\\\"");
        System.out.println(pathFromFileSystemRoot(daw));
    }


    private static String pathFromFileSystemRoot(File file) {
        String FILE_SYSTEM_ROOT_PATH = "FILE_SYSTEM_ROOT";
        String path = file.getAbsolutePath().replaceAll("\"", "");
        int index = path.indexOf(FILE_SYSTEM_ROOT_PATH);
        if (index == -1)
            return null;
        //                     replacing file system root path,            removing last\   adding >
        path = (path.substring(index + FILE_SYSTEM_ROOT_PATH.length() + 1, path.length() - 1) + ">");
        return path;
    }

}
