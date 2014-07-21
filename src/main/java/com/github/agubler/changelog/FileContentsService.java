package com.github.agubler.changelog;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.ContentsService;

import java.io.IOException;
import java.util.Collections;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_CONTENTS;

/**
 * Extends the {@link ContentsService} to provide support for create/update/get files from Github
 */
public class FileContentsService extends ContentsService {

    /**
     * Default constructor
     */
    public FileContentsService() {
        super();
    }

    /**
     * Constructor with the initialised github client
     * @param client The githubClient
     */
    public FileContentsService(final GitHubClient client) {
        super(client);
    }

    /**
     * Creates or updates a file in github, updates if a {@link GitHubFileContentRequest#sha} is present
     * @param repository The repository to push the file to
     * @param filePath the path of the file (includes name)
     * @param gitHubFileContent The base64 encoded content of the file
     * @return The Object created
     * @throws IOException
     */
    public Object createOrUpdateFile(IRepositoryIdProvider repository, String filePath, GitHubFileContentRequest gitHubFileContent) throws IOException {
        String id = getId(repository);

        return client.put(SEGMENT_REPOS + '/' + id + SEGMENT_CONTENTS + "/" + filePath, gitHubFileContent, Object.class);
    }

    /**
     * Returns a file from github, null if the file cannot be found
     *
     * @param repository The repository to push the file to
     * @param filePath the path of the file (includes name)
     * @param ref the branch to find the content
     * @return The file contents object
     * @throws IOException
     */
    public RepositoryContents getFile(IRepositoryIdProvider repository, String filePath, String ref) throws IOException {
        String id = getId(repository);

        GitHubRequest request = createRequest();
        request.setType(RepositoryContents.class);
        request.setUri(SEGMENT_REPOS + '/' + id + SEGMENT_CONTENTS + "/" + filePath);
        if (ref != null && ref.length() > 0) {
            request.setParams(Collections.singletonMap("ref", ref));
        }

        RepositoryContents repositoryContents = null;
        try {
            repositoryContents = (RepositoryContents) client.get(request).getBody();
        } catch (RequestException ignored) { }

        return repositoryContents;
    }
}
