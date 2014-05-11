package release

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskState

/**
 * @author elberry
 * @author evgenyg
 * @author thokuest
 */
class ReleasePlugin extends PluginHelper implements Plugin<Project> {
	static final String RELEASE_GROUP = "Release"

	private Class<BaseScmPlugin> scmPluginClass;
    private BaseScmPlugin scmPlugin

	void apply(Project project) {
		this.project = project

		project.extensions.create("release", ReleasePluginConvention)

        def scmClosure = {
            if (this.scmPlugin == null) {
                setup();
            }

            return scmPlugin
        }

        configureVerificationTask(scmClosure)
        configureReleaseTask(scmClosure)
        configureBranchReleaseTask(scmClosure)
	}

    void applyErrorHandler() {
        project.gradle.taskGraph.afterTask { Task task, TaskState state ->
            if (state.failure && (task.name == "release" || task.name == "branch")) {
                if (project.release.revertOnFail && project.file(project.release.versionPropertyFile)?.exists()) {
                    log.error("Release process failed, reverting back any changes made by Release Plugin.")
                    this.scmPlugin.revert()
                } else {
                    log.error("Release process failed, please remember to revert any uncommitted changes made by the Release Plugin.")
                }
            }
        }
    }

    void configureVerificationTask(def scmClosure) {
        project.task('verifyRelease',
                description: 'Verifies the project',
                group: RELEASE_GROUP,
                type: VerifyReleaseTask) {
            scm = scmClosure
            plugin = this
        }

        project.tasks.getByName('build').mustRunAfter 'verifyRelease'
    }

    void configureReleaseTask(def scmClosure) {
        project.task('release',
                description: 'Releases the project',
                group: RELEASE_GROUP,
                dependsOn: [':verifyRelease', ':build'],
                type: ReleaseTask) {
            scm = scmClosure
            plugin = this
        }
    }

    void configureBranchReleaseTask(def scmClosure) {
        project.task('branchRelease',
                description: 'Branches the project',
                group: RELEASE_GROUP,
                dependsOn: [':verifyRelease'],
                type: BranchReleaseTask) {
            scm = scmClosure
            plugin = this
        }
    }

    void writeVersionProperty() {
        if (checkVersionModified()) {
            def propFile = findPropertiesFile()

            try {
                project.ant.propertyfile(file: propFile, comment: '') {
                    entry(key: 'version', value: project.version)
                }
            } catch (Exception e) {
                throw new GradleException("Unable to update version property. Please check file permissions, and ensure property is in \"${prop}=${newVersion}\" format.", e)
            }
        }
    }

    boolean checkVersionModified() {
        project.ext.hasProperty('versionModified') && project.ext.'versionModified'
    }

    String getCommitNewVersionMessage() {
        return interpolate(project.release.newVersionCommitMessage)
    }

    String interpolate(String message) {
        if (project.release.preCommitText) {
            message = "${project.release.preCommitText} $message"
        }

        return message
    }

    void setup() {
        selectScmPlugin()

        this.scmPlugin = scmPluginClass.newInstance([project: this.project])

        initializeScmPlugin();
    }

    void initializeScmPlugin() {
        checkPropertiesFile()
        scmPlugin.initialize()
    }

	void confirmReleaseVersion() {
		def version = getReleaseVersion();
		updateVersionProperty(version)
	}

    String getReleaseVersion(String candidateVersion = "${project.version}") {
        String releaseVersion = ReleaseOptions.releaseVersion()

        if (ReleaseOptions.nonInteractive()) {
            return releaseVersion ?: candidateVersion;
        }

        return cli.readLine("This release version:", releaseVersion ?: candidateVersion);
    }

    String getNextVersion(String candidateVersion) {
        String nextVersion = ReleaseOptions.developmentVersion()

        if (ReleaseOptions.nonInteractive()) {
            return nextVersion ?: candidateVersion;
        }

        return cli.readLine("Enter the next version (current one released as [${project.version}]):", nextVersion ?: candidateVersion);
    }


	def checkPropertiesFile() {
		File propertiesFile = findPropertiesFile()

		Properties properties = new Properties()
		propertiesFile.withReader { properties.load(it) }

		assert properties.version, "[$propertiesFile.canonicalPath] contains no 'version' property"
		assert project.release.versionPatterns.keySet().any { (properties.version =~ it).find() },               \
                             "[$propertiesFile.canonicalPath] version [$properties.version] doesn't match any of known version patterns: " +
				project.release.versionPatterns.keySet()

        // set the project version from the properties file if it was not otherwise specified
        if ( !isVersionDefined() ) {
            project.version = properties.version
        }
	}

	/**
	 * Looks for special directories in the project folder, then applies the correct SCM Release Plugin for the SCM type.
	 * @param project
	 */
	private void selectScmPlugin() {

		def projectPath = project.rootProject.projectDir.canonicalFile

		Class c = findScmType(projectPath)

		if (!c) {
			throw new GradleException(
					'Unsupported SCM system, no .svn, .bzr, .git, or .hg found in ' +
							"[${ projectPath }] or its parent directories.")
		}

		assert BaseScmPlugin.isAssignableFrom(c)

        this.scmPluginClass = c;
	}

	/**
	 * Recursively look for the type of the SCM we are dealing with, if no match is found look in parent directory
	 * @param directory the directory to start from
	 */
	private Class findScmType(File directory) {

		Class c = (Class) directory.list().with {
			delegate.grep('.svn') ? SvnReleasePlugin :
				delegate.grep('.bzr') ? BzrReleasePlugin :
					delegate.grep('.git') ? GitReleasePlugin :
						delegate.grep('.hg') ? HgReleasePlugin :
							null
		}

		if (!c && directory.parentFile) {
			c = findScmType(directory.parentFile)
		}

		c
	}

}