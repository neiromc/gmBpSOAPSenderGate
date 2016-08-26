package ru.geekmonkey.bp.models;

/**
 * Created by neiro on 18.07.16.
 */
public class ResourceFileName {

    private String fileName;

    public ResourceFileName(String configFileName) {
        this.fileName = configFileName;
    }

    public String getFileNameFull() {
        return getClass().getResource("/"+ fileName).getFile();
    }

    public String getFileName() {
        return fileName;
    }
}
