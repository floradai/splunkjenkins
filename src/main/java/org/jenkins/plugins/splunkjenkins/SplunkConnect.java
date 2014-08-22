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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fdai on 8/14/14.
 */
public class SplunkConnect {

    private String splunkInstance;
    private int port;
    private String user;
    private String pass;
    private Service service;
    private boolean isConnected;

    private boolean doOneShot;
    private boolean doMonitorSetup;
    private String splunkInd;

    public SplunkConnect(String loc, int port, String user, String pass, boolean oneShot, boolean monitorSetUp, String ind) {
        this.splunkInstance = loc;
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.service = null;

        this.doOneShot = oneShot;
        this.doMonitorSetup = monitorSetUp;

        this.splunkInd = ind;
    }

    public boolean setup() {
       return setup(splunkInstance, port, user, pass);
    }

    private boolean setup(String host, int port, String user, String pass) {
        service =  new Service(host, port);
        try {
            service.login(user, pass);
            isConnected = true;
            return true;
        } catch (Exception e) {
            isConnected = false;
            return false;
        }
    }

    public boolean shutdown() {
        try {
            service.logout();
            isConnected = false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }


    public Index getIndex(String indexname) {
        return service.getIndexes().get(indexname);
    }

    /*
        Sets up a basic new monitorInput (host, index, sourcetype) or modifies an existing one
     */
    public boolean monitorInput(String monitorInp, String host, String index, String sourcetype) {
        try {
            InputCollection myInputs = service.getInputs();
            if (!myInputs.containsKey(monitorInp)) {
                myInputs.create(monitorInp, InputKind.Monitor);
                MonitorInput newMonInp = (MonitorInput) myInputs.get(monitorInp);

                newMonInp.setHost(host);
                if (this.getIndex(index) == null)
                    service.getIndexes().create(index);
                newMonInp.setIndex(index);
                newMonInp.setSourcetype(sourcetype);

                newMonInp.update();

                return true;
            } else {
                return false;
                //return modifyMonitofInput(monitorInp, new HashMap<String, String>());
            }
        } catch (Exception e) {
            return false;
        }
    }

    //TODO: Modify via javascript? Set up a form on frontend
    /*public boolean modifyMonitofInput(String monitorInp, Map<String, String> dict) {
        MonitorInput monitorInput = (MonitorInput) service.getInputs().get(monitorInp);
        for (String key : dict.keySet()) {

        }
        return false;
    }*/


    public boolean oneshotUpload(String indexname, String destPath) {
        try {
            if (this.getIndex(indexname) == null) {
                service.getIndexes().create(indexname);
            }
            Index ind = service.getIndexes().get(indexname);
            ind.keySet();

            ind.upload(destPath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public EntityCollection<Application> getApps() {
        return service.getApplications();
    }

    // Getter Methods
    public String getSplunkInstance() {
        return splunkInstance;
    }
    public int getPort() {
        return port;
    }
    public String getUser() {
        return user;
    }
    public String getPass() {
        return pass;
    }
    public Service getService() {
        return service;
    }
    public boolean isDoOneShot() {
        return doOneShot;
    }
    public boolean isDoMonitorSetup() {
        return doMonitorSetup;
    }

    public String getDestInd() {
        return splunkInd;
    }
}

