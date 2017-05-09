package by.fpm.barbuk.cloudEntities;

import java.text.DecimalFormat;

/**
 * Created by B on 08.05.2017.
 */
public class SizeMapper {

    private static final String[] sizes = {"Bytes", "KB", "MB", "GB", "TB"};
    private static final DecimalFormat decimalFormat =new DecimalFormat("#0.00");

    public static String mapSize(long bytes){
        if(bytes==0)
            return "0 Bytes";
        int i = (int) Math.floor(Math.log(bytes) / Math.log(1024));
        return decimalFormat.format(bytes / Math.pow(1024, i)) + sizes[i];
    }

}
