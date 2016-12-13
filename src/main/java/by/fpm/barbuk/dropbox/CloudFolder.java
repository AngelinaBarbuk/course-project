package by.fpm.barbuk.dropbox;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by B on 06.12.2016.
 */
public class CloudFolder implements Serializable {
    private String showName;
    private String root;
    private List<String> path;
    private String size;
    private boolean isDir;
    private int bytes;

    private List<CloudFile> content = new ArrayList<>();
    private List<CloudFile> folders = new ArrayList<>();
    private List<CloudFile> files = new ArrayList<>();

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public boolean isDir() {
        return isDir;
    }

    public void setDir(boolean dir) {
        isDir = dir;
    }

    public int getBytes() {
        return bytes;
    }

    public void setBytes(int bytes) {
        this.bytes = bytes;
    }

    public List<CloudFile> getContent() {
        return content;
    }

    public void setContent(List<CloudFile> content) {
        this.content = content;
    }

    public List<CloudFile> getFolders() {
        return folders;
    }

    public void setFolders(List<CloudFile> folders) {
        this.folders = folders;
    }

    public List<CloudFile> getFiles() {
        return files;
    }

    public void setFiles(List<CloudFile> files) {
        this.files = files;
    }

    public String getShowName() {
        return showName;
    }

    public void setShowName(String showName) {
        this.showName = showName;
    }
}
