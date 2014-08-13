package org.jenkins.plugins.splunkjenkins;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.StandardArtifactManager;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.text.Normalizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by fdai on 8/12/14.
 */
public class SJNotifier extends Notifier {

    private static final Logger LOGGER = Logger.getLogger(SJNotifier.class.getName());
    private SplunkJenkinsProfile spj;

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public SJNotifier() {
        super();
    }


    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        try {
            Source source = Source.newSourceFrom(build, new StandardArtifactManager(build));
            SplunkJenkinsProfile sjp = DESCRIPTOR.getSPJ();
            return sjp.upload(source);
        } catch (SftpException e) {
            LOGGER.log(Level.SEVERE, "SFTPException in SplunkJenkinsProfile.upload");
        } catch (JSchException e) {
            LOGGER.log(Level.SEVERE, "JSCHException in SplunkJenkinsProfile.upload");
        }
        return false;

    }

    public BuildStepMonitor getRequiredMonitorService() {

        return null;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }





    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private SplunkJenkinsProfile spj;



        public DescriptorImpl() {
            this(SJNotifier.class);
        }

        public DescriptorImpl(Class<? extends Publisher> c) {
            super(c);
            load();
        }


        public SplunkJenkinsProfile getSPJ() {
            return spj;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException{
            setSPJ(req.bindJSON(SplunkJenkinsProfile.class, json));
            save();
            return true;
        }

        @Override
        public SJNotifier newInstance(StaplerRequest req, JSONObject formData)  throws FormException{
            SJNotifier sjn = new SJNotifier();
            sjn.DESCRIPTOR.configure(req, formData);
            return sjn;
        }


        public void setSPJ(SplunkJenkinsProfile spj) {
            this.spj = spj;
        }

        @Override
        public String getDisplayName() {
            return "Uploads artifacts to a machine Splunk can retrieve data from";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /*
        public FormValidation configCheck(StaplerRequest req, StaplerResponse rsp) {
            if (Integer.getInteger(req.getParameter("port")) == null)
                return FormValidation.error("Must have port");

            spj = new SplunkJenkinsProfile(req.getParameter("name"), req.getParameter("host"),
                    Integer.getInteger(req.getParameter("port")),
                    req.getParameter("destPath"),
                    req.getParameter("prvkey"));

            return FormValidation.ok();
        }*/
    }
}
