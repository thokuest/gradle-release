package release

import java.util.regex.Matcher

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class ReleaseTask extends DefaultTask {
    def scm
    ReleasePlugin plugin;

    @TaskAction
    void release() {
        tryReleaseProject();
    }

    void tryReleaseProject() {
        plugin.writeVersionProperty();

        preTagCommit();
        commitTag();
        updateVersion();
        commitNewVersion();
    }

    void preTagCommit() {
        if (project.properties['usesSnapshot'] || project.properties['versionModified']) {
            // should only be committed if the project was using a snapshot version.
            def message = plugin.interpolate(project.release.preTagCommitMessage)

            scm().commit("$message '${plugin.tagName()}'.")
        }
    }

    void commitTag() {
        if (project.release.skipTag) {
            return;
        }

        def message = plugin.interpolate(project.release.tagCommitMessage)

        scm().createReleaseTag("$message '${plugin.tagName()}'.")
    }

    void updateVersion() {
        def version = project.version.toString()
        Map<String, Closure> patterns = project.release.versionPatterns

        for (entry in patterns) {

            String pattern = entry.key

            Closure handler = entry.value
            Matcher matcher = version =~ pattern

            if (matcher.find()) {
                String nextVersion = handler(matcher, project)
                if (project.properties['usesSnapshot']) {
                    nextVersion += '-SNAPSHOT'
                }

                nextVersion = plugin.getNextVersion(nextVersion);

                project.ext.set("release.oldVersion", project.version)
                project.ext.set("release.newVersion", nextVersion)

                plugin.updateVersionProperty(nextVersion)
                plugin.writeVersionProperty()
                return
            }
        }

        throw new GradleException("Failed to increase version [$version] - unknown pattern")
    }

    def commitNewVersion() {
        def message = plugin.getCommitNewVersionMessage();

        scm().commit("$message '${plugin.tagName()}'.")
    }


}
