package com.github.agubler.changelog;

import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.springframework.util.StringUtils.hasText;

/**
 * The Builder for the Github Change Log Extension
 */
public class GithubChangeLogBuilder extends Builder {

    /**
     * Default github host
     */
    private static final String DEFAULT_GITHUB_HOST = "github.com";

    /**
     * The owner of the repository for the change log
     */
    private final String githubOwner;

    /**
     * The repository to generate a change log for
     */
    private final String githubRepository;

    /**
     * The branch on the repository to push the change log to
     */
    private final String githubChangeLogBranch;

    /**
     * The change log filename, this needs to include the path and is relative to the root of the repository
     */
    private final String changeLogFilename;

    /**
     * Indicates if the change log should parse jira references
     */
    private final boolean parseJiraReferences;

    /**
     * The jira url to use if parsing url references
     */
    private final String jiraUrl;

    /**
     * The constructor for the builder
     *
     * @param githubOwner The owner of the repo
     * @param githubRepository the name of the repo
     * @param githubChangeLogBranch the branch for the change log
     * @param changeLogFilename the change log filename
     * @param parseJiraReferences indicates if the jira references should be parsed
     */
    @DataBoundConstructor
    public GithubChangeLogBuilder(String githubOwner, String githubRepository, String githubChangeLogBranch,
                                  String changeLogFilename, boolean parseJiraReferences, String jiraUrl) {
        this.githubOwner = githubOwner;
        this.githubRepository = githubRepository;
        this.githubChangeLogBranch = githubChangeLogBranch;
        this.changeLogFilename = changeLogFilename;
        this.parseJiraReferences = parseJiraReferences;
        this.jiraUrl = jiraUrl;
    }

    public String getGithubOwner() {
        return githubOwner;
    }

    public String getGithubRepository() {
        return githubRepository;
    }

    public String getGithubChangeLogBranch() {
        return githubChangeLogBranch;
    }

    public String getChangeLogFilename() {
        return changeLogFilename;
    }

    public boolean getParseJiraReferences() {
        return parseJiraReferences;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException {
        if (this.validate()) {
            String gitHubHost = getDescriptor().getGihubHost();

            if (!hasText(gitHubHost)) {
                gitHubHost = DEFAULT_GITHUB_HOST;
            }

            listener.getLogger().println("[INFO] Starting change log generation");
            ChangeLogService changeLogService = new ChangeLogService(listener);
            changeLogService.createChangeLog(gitHubHost, getDescriptor().getGithubOAuthToken(), this.githubOwner, this.githubRepository,
                    this.githubChangeLogBranch, this.changeLogFilename, this.parseJiraReferences, this.jiraUrl);
            listener.getLogger().println("[INFO] Change log generation complete");
            return true;
        } else {
            listener.getLogger().println("[ERROR] Unable to generate github change log; missing configuration");
            return false;
        }
    }

    /**
     * Validate the github change log config
     * @return boolean that indicates whether the configuration is correct
     */
    private boolean validate() {
        return hasText(getDescriptor().getGithubOAuthToken()) && hasText(this.getGithubOwner()) && hasText(this.getGithubRepository()) &&
               hasText(this.getGithubChangeLogBranch()) && hasText(this.getChangeLogFilename());
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link GithubChangeLogBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * The host for github, defaults to gihub.com.
         */
        private String githubHost;

        /**
         * The OAuth token for access to the github repositories
         */
        private String githubOAuthToken;

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckGithubOAuthToken(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set an oauth token");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckGithubOwner(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set the github repository owner");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckGithubRepository(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set the github repository");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckGithubChangeLogBranch(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set the change log branch");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckChangeLogFilename(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set a change log file path/name");
            }
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Github Change Log";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            githubHost = formData.getString("githubHost");
            githubOAuthToken = formData.getString("githubOAuthToken");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        public String getGihubHost() {
            return githubHost;
        }

        public String getGithubOAuthToken() {
            return githubOAuthToken;
        }
    }
}

