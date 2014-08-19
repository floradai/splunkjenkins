package org.jenkins.plugins.splunkjenkins;

import com.jcraft.jsch.*;
import com.trilead.ssh2.SFTPException;
import jenkins.util.VirtualFile;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;


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
    public final String getPrvKey() {
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
            //jsch.setKnownHosts("/Users/fdai/.ssh/known_hosts");
            jsch.addIdentity(prvkey, );
            session = jsch.getSession(name, host, 8010);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            for (VirtualFile f : source.getChildren()) {
                if (f.isFile()) {
                    String srcFilePath = f.toURI().getRawPath();
                    String fixed = srcFilePath.substring(srcFilePath.indexOf("/"));
                    String destFile = srcFilePath.substring(srcFilePath.indexOf("jobs/"));
                    checkDirPath(channelSftp, destFile);
                    File file = new File(fixed);
                    String dest = destPath+destFile;
                    inp = new FileInputStream(file);
                    channelSftp.put(inp, dest);
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


    public boolean testUpload(Source source) throws JSchException, IOException, SftpException {
        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        FileInputStream inp = null;

        try {
            JSch jsch = new JSch();
            session.setPortForwardingL(8010, "localhost", 22);
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
            if (session != null)
                session.disconnect();
        }
        return true;
    }

    /** Maybe socket programming
     * CON: requires server side set up
     *
     * public boolean socketUpload(Source source) throws IOException {

        Socket socket = null;
        OutputStream out = null;
        BufferedInputStream inp = null;
        try {
            socket = new Socket(host, port);
            if (socket.isConnected()) {
                for (VirtualFile f : source.getChildren()) {
                    File file = new File(f.toURI().toString());
                    inp = new BufferedInputStream(new FileInputStream(file));
                    byte[] bytes = new byte[(int)file.length()];
                    out = socket.getOutputStream();
                    out.write(bytes);
                    out.flush();
                }
            }
        }
        finally {
            if (socket != null)
                socket.close();
            if (inp != null)
                inp.close();
        }

        return true;
    }*/
}
