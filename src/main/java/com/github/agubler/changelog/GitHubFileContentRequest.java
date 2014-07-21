package com.github.agubler.changelog;

/**
 * The DTO to represent a file action (create/update/delete) using Github API
 */
public class GitHubFileContentRequest {

    /**
     * The commit message
     */
    private String message;

    /**
     * The base64 encoded file content
     */
    private String content;

    /**
     * The branch for the file to be created for, defaults to the repos default branch
     */
    private String branch;

    /**
     * The sha of the existing file, used when updating / deleting a file
     */
    private String sha;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }
}
