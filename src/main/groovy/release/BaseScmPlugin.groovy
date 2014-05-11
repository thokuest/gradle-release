package release

import org.gradle.api.GradleException
import org.gradle.api.Project

import release.helper.Validate

/**
 * Base class for all SCM-specific plugins
 * @author evgenyg
 */
abstract class BaseScmPlugin extends PluginHelper {

	private final String pluginName = this.class.simpleName

    public BaseScmPlugin(Project project) {
        this.project = project;
    }

	void initialize() {
    }

	abstract void checkCommitNeeded()

	abstract void checkUpdateNeeded()

	abstract void createReleaseTag(String message)

    void createReleaseBranch(String message) {
        throw new GradleException("branching is currently not supported by this SCM plugin");
    }

	abstract void commit(String message)

	abstract void revert()

    void assertNoModifications(def condition, def message) {
        if (condition) {
            Validate.warnOrFail(project.release.failOnCommitNeeded, message)
        }
    }

    void assertNoUnversionedFiles(def condition, def message) {
        if (condition) {
            Validate.warnOrFail(project.release.failOnUnversionedFiles, message)
        }
    }

    void assertNoPendingCommits(def condition, def message) {
        if (condition) {
            Validate.warnOrFail(project.release.failOnPublishNeeded, message);
        }
    }

    void assertUpToDate(def condition, def message) {
        if (condition) {
            Validate.warnOrFail(project.release.failOnUpdateNeeded, message);
        }
    }
}
