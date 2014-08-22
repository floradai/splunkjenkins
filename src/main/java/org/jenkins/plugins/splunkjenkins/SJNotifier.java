package org.jenkins.plugins.splunkjenkins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import static hudson.Util.fixEmptyAndTrim;
import static hudson.Util.getDigestOf;

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

    private boolean doOneShot;
    private String splunkIndex;

    @DataBoundConstructor
    public SJNotifier(String name, String host, int port, String dest, String prvkey, String pkeypass,
                      boolean canSplunk, boolean optBuildSel, boolean optArchiveSel, boolean optWorkspaceSel,
                      boolean optSplunk, String sHost, int sPort, String sUser, String sPass,
                      boolean optOneShot, String splunkInd) {
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

        this.doOneShot = optOneShot;
        this.splunkIndex = splunkInd;
    }


    //Getter methods
    public String getSJname() { return this.sjname;}
    public String getSJhost() {return this.sjhost;}
    public int getSJport() {return this.sjport; }
    public String getSJdest() {return this.sjdest;}
    public String getSjprvkey() {return this.sjprvkey;}
    public String getSjprvkeypass() {return this.sjprvkeypass;}
    public String getSplunkPass() { return sPass; }
    public String getSplunkUser() { return sUser; }
    public int getSplunkPort() { return sPort; }
    public String getSplunkHost() { return sHost; }
    public boolean getOneShot() {return doOneShot;}

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        //Update this SJNotifier with latest DESCRIPTOR variable
        this.sjname = getDescriptor().getName();
        this.sjhost = getDescriptor().getHost();
        this.sjport = getDescriptor().getPort();
        this.sjdest = getDescriptor().getDest();
        this.sjprvkey = getDescriptor().getPrvkey();
        this.sjprvkeypass = getDescriptor().getPkeypass();

        this.sPass = getDescriptor().getSplunkpass();
        this.sUser = getDescriptor().getSplunkuser();
        this.sHost = getDescriptor().getSplunkhost();
        this.sPort = getDescriptor().getSplunkport();

        StandardArtifactManager sam = new StandardArtifactManager(build);
        Source s = new Source(build.getProject().getName(), build.getNumber(), sam.root(), this.build, this.archive, this.workspace);
        SplunkJenkinsProfile spj = new SplunkJenkinsProfile(getSJname(), getSJhost(), getSJport(), getSJdest(), getSjprvkey(), getSjprvkeypass());
        SplunkConnect splunkInst= new SplunkConnect(getSplunkHost(), getSplunkPort(), getSplunkUser(), getSplunkPass(), this.doOneShot, false, this.splunkIndex);
        try {
            spj.upload(s, splunkInst); //TODO: add boolean to check if oneShot upload or apply monitor?
        } catch (Exception e) {
            throw new IOException("problem with upload call in SJNotifier", e);
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
            boolean oneShot = formData.getBoolean("optOneShot");
            String splunkIndex = formData.getString("splunkIndex");
            SJNotifier sjn = new SJNotifier(this.name, this.host, this.port, this.dest, this.prvkey, this.pkeypass,
                    formData.getBoolean("canSplunk"), optBuildSel, optArchiveSel, optWorkspaceSel,
                    this.optSplunk, this.splunkhost, this.splunkport, this.splunkuser, this.splunkpass,
                    oneShot, splunkIndex);
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
                JSONObject info = json.getJSONObject("optSplunk");
                this.optSplunk = true;
                this.splunkhost = info.getString("splunkhost");
                this.splunkport = info.getInt("splunkport");
                this.splunkuser = info.getString("splunkuser");
                this.splunkpass = info.getString("splunkpass");
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
