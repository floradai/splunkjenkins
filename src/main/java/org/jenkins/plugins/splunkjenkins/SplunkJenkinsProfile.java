package org.jenkins.plugins.splunkjenkins;

import com.jcraft.jsch.*;
import jenkins.util.VirtualFile;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by fdai on 8/12/14.
 */
public class SplunkJenkinsProfile {

    private String name;
    private String host;
    private int port;
    private String destPath;
    private String prvkey;

    private Destination dest;


    @DataBoundConstructor
    public SplunkJenkinsProfile(String name, String host, int port, String destPath, String prvkey) {
        this.dest = new Destination(destPath);
        this.name = name;
        this.host = host;
        this.port = port;
        this.destPath = destPath;
        this.prvkey = prvkey;
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
    public final String getPrvkey() {
        return prvkey;
    }

    /**
     * Uploads files to Machine for Splunk Instance
     *      Files are uploaded to @destPath which Splunk Instance is monitoring
     *      Example - destPath = /Home/jenkins/
     *                  filepath = jobs/jobName/build/buildDate/*
     */
    public boolean upload(Source source) throws JSchException, SftpException, FileNotFoundException, IOException{

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        FileInputStream inp = null;

        try {
            JSch jsch = new JSch();
            jsch.addIdentity(prvkey);
            session = jsch.getSession(name, host, port);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;

            for (VirtualFile f : source.getChildren()) {
                if (f.isFile()) {
                    File file = new File(dest.getPath() + f.getName());
                    String destFile = destPath + f;
                    inp = (FileInputStream) f.open();
                    channelSftp.put(inp, destFile);
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
}
