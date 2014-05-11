package release

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class BranchReleaseTask extends DefaultTask {
    def scm;

    ReleasePlugin plugin

    @TaskAction
    void branch() {
        createReleaseBranch();
    }

    void createReleaseBranch() {
        String branchVersion = ReleaseOptions.branchVersion() ?: "${project.version}" - "-SNAPSHOT"
        String oldVersion = "${project.version}"

        if (branchVersion != oldVersion) {
            project.version = branchVersion
            project.ext.set('versionModified', true)

            plugin.writeVersionProperty()
            scm().commit(plugin.getCommitNewVersionMessage())
        }

        String branchCommitMessage = project.release.branchCommitMessage

        String message = plugin.interpolate("$branchCommitMessage '${plugin.branchName()}'.")

        scm().createReleaseBranch(message)

        if (branchVersion != oldVersion) {
            project.version = oldVersion

            plugin.writeVersionProperty()
            scm().commit(plugin.getCommitNewVersionMessage())
        }
    }
}
