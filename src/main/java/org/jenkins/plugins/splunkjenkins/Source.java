package org.jenkins.plugins.splunkjenkins;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import jenkins.model.StandardArtifactManager;
import jenkins.util.VirtualFile;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by fdai on 8/13/14.
 *
 * Defines an object that determines the artifact's destination on the build machine that Splunk can retreive files from.
 */
public class Source {


    private String projectName;
    private int buildID;
    private String url;
    private VirtualFile srcPath;

    public Source(String projectName, int buildID, VirtualFile path){
        this.projectName = projectName;
        this.buildID = buildID;
        this.url = "/jobs/" + projectName + "/" + buildID + "/";
        this.srcPath = path;

    }

    /**
     * Creates a Source from a Run (also a build) if the @run's path is readable and a directory
     *
     *
     * @param run The run or build or any class that is a Run instance.
     * @param sam Must be an StandardArtifactManager constructed from @run
     * @return Source object
     */
    public static Source newSourceFrom(Run<?,?> run, StandardArtifactManager sam) throws IOException {
        if (sam.root().isDirectory() && sam.root().canRead()) {
            return new Source(run.getParent().getName(), run.getNumber(), sam.root());
        }
        throw new IOException("Error with Run or StandardArtifactManager in Source");
    }


    /**
     * Gets existing sub-files and directories within @srcPath
     *
     * @return All children (files & directories) of the Source or null if not a directory or couldn't get listing
     * @throws IOException
     */
    public VirtualFile[] getChildren() throws IOException {
        if (srcPath.isDirectory() && srcPath.canRead())
            return (VirtualFile[]) getChildren(srcPath).toArray();
        return null;
    }

    protected ArrayList<VirtualFile> getChildren(VirtualFile f) throws IOException {
        ArrayList<VirtualFile> k = new ArrayList<VirtualFile>();
        for(VirtualFile file : f.list()) {
            if (file.isDirectory()) {
                k.addAll(getChildren(file));
            } else if (file.isFile()) {
                k.add(file);
            } else {
                throw new IOException("VirtualFile f in getChildren cannot be read as File or Directory");
            }
        }
        return k;
    }




}
