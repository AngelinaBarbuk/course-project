package by.fpm.barbuk.uploadBigFile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by B on 05.03.2017.
 */
public class UserFiles implements Serializable {

    public static int counter = 0;

    private List<BigFile> bigFiles = new ArrayList<>();

    public List<BigFile> getBigFiles() {
        return bigFiles;
    }

    public void setBigFiles(List<BigFile> bigFiles) {
        this.bigFiles = bigFiles;
    }
}
