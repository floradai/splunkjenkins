package org.jenkins.plugins.splunkjenkins;

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
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import sun.text.resources.FormatData_es_EC;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by fdai on 8/13/14.
 */
public class SJNotifier extends Notifier {
    private static final Logger LOGGER = Logger.getLogger(SJNotifier.class.getName());

    private String sjname;
    private String sjhost;
    private int sjport;
    private String sjdestPath;
    private String sjprvkey;


    @DataBoundConstructor
    public SJNotifier(String n) {
        super();
        this.sjname = n;
    }


    public String getSJname() {
        return sjname;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public BuildStepDescriptor getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private String token;

        public DescriptorImpl() {
            load();
        }

        public String getToken() {
            return token;
        }

        @Override
        public SJNotifier newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            if (token == null)
                token = req.getParameter("sjnameToken");
            return new SJNotifier(token);
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            token = req.getParameter("sjnameToken");
            try {
                new SJNotifier(token);
            } catch (Exception e) {
                throw new FormException("Failed to init SJNotofier", e, "");
            }
            save();
            return true;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Plugin: SplunkJenkins";
        }
    }


}


