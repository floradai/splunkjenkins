package org.jenkins.plugins.splunkjenkins;

/**
 * Created by fdai on 8/13/14.
 *
 * Defines an object that determines the artifact's destination on an machine that Splunk can retreive files from.
 */
public class Destination {

    private String path;

    public Destination(String path) {
        this.path = path;
    }

    /**
     * Creates a Destination object from @path
     *
     * @param path
     * @return Destination Object
     */
    public static Destination newDestFrom(String path) {
        return new Destination(path);
    }

    //Returns homePath up to directory location with "/"
    public String getHomePath() {
        return path.substring(0, path.lastIndexOf("/")+1);

    }

    //Returns destination directory
    public String getDestDir() {
        return path.substring(path.lastIndexOf("/")+1);
    }

    public String getPath() {
        return path;
    }




}
