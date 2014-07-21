package com.github.agubler.changelog;

import com.github.agubler.changelog.TagComparator;
import org.eclipse.egit.github.core.RepositoryTag;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class TagComparatorTest {

    private TagComparator tagComparator = new TagComparator();
    private RepositoryTag repositoryTag1 = new RepositoryTag();
    private RepositoryTag repositoryTag2 = new RepositoryTag();

    @Test
    public void testTagComparatorEquals() {
        repositoryTag1.setName("1.2.3.29");
        repositoryTag2.setName("1.2.3.29");
        assertThat(tagComparator.compare(repositoryTag1, repositoryTag2), is(equalTo(0)));
    }

    @Test
    public void testTagComparatorEqualsWithText() {
        repositoryTag1.setName("asset-text-version-0.0.0.0.30.29");
        repositoryTag2.setName("asset-text-version-0.0.0.0.30.29");
        assertThat(tagComparator.compare(repositoryTag1, repositoryTag2), is(equalTo(0)));
    }

    @Test
    public void testTagComparatorEqualsWithDifferingText() {
        repositoryTag1.setName("asset-text-version-0.0.0.0.30.29");
        repositoryTag2.setName("asset-version-0.0.0.0.30.29");
        assertThat(tagComparator.compare(repositoryTag1, repositoryTag2), is(equalTo(0)));
    }

    @Test
    public void testTagComparator() {
        repositoryTag1.setName("asset-version-1.0.1");
        repositoryTag2.setName("asset-version-1.0.2");
        assertThat(tagComparator.compare(repositoryTag1, repositoryTag2), is(equalTo(-1)));
        assertThat(tagComparator.compare(repositoryTag2, repositoryTag1), is(equalTo(1)));
    }
}
