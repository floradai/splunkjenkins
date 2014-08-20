package org.jenkins.plugins.splunkjenkins;

import com.splunk.*;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.kohsuke.stapler.QueryParameter;

import javax.xml.ws.http.HTTPException;

/**
 * Created by fdai on 8/14/14.
 */
public class SplunkConnect {

    private String splunkInstance;
    private int port;
    private String user;
    private String pass;
    private Service service;

    public SplunkConnect(String loc, int port, String user, String pass) {
        this.splunkInstance = loc;
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.service = null;
    }

    public boolean setup() {
       return setup(splunkInstance, Integer.toString(port), user, pass);
    }

    public boolean setup(String host, String port, String user, String pass) {
        service =  new Service(host, Integer.parseInt(port));
        try {
            service.login(user, pass);
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    public boolean index(String indexname) {
        service.getIndexes().get(indexname);
        return true;
    }

    public boolean oneshotUpload(String indexname, String filepath) {
        Index ind = service.getIndexes().get(indexname);
        ind.upload(filepath);
        return false;
    }

    public boolean monitorUpload() {
        return false;
    }

    public boolean newIndex(String index){
        IndexCollection myIndexes = service.getIndexes();
        Index myIndex = myIndexes.get(index);
        if (myIndex == null) {

        }
        return false;
    }

}

