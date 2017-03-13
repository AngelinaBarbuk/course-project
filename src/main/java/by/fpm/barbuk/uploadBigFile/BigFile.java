package by.fpm.barbuk.uploadBigFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by B on 02.12.2016.
 */
public class BigFile implements java.io.Serializable {

    private String id;
    private String fileName;
    private String type;
    private List<FilePart> parts = new ArrayList<>();
    private long size;

    public BigFile() {
        id = "" + UserFiles.counter++;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<FilePart> getParts() {
        return parts;
    }

    public void setParts(List<FilePart> parts) {
        this.parts = parts;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
