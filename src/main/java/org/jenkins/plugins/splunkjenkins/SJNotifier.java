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
import hudson.util.Secret;
import static hudson.Util.fixEmptyAndTrim;
import jenkins.model.StandardArtifactManager;
import jenkins.util.VirtualFile;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import sun.text.resources.FormatData_es_EC;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
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
    private String sjprvkeypass;

    private boolean canSplunk;
    private boolean archive;
    private boolean workspace;
    private boolean build;

    private String sHost;
    private int sPort;
    private String sUser;
    private String sPass;


    @DataBoundConstructor
    public SJNotifier(String name, String host, int port, String dest, String prvkey, String pkeypass,
                      boolean canSplunk, boolean optBuildSel, boolean optArchiveSel, boolean optWorkspaceSel,
                      boolean optSplunk, String sHost, int sPort, String sUser, String sPass ) {
        this.sjname = name;
        this.sjhost = host;
        this.sjport = port;
        this.sjdest = dest;
        this.sjprvkey = prvkey;
        this.sjprvkeypass = pkeypass;

        this.archive = optArchiveSel;
        this.workspace = optWorkspaceSel;
        this.build = optBuildSel;

        this.sHost = optSplunk ? sHost : null;
        this.sPort = optSplunk ? sPort : -1;
        this.sUser = optSplunk ? sUser : null;
        this.sPass = optSplunk ? sPass : null;
    }

    //Getter methods
    public String getSJname() { return this.sjname;}
    public String getSJhost() {return this.sjhost;}
    public int getSJport() {return this.sjport; }
    public String getSJdest() {return this.sjdest;}
    public String getSjprvkey() {return this.sjprvkey;}
    public String getSjprvkeypass() {return this.sjprvkeypass;}
    public String getsPass() { return sPass; }
    public String getsUser() { return sUser; }
    public int getsPort() { return sPort; }
    public String getsHost() { return sHost; }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        //Update this SJNotifier with latest DESCRIPTOR variable
        this.sjname = getDescriptor().getName();
        this.sjhost = getDescriptor().getHost();
        this.sjport = getDescriptor().getPort();
        this.sjdest = getDescriptor().getDest();
        this.sjprvkey = getDescriptor().getPrvkey();
        this.sjprvkeypass = getDescriptor().getPkeypass();


        StandardArtifactManager sam = new StandardArtifactManager(build);
        Source s = new Source(build.getProject().getName(), build.getNumber(), sam.root(), this.build, this.archive, this.workspace);
        SplunkJenkinsProfile spj = new SplunkJenkinsProfile(getSJname(), getSJhost(), getSJport(), getSJdest(), getSjprvkey(), getSjprvkeypass());
        try {
            spj.upload(s);
        } catch (Exception e) {
            throw new IOException("problem with upload call in SJNotifier", e);
        }

        if (toSplunk()) {
            SplunkConnect splunk = new SplunkConnect(getsHost(), getsPort(), getsUser(), getsPass());
            boolean isConnected = splunk.setup();
            if(isConnected) {

            } else {
                LOGGER.log(Level.SEVERE, "Splunk cannot connect - End of Performance");
            }


        }
        return true;

    }


    public boolean toSplunk() {return this.sHost == null ? false : true;}

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }



    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> implements ModelObject {
        private String name;
        private String host;
        private int port;
        private String dest;
        private String prvkey;
        private String pkeypass;

        private String splunkhost;
        private int splunkport;
        private String splunkuser;
        private String splunkpass;

        private boolean optArchiveSel, optBuildSel, optWorkspaceSel, optSplunk;

        public DescriptorImpl() {
            super(SJNotifier.class);
            load();
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            optArchiveSel = formData.getBoolean("optArchiveSel");
            optBuildSel = formData.getBoolean("optBuildSel");
            optWorkspaceSel = formData.getBoolean("optWorkspaceSel");
            SJNotifier sjn = new SJNotifier(this.name, this.host, this.port, this.dest, this.prvkey, this.pkeypass,
                    formData.getBoolean("canSplunk"), optBuildSel, optArchiveSel, optWorkspaceSel,
                    this.optSplunk, this.splunkhost, this.splunkport, this.splunkuser, this.splunkpass);
            return sjn;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            this.name = json.getString("name");
            this.host = json.getString("host");
            this.port = json.getInt("port");
            this.dest = json.getString("dest");
            this.prvkey = json.getString("prvkey");
            this.pkeypass = json.getString("pkeypass");

            try {
                this.optSplunk = json.getBoolean("optSplunk");
                this.splunkhost = optSplunk ? json.getString("splunkhost") : null;
                this.splunkport = optSplunk ? json.getInt("splunkport") : null;
                this.splunkuser = optSplunk ? json.getString("splunkuser") : null;
                this.splunkpass = optSplunk ? json.getString("splunkpass") : null;
            } catch (JSONException e) {
                LOGGER.log(Level.INFO, "NO spelunking");
                this.optSplunk = false;
                this.splunkhost = null;
                this.splunkport = -1;
                this.splunkuser = null;
                this.splunkpass = null;
            }
            save();
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Splunker Jenkins";
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
        @Exported
        public String getPkeypass() {return pkeypass; }
        @Exported
        public boolean getOptArchiveSel() { return optArchiveSel; }
        @Exported
        public boolean getOptBuildSel() {return optBuildSel;}
        @Exported
        public boolean getOptWorkspaceSel() {return optWorkspaceSel;}
        @Exported
        public String getSplunkhost() { return splunkhost; }
        @Exported
        public int getSplunkport() { return splunkport; }
        @Exported
        public String getSplunkuser() { return splunkuser; }
        @Exported
        public String getSplunkpass() { return splunkpass; }


    }
}
