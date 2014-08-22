package org.jenkins.plugins.splunkjenkins;

import com.jcraft.jsch.*;
import com.trilead.ssh2.SFTPException;
import hudson.util.Secret;
import jenkins.util.VirtualFile;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;


/**
 * Created by fdai on 8/12/14.
 */
public class SplunkJenkinsProfile {

    private String name;
    private String host;
    private int port;
    private String destPath;
    private String prvkey;
    private String prvkeypass;

    private Destination dest;


    public SplunkJenkinsProfile(String name, String host, int port, String destPath, String prvkey, String prvkeypass) {
        this.dest = new Destination(destPath);
        this.name = name;
        this.host = host;
        this.port = port;
        this.destPath = destPath;
        this.prvkey = prvkey;
        this.prvkeypass = prvkeypass;
    }


    public final String getDestPath() {
        return destPath;
    }
    public final String getName() {
        return name;
    }
    public final String getHost() {
        return host;
    }
    public final int getPort() {
        return port;
    }
    public final String getPrvKey() {
        return prvkey;
    }
    public final String getPrvkeypass() {return prvkeypass;}

    /**
     * Uploads files to Machine for Splunk Instance
     *      Files are uploaded to @destPath which Splunk Instance is monitoring
     *      Example - destPath = /Home/jenkins/
     *                  filepath = jobs/jobName/build/buildDate/*
     */
    public boolean upload(Source source, SplunkConnect splunk) throws JSchException, SftpException, FileNotFoundException, IOException{
        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        FileInputStream inp = null;

        try {
            JSch jsch = new JSch();
            jsch.addIdentity(prvkey, prvkeypass);
            session = jsch.getSession(name, host, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            for (VirtualFile f : source.getChildren()) {
                if (f.isFile()) {
                    String srcURI = f.toURI().getRawPath();
                    String srcFile = srcURI.substring(srcURI.indexOf("/"));
                    String destFile = srcFile.substring(srcFile.indexOf("jobs/"));

                    inp = new FileInputStream(srcFile);

                    if (checkDirPath(channelSftp, destFile))
                        channelSftp.put(inp, destPath + "/" + destFile);
                    if (splunk.isDoOneShot()) {
                        if (!splunk.isConnected()) {
                            splunk.setup();
                        }
                        splunk.oneshotUpload(splunk.getDestInd(), destPath + "/" + destFile);

                    }else {throw new Exception("Directory check did not check");}

                } else if (f.isDirectory()) {
                    throw new IOException("Should be no directories: Check Source.java");
                }
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (channel != null)
                channel.disconnect();
            if (channelSftp != null)
                session.disconnect();
            if (inp != null) {
                inp.close();
            }
        }
        return true;
    }




    private boolean checkDirPath(ChannelSftp sftp, String filePath)  {
        String tempDir;
        String[] dirs;
        Vector<ChannelSftp.LsEntry> ls;
        LSSelector lsSelector;
        try {
            sftp.cd(destPath);
            tempDir = sftp.pwd();
            dirs = filePath.split("/");
            for( int i = 0; i < dirs.length - 1; i++) {
                lsSelector = new  LSSelector(dirs[i]);
                sftp.ls(tempDir, lsSelector);
                ls = lsSelector.getVector();
                if (ls.size() == 0) {
                    sftp.mkdir(dirs[i]);
                }
                tempDir += "/" + dirs[i];
                sftp.cd(tempDir);
            }
        } catch (SftpException e) {
            return false;
        }
        return true;
    }


    private class LSSelector implements ChannelSftp.LsEntrySelector {
        private String check;
        private Vector<ChannelSftp.LsEntry> v;
        private LSSelector(String k) {
            this.check = k;
            this.v = new Vector<ChannelSftp.LsEntry>();
        }

        public int select(ChannelSftp.LsEntry lsEntry) {
            if (lsEntry.getFilename().equals(check)) {
                v.add(lsEntry);
            }
            return CONTINUE;
        }
        public Vector<ChannelSftp.LsEntry> getVector() {
            return this.v;
        }
    }
}
