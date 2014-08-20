package org.jenkins.plugins.splunkjenkins;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import jenkins.model.StandardArtifactManager;
import jenkins.util.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by fdai on 8/13/14.
 *
 * Defines an object that determines the artifact's destination on the build machine that Splunk can retreive files from.
 * Automatically obtains srcPath = buildDir/archive
 *
 */
public class Source {
    private String projectName;
    private int buildID;
    private String url;
    private VirtualFile srcPath;

    private boolean checkArchive;
    private boolean checkWorkspace;
    private boolean checkBuild;

    public Source(String projectName, int buildID, VirtualFile path, boolean build, boolean archive, boolean workspace){
        this.projectName = projectName;
        this.buildID = buildID;
        this.url = "/jobs/" + projectName + "/" + buildID + "/";
        this.srcPath = path;

        this.checkArchive = archive;
        this.checkWorkspace = workspace;
        this.checkBuild = build;


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
            return new Source(run.getParent().getName(), run.getNumber(), sam.root(), true, true, true);
        }
        throw new IOException("Error with Run or StandardArtifactManager in Source");
    }


    /**
     * Gets existing sub-files and directories within @srcPath
     *
     * @return All children (files & directories) of the Source or null if not a directory or couldn't get listing
     * @throws IOException
     */
    public ArrayList<VirtualFile> getChildren() throws IOException {
        ArrayList<VirtualFile> allChildren = new ArrayList<VirtualFile>();

        if (this.checkArchive)
            allChildren.addAll(getChildren(cleanPath(DirectoryEnum.ARCHIVE), DirectoryEnum.ARCHIVE));

        if (this.checkWorkspace)
            allChildren.addAll(getChildren(cleanPath(DirectoryEnum.WORKSPACE), DirectoryEnum.WORKSPACE));

        if(this.checkBuild)
            allChildren.addAll(getChildren(cleanPath(DirectoryEnum.BUILD), DirectoryEnum.BUILD));

        return allChildren;
    }

    private ArrayList<VirtualFile> getChildren(VirtualFile f, DirectoryEnum e) throws IOException {
        ArrayList<VirtualFile> k = new ArrayList<VirtualFile>();
        if (f != null) {
            for (VirtualFile file : f.list()) {
                if (file.isDirectory() && e != DirectoryEnum.BUILD && !file.getName().equals("archive")) {
                    k.addAll(getChildren(file, e));
                } else if (file.isFile()) {
                    k.add(file);
                } else {
                    throw new IOException("VirtualFile f in getChildren cannot be read as File or Directory");
                }
            }
        }
        return k;
    }

    public VirtualFile cleanPath(DirectoryEnum dEnum) {
        String path = srcPath.toString();
        File f;
        switch(dEnum) {
            case ARCHIVE:
                path = path.substring(path.indexOf("/"));
                f = new File(path);
                if (f.exists() && f.isDirectory())
                    return VirtualFile.forFile(new File(path));
                break;

            case BUILD:
                path = path.substring(path.indexOf("/"));
                path = path.substring(0, path.lastIndexOf("/")+1);
                 f = new File(path);
                if (f.exists() && f.isDirectory())
                    return VirtualFile.forFile(new File(path));
                break;

            case WORKSPACE:
                path = path.substring(path.indexOf("/"));
                path = path.substring(0, path.indexOf("/builds/"));
                path += "/workspace";
                f = new File(path);
                if (f.exists() && f.isDirectory())
                    return VirtualFile.forFile(new File(path));
                break;
        }
        return null;
    }


    public String getProjectName() { return projectName; }


}
