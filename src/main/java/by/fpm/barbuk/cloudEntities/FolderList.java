package by.fpm.barbuk.cloudEntities;

import java.util.List;

/**
 * Created by B on 18.12.2016.
 */
public class FolderList {

    private List<CloudFile> folders;
    private String prevFolder;

    public List<CloudFile> getFolders() {
        return folders;
    }

    public void setFolders(List<CloudFile> folders) {
        this.folders = folders;
    }

    public String getPrevFolder() {
        return prevFolder;
    }

    public void setPrevFolder(String prevFolder) {
        this.prevFolder = prevFolder;
    }
}
