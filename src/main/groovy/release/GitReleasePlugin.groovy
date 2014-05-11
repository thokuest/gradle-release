package release

import org.gradle.api.Project

import release.helper.Validate

/**
 * @author elberry
 * @author evgenyg
 * @author szpak
 * Created: Tue Aug 09 23:24:40 PDT 2011
 */
class GitReleasePlugin extends BaseScmPlugin {

	private static final String LINE = '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'

	private static final String UNCOMMITTED = 'uncommitted'
	private static final String UNVERSIONED = 'unversioned'
	private static final String AHEAD = 'ahead'
	private static final String BEHIND = 'behind'

    GitReleasePlugin(Project project) {
        super(project)
    }

	@Override
	void initialize() {
        def requireBranch = project.release.git.requireBranch

        if (!requireBranch) {
            return;
        }

		def branch = gitCurrentBranch()

        Validate.assertTrue(branch == requireBranch,
            "Current Git branch is \"$branch\" and not \"${requireBranch}\".")
	}

	@Override
	void checkCommitNeeded() {
		def status = gitStatus()

        def format = { message, files ->
            ([message, LINE, * files, LINE] as String[]).join('\n')
        }

        assertNoUnversionedFiles(status[UNVERSIONED],
            format('You have unversioned files:', status[UNVERSIONED]))

        assertNoModifications(status[UNCOMMITTED],
            format('You have uncommitted files:', status[UNCOMMITTED]))
	}


	@Override
	void checkUpdateNeeded() {
		gitExec(['remote', 'update'], '')

		def status = gitRemoteStatus()

        assertNoPendingCommits(status[AHEAD],
            "You have ${status[AHEAD]} local change(s) to push.")

        assertUpToDate(status[BEHIND],
            "You have ${status[BEHIND]} remote change(s) to pull.")
	}


	@Override
	void createReleaseTag(String message = "") {
		def tagName = tagName()
		gitExec(['tag', '-a', tagName, '-m', message ?: "Created by Release Plugin: ${tagName}"], "Duplicate tag [$tagName]", 'already exists')
		gitExec(['push', 'origin', tagName], '', '! [rejected]', 'error: ', 'fatal: ')
	}


	@Override
	void commit(String message) {
		gitExec(['commit', '-a', '-m', message], '')
		def pushCmd = ['push', 'origin']

		if (plugin.release.git.pushToCurrentBranch) {
			pushCmd << gitCurrentBranch()
		} else {
			def requireBranch = plugin.release.git.requireBranch
			log.debug("commit - {requireBranch: ${requireBranch}}")
			if(requireBranch) {
				pushCmd << requireBranch
			} else {
				pushCmd << 'master'
			}
		}
		gitExec(pushCmd, '', '! [rejected]', 'error: ', 'fatal: ')
	}

	@Override
	void revert() {
		gitExec(['checkout', findPropertiesFile().name], "Error reverting changes made by the release plugin.")
	}

	private String gitCurrentBranch() {
		def matches = gitExec('branch').readLines().grep(~/\s*\*.*/)
		matches[0].trim() - (~/^\*\s+/)
	}

	private Map<String, List<String>> gitStatus() {
		gitExec('status', '--porcelain').readLines().groupBy {
			if (it ==~ /^\s*\?{2}.*/) {
				UNVERSIONED
			} else {
				UNCOMMITTED
			}
		}
	}

	private Map<String, Integer> gitRemoteStatus() {
		def branchStatus = gitExec('status', '-sb').readLines()[0]
		def aheadMatcher = branchStatus =~ /.*ahead (\d+).*/
		def behindMatcher = branchStatus =~ /.*behind (\d+).*/

		def remoteStatus = [:]

		if (aheadMatcher.matches()) {
			remoteStatus[AHEAD] = aheadMatcher[0][1]
		}
		if (behindMatcher.matches()) {
			remoteStatus[BEHIND] = behindMatcher[0][1]
		}
		remoteStatus
	}

	String gitExec(Collection<String> params, String message, String... pattern) {
		def gitDir = resolveGitDir()
		def workTree = resolveWorkTree()
		def cmdLine = ['git', "--git-dir=${gitDir}", "--work-tree=${workTree}"].plus(params)

        return cli.launcher {
            commands = cmdLine
            errorMessage = message
            errorPattern = pattern
		}.out()
	}

	private String resolveGitDir() {
		if (plugin.release.git.scmRootDir) {
			project.rootProject.file(plugin.release.git.scmRootDir + "/.git").canonicalPath.replaceAll("\\\\", "/")
		} else {
			project.rootProject.file(".git").canonicalPath.replaceAll("\\\\", "/")
		}
	}

	private String resolveWorkTree() {
		if (plugin.release.git.scmRootDir) {
			project.rootProject.file(plugin.release.git.scmRootDir).canonicalPath.replaceAll("\\\\", "/")
		} else {
			project.rootProject.projectDir.canonicalPath.replaceAll("\\\\", "/")
		}
	}

	String gitExec(String... params) {
		def gitDir = resolveGitDir()
		def workTree = resolveWorkTree()
		def cmdLine = ['git', "--git-dir=${gitDir}", "--work-tree=${workTree}"]

		cmdLine.addAll params

		return cli.launcher {
            commands = cmdLine
		}.out()
	}
}
