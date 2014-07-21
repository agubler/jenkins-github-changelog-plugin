package com.github.agubler.changelog;

import hudson.model.BuildListener;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.egit.github.core.*;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The change log creation service
 */
public class ChangeLogService {

    /**
     * Commit message for the change log
     */
    private static final String COMMIT_MESSAGE = "Auto-generated Change Log from Build";

    /**
     * Markdown new line
     */
    private static final String MD_NEW_LINE = "\n";

    /**
     * The Change log title
     */
    private static final String MD_CHANGE_LOG_TITLE = "## Change Log" + MD_NEW_LINE;

    /**
     * The markdown format for the release title
     */
    private static final String MD_CHANGE_LOG_RELEASE_FORMAT = "###";

    /**
     * Builder listener
     */
    private BuildListener listener;

    /**
     * Constructor; takes the build listener
     *
     * @param listener the listener
     */
    public ChangeLogService(BuildListener listener) {
        this.listener = listener;
    }

    /**
     * Creates the change log
     *
     * @param gitHubHost the github host
     * @param gitHubAuthToken the github oauth token
     * @param owner the repository owner
     * @param repositoryName the repository
     * @param changeLogBranch the change log branch
     * @param changeLogName the change log filename (includes path)
     * @throws IOException
     */
    public void createChangeLog(String gitHubHost, String gitHubAuthToken, String owner, String repositoryName,
                                String changeLogBranch, String changeLogName, boolean parseJiraReferences, String jiraUrl) throws IOException {

        GitHubClient client;

        if (gitHubHost != null) {
            client = new GitHubClient(gitHubHost);
        } else {
            client = new GitHubClient();
        }

        client.setOAuth2Token(gitHubAuthToken);

        FileContentsService fileContentsService = new FileContentsService(client);
        RepositoryService repositoryService = new RepositoryService(client);

        Repository repository = repositoryService.getRepository(owner, repositoryName);

        logger("[INFO] Repository: " + repositoryName + " found for owner " + owner);

        RepositoryContents existingChangeLog = fileContentsService.getFile(repository, changeLogName, changeLogBranch);

        GitHubFileContentRequest gitHubFileContentRequest = new GitHubFileContentRequest();
        gitHubFileContentRequest.setMessage(COMMIT_MESSAGE);
        gitHubFileContentRequest.setBranch(changeLogBranch);

        if (existingChangeLog != null) {
            logger("[INFO] Existing change log " + changeLogName + " found for update");
            gitHubFileContentRequest.setSha(existingChangeLog.getSha());
        }

        List<RepositoryTag> repositoryTags = repositoryService.getTags(repository);
        Collections.sort(repositoryTags, Collections.reverseOrder(new TagComparator()));
        ListIterator<RepositoryTag> repositoryTagListIterator = repositoryTags.listIterator();

        String changeLogText = MD_CHANGE_LOG_TITLE;
        String gitHubPullRequestUrl = "https://" + gitHubHost + "/" + owner + "/" + repositoryName + "/pull/";

        while (repositoryTagListIterator.hasNext()) {
            if (repositoryTagListIterator.hasPrevious()) {
                RepositoryTag head = repositoryTagListIterator.previous();
                repositoryTagListIterator.remove();
                TypedResource base = repositoryTagListIterator.next().getCommit();
                changeLogText = changeLogText + generateChangeLogForCommits(client, repository, gitHubPullRequestUrl,
                        head.getName(), head.getCommit(), base, parseJiraReferences, jiraUrl);
            } else {
                RepositoryBranch headBranch = findBranch(repositoryService, repository, "integration");
                TypedResource base = repositoryTagListIterator.next().getCommit();
                if (headBranch != null) {
                    TypedResource head = headBranch.getCommit();
                    changeLogText = changeLogText + generateChangeLogForCommits(client, repository, gitHubPullRequestUrl,
                            "upcoming", head, base, parseJiraReferences, jiraUrl);
                }
            }
        }

        //base64 encode and set the content on the request object
        gitHubFileContentRequest.setContent(new String(Base64.encodeBase64(changeLogText.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
        //put the file onto github
        fileContentsService.createOrUpdateFile(repository, changeLogName, gitHubFileContentRequest);
        listener.getLogger().println("[INFO] Change log generation complete - https://" + gitHubHost + "/" + owner + "/" + repositoryName + "/blob/" + changeLogBranch + "/" + changeLogName);
    }

    /**
     * Find specified branch from repositories branch list
     *
     * @param repositoryService The Github repository service
     * @param repository The repository to search
     * @param branchName The branch name to look for
     * @return The RepositoryBranch
     * @throws IOException
     */
    private RepositoryBranch findBranch(RepositoryService repositoryService, IRepositoryIdProvider repository,
                                        String branchName) throws IOException {
        List<RepositoryBranch> branches = repositoryService.getBranches(repository);
        for (RepositoryBranch branch : branches) {
            if (branch.getName().equals(branchName)) {
                return branch;
            }
        }
        return null;
    }

    /**
     * Using the initialised {@link GitHubClient} returns the markdown change log text for with the pull requests between to commits
     * for a specific repository.
     *
     * @param client The initialised {@code GitHubClient}
     * @param repository The repository for comparison
     * @param gitHubPullRequestUrl The pull request url template
     * @param head The head commit
     * @param base The base commit
     * @return The markdown change log text
     * @throws IOException
     */
    private String generateChangeLogForCommits(GitHubClient client, Repository repository, String gitHubPullRequestUrl, String headTitle,
                                               TypedResource head, TypedResource base, boolean findJiraReferences, String jiraUrl) throws IOException {
        CommitService commitService = new CommitService(client);
        RepositoryCommitCompare repositoryCommitCompare = commitService.compare(repository, base.getSha(), head.getSha());

        logger("[INFO] Generating changelog for version " + headTitle);
        Date commitDate = commitService.getCommit(repository, head.getSha()).getCommit().getCommitter().getDate();
        String changeLogText = MD_NEW_LINE + MD_CHANGE_LOG_RELEASE_FORMAT + headTitle + " (" + commitDate.toString() + ")" + MD_NEW_LINE;

        for (RepositoryCommit repositoryCommit : repositoryCommitCompare.getCommits()) {

            //match PR commits only
            Pattern pattern = Pattern.compile("^Merge pull request #.*", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(repositoryCommit.getCommit().getMessage());

            if (matcher.matches()) {
                //split to separate the message for the merge and the title of the PR
                String[] mergeMessageArray = repositoryCommit.getCommit().getMessage().split("\\n\\n");
                String mergeMessage = mergeMessageArray[0];
                //parse the PR number from the merge commit message
                String prNumber = mergeMessage.split("#")[1].split(" ")[0];
                //Append the change log item to the main text
                changeLogText = changeLogText + "- [#" + prNumber + "](" + gitHubPullRequestUrl + prNumber + ") " +
                        parseJiraReference(findJiraReferences, jiraUrl, mergeMessageArray[1]) + "\n";
            }
        }
        return changeLogText;
    }

    /**
     * Parse any jira references added to the PR message
     *
     * @param findJiraReferences indicates if jira references need to be parsed
     * @param jiraUrl The jira url
     * @param message The PR message to parse
     * @return The parsed message
     */
    private String parseJiraReference(boolean findJiraReferences, String jiraUrl, String message) {
        if (findJiraReferences) {
            Pattern jiraPattern = Pattern.compile("(\\w\\w\\w-\\d+)");
            Matcher jiraMatcher = jiraPattern.matcher(message);
            if (jiraMatcher.find()) {
                return message.replaceAll("(\\w\\w\\w-\\d+)", "[" + jiraMatcher.group(0) + "](" + jiraUrl + "/browse/" + jiraMatcher.group(0) + ")");
            }
        }
        return message;
    }

    /**
     * Logs to the listener
     * @param msg The message to log
     */
    private void logger(String msg) {
        if (this.listener != null) {
            listener.getLogger().println(msg);
        } else {
            //Generally for debugging
            System.out.println(msg);
        }
    }
}
