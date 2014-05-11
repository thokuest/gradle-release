package release

import org.gradle.api.GradleException
import org.gradle.api.Project

import release.helper.CommandLine

/**
 * Helper object extended by plugins.
 * @author evgenyg
 */
class PluginHelper {

	private static final String LINE_SEP = System.getProperty('line.separator')
	private static final String PROMPT = "${LINE_SEP}??>"

	@SuppressWarnings('StatelessClass')
	Project project

    CommandLine cli = new CommandLine()

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

	File findPropertiesFile() {
		File propertiesFile = project.file(project.release.versionPropertyFile)
		if (!propertiesFile.file) {
			if (!isVersionDefined()) {
				project.version = ReleaseOptions.nonInteractive() ? "1.0" : readLine("Version property not set, please set it now:", "1.0")
			}
			boolean createIt = project.hasProperty('version') && cli.promptYesOrNo("[$propertiesFile.canonicalPath] not found, create it with version = ${project.version}")
			if (createIt) {
				propertiesFile.append("version=${project.version}")
			} else {
				log.debug "[$propertiesFile.canonicalPath] was not found, and user opted out of it being created. Throwing exception."
				throw new GradleException("[$propertiesFile.canonicalPath] not found and you opted out of it being created,\n please create it manually and and specify the version property.")
			}
		}
		propertiesFile
	}

    boolean isVersionDefined() {
        project.version && "unspecified" != project.version
    }

	String tagName() {
		String prefix = project.release.tagPrefix ? "${project.release.tagPrefix}-" : (project.release.includeProjectNameInTag ? "${project.rootProject.name}-" : "")
		return "${prefix}${project.version}"
	}

    String branchName() {
        String branchName = ReleaseOptions.branchName();

        if (branchName != null) {
            return branchName;
        }

        String branchVersion = "${project.version}" - "-SNAPSHOT"
        return "${project.rootProject.name}-${branchVersion}-release";
    }

	String findProperty(String key, String defaultVal = "") {
		System.properties[key] ?: project.properties[key] ?: defaultVal
	}
}
