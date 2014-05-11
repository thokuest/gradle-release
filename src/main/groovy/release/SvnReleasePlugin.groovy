package release

import java.util.regex.Matcher

import org.gradle.api.Project

import release.helper.Validate

/**
 * A command-line style SVN client. Requires user has SVN installed locally.
 * @author elberry
 * @author evgenyg
 * @author thokuest
 */
class SvnReleasePlugin extends BaseScmPlugin {

	private static final String ERROR = 'Commit failed'
	private static final def urlPattern = ~/URL:\s(.*?)(\/(trunk|branches|tags).*?)$/
	private static final def revPattern = ~/Revision:\s(.*?)$/
    private static final def committedRevPattern = ~/Committed revision\s(.*[0-9])\.$/

    private List<String> svn

    private String svnUrl
    private String svnRev
    private String svnRoot

    SvnReleasePlugin(Project project) {
        super(project)
    }

	void initialize() {
        svn = ['svn'] + usernameAndPassword()

		findSvnUrl()
	}

	@Override
	void checkCommitNeeded() {
		String out = cli.launcher {
            commands = svn + ['status']
        }.out()

		def changes = []
		def unknown = []

		out.eachLine { line ->
			switch (line?.trim()?.charAt(0)) {
				case '?':
					unknown << line
					break
				default:
					changes << line
					break
			}
		}

        assertNoUnversionedFiles(changes, "You have ${changes.size()} un-commited changes.")
        assertNoUnversionedFiles(unknown, "You have ${unknown.size()} un-versioned files.")
	}


	@Override
	void checkUpdateNeeded() {
		String svnRemoteRev = ""

		String out = cli.launcher {
            commands = svn + ['status', '-q', '-u']
        }.out()

		def missing = []
		out.eachLine { line ->
			switch (line?.trim()?.charAt(0)) {
				case '*':
					missing << line
					break
			}
		}

        assertUpToDate(missing, "You are missing ${missing.size()} changes.")

		out = cli.launcher {
            commands = svn + ['info', svnUrl]
            environment = [LC_COLLATE: "C", LC_CTYPE: "en_US.UTF-8"]
        }.out()

		out.eachLine { line ->
			Matcher matcher = line =~ revPattern
			if (matcher.matches()) {
				svnRemoteRev = matcher.group(1)
			}
		}

        assertUpToDate(svnRev != svnRemoteRev, "Local revision (${svnRev}) does not match remote (${svnRemoteRev}), local revision is used in tag creation.")
	}


	@Override
	void createReleaseTag(String message = "") {
		copy("tags/${tagName()}", message)
	}

    void copy(String to, String message) {
        String out = cli.launcher {
            commands = svn + ['cp', "${svnUrl}@${svnRev}", "${svnRoot}/${to}", '-m', message ?: "Created by Release Plugin: ${to}"]
        }.out()

        updateRevisionProperty(out)
    }

    @Override
    void createReleaseBranch(String message = "") {
        copy("branches/${branchName()}", message)
    }

	@Override
	void commit(String message) {
		String out = cli.launcher {
            commands = svn + ['ci', '-m', message]
            errorMessage = 'Error committing new version'
            errorPattern = ERROR
		}.out()

        updateRevisionProperty(out)
	}

    void updateRevisionProperty(String cliOut) {
        cliOut.eachLine { line ->
            Matcher matcher = line =~ committedRevPattern

            if (matcher.matches()) {
                svnRev = matcher.group(1)
            }
        }
    }

	@Override
	void revert() {
		cli.launcher {
            commands = svn + ['revert', findPropertiesFile().name]
            errorMessage = 'Error reverting changes made by the release plugin.'
            errorPattern = ERROR
		}.execute()
	}

    List<String> usernameAndPassword() {
        String username = ReleaseOptions.username();
        String password = ReleaseOptions.password();

        return username != null ? ['--no-auth-cache', '--username', username, '--password', password] : [];
    }

	private void findSvnUrl() {
		String out = cli.launcher {
            commands = svn + ['info']
            environment = [LC_COLLATE: "C", LC_CTYPE: "en_US.UTF-8"]
		}.out()

		out.eachLine { line ->
			Matcher matcher = line =~ urlPattern
			if (matcher.matches()) {
				svnRoot = matcher.group(1)
				String svnProject = matcher.group(2)

				svnUrl = "$svnRoot$svnProject"
			}
			matcher = line =~ revPattern
			if (matcher.matches()) {
				svnRev = matcher.group(1)
			}
		}

        Validate.assertTrue(svnUrl != null,
            'Could not determine root SVN url.')
	}
}