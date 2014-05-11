package release

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskAction

import release.helper.Validate

class VerifyReleaseTask extends DefaultTask {
    def scm;

    ReleasePlugin plugin;

    @TaskAction
    void verify() {
        verfiyUnchanged();
        checkForSnapshotDependencies();
    }

    void verfiyUnchanged() {
        scm().checkCommitNeeded();
        scm().checkUpdateNeeded();
    }

    void checkForSnapshotDependencies() {
        unSnapshotVersion()

        def matcher = { Dependency d -> d.version?.contains('SNAPSHOT') }
        def collector = { Dependency d -> "${d.group ?: ''}:${d.name}:${d.version ?: ''}" }

        def message = ""

        project.allprojects.each { project ->
            def snapshotDependencies = [] as Set

            project.configurations.each { cfg ->
                snapshotDependencies += cfg.dependencies?.matching(matcher)?.collect(collector)
            }
            if (snapshotDependencies.size() > 0) {
                message += "\n\t${project.name}: ${snapshotDependencies}"
            }
        }

        if (message) {
            message = "Snapshot dependencies detected: ${message}"

            Validate.warnOrFail(project.release.failOnSnapshotDependencies, message)
        }
    }

    void unSnapshotVersion() {
        def version = project.version.toString()

        if (version.contains('-SNAPSHOT')) {
            project.ext.set('usesSnapshot', true)
            project.ext.set('snapshotVersion', version)
            version -= '-SNAPSHOT'
            updateVersionProperty(version)
        } else {
            project.ext.set('usesSnapshot', false)
        }
    }

    /**
     * Updates properties file (<code>gradle.properties</code> by default) with new version specified.
     * If configured in plugin convention then updates other properties in file additionally to <code>version</code> property
     *
     * @param newVersion new version to store in the file
     */
    void updateVersionProperty(String newVersion) {
        def oldVersion = "${project.version}"

        if (oldVersion != newVersion) {
            project.version = newVersion
            project.ext.set('versionModified', true)
            project.subprojects?.each { Project subProject ->
                subProject.version = newVersion
            }
        }
    }
}
