package org.jenkins.plugins.splunkjenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.StandardArtifactManager;
import jenkins.util.VirtualFile;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import sun.text.resources.FormatData_es_EC;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Logger;

/**
 * Created by fdai on 8/13/14.
 */
public final class SJNotifier extends Notifier implements Describable<Publisher> {
    private static final Logger LOGGER = Logger.getLogger(SJNotifier.class.getName());
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    private String sjname;
    private String sjhost;
    private int sjport;
    private String sjdest;
    private String sjprvkey;

    private boolean canSplunk;

    @DataBoundConstructor
    public SJNotifier(String name, String host, int port, String dest, String prvkey, boolean canSplunk) {
        this.sjname = name;
        this.sjhost = host;
        this.sjport = port;
        this.sjdest = dest;
        this.sjprvkey = prvkey;

        this.canSplunk = canSplunk;
    }

    public String getSJname() { return this.sjname;}
    public String getSJhost() {return this.sjhost;}
    public int getSJport() {return this.sjport; }
    public String getSJdest() {return this.sjdest;}
    public String getSjprvkey() {return this.sjprvkey;}
    public boolean getCanSplunk() {return this.canSplunk;}

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        //Check to update this instance
        this.sjname = getDescriptor().getName();
        this.sjhost = getDescriptor().getHost();
        this.sjport = getDescriptor().getPort();
        this.sjdest = getDescriptor().getDest();
        this.sjprvkey = getDescriptor().getPrvkey();
        if (getCanSplunk()) {
            StandardArtifactManager sam = new StandardArtifactManager(build);
            Source s = new Source(build.getProject().getName(), build.getNumber(), sam.root());
            SplunkJenkinsProfile spj = new SplunkJenkinsProfile(getSJname(), getSJhost(), getSJport(), getSJdest(), getSjprvkey());
            try {
                spj.upload(s);
            } catch (Exception e) {
                throw new IOException("problem with upload call in SJNotifier", e);
            }
            return true;
        }
        return false;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }


    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> implements ModelObject {
        private String name;
        private String host;
        private int port;
        private String dest;
        private String prvkey;

        public DescriptorImpl() {
            super(SJNotifier.class);
            load();
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            SJNotifier sjn = new SJNotifier(this.name, this.host, this.port, this.dest, this.prvkey, formData.getBoolean("canSplunk"));
            return sjn;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            this.name = json.getString("name");
            this.host = json.getString("host");
            this.port = json.getInt("port");
            this.dest = json.getString("dest");
            this.prvkey = json.getString("prvkey");
            save();
            return true;
        }

        @Override
        public String getDisplayName() {
            return "SKDJFLSKDFJ";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }
        @Exported
        public String getName() {return name;}
        @Exported
        public String getHost() {return host;}
        @Exported
        public int getPort() {return port;}
        @Exported
        public String getDest() {return dest;}
        @Exported
        public String getPrvkey() {return prvkey;}

    }
}
