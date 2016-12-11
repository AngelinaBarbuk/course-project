package by.fpm.barbuk.support.web;

/**
 * Created by B on 04.12.2016.
 */
public class AjaxRequestBody {
//    public static final String ajaxRequestTypes[]={
//            "Open_Dir",
//            "Open_File"
//    };

    public String requestType;
    public String path;

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
