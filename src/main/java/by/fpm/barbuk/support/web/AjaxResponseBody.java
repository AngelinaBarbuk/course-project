package by.fpm.barbuk.support.web;

import by.fpm.barbuk.dropbox.CloudFile;

import java.io.Serializable;
import java.util.List;

/**
 * Created by B on 04.12.2016.
 */
public class AjaxResponseBody implements Serializable {

    public String path;
    public List<CloudFile> items;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<CloudFile> getItems() {
        return items;
    }

    public void setItems(List<CloudFile> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "AjaxResponseBody{" +
                "path='" + path + '\'' +
                ", items=" + items +
                '}';
    }
}
