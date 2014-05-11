package release

import org.gradle.api.Project

import release.helper.Validate

/**
 * @author elberry
 * @author evgenyg
 * Created: Tue Aug 09 23:26:04 PDT 2011
 */
class BzrReleasePlugin extends BaseScmPlugin {
	private static final String ERROR = 'ERROR'
	private static final String DELIM = '\n  * '

    BzrReleasePlugin(Project project) {
        super(project)
    }

	@Override
	void initialize() {
		boolean hasXmlPlugin = cli.launcher {
            commands = ['bzr', 'plugins']
		}.out().readLines().any { it.startsWith('xmloutput') }

        Validate.assertTrue(hasXmlPlugin,
            'The required xmloutput plugin is not installed in Bazaar, please install it.')
	}

	@Override
	void checkCommitNeeded() {
		String out = cli.launcher {
            commands = ['bzr', 'xmlstatus']
		}.out()

		def xml = new XmlSlurper().parseText(out)
		def added = xml.added?.size() ?: 0
		def modified = xml.modified?.size() ?: 0
		def removed = xml.removed?.size() ?: 0
		def unknown = xml.unknown?.size() ?: 0

		def format = { String name ->
			["${name.capitalize()}:",
					xml."$name".file.collect { it.text().trim() },
					xml."$name".directory.collect { it.text().trim() }].
					flatten().
					join(DELIM) + '\n'
		}

        assertNoUnversionedFiles(unknown, "You have un-versioned files:\n${format('unknown')}")

        assertNoModifications(added || modified || removed,
            'You have un-committed files:\n' +
                (added ? format('added') : '') +
                (modified ? format('modified') : '') +
                (removed ? format('removed') : ''))
	}


	@Override
	void checkUpdateNeeded() {
		String out = cli.launcher {
            commands = ['bzr', 'xmlmissing']
		}.out()

		def xml = new XmlSlurper().parseText(out)
		int extra = ("${xml.extra_revisions?.@size}" ?: 0) as int
		int missing = ("${xml.missing_revisions?.@size}" ?: 0) as int

		//noinspection GroovyUnusedAssignment
		def format = {
			int number, String name, String path ->

			["You have $number $name changes${ number == 1 ? '' : 's' }:",
					xml."$path".logs.log.collect {
						int cutPosition = 40
						String message = it.message.text()
						message = message.readLines()[0].substring(0, Math.min(cutPosition, message.size())) +
								(message.size() > cutPosition ? ' ..' : '')
						"[$it.revno]: [$it.timestamp][$it.committer][$message]"
					}].
					flatten().
					join(DELIM)
		}

        assertNoPendingCommits(extra > 0,
            format(extra, 'unpublished', 'extra_revisions'))

        assertUpToDate(missing > 0,
            format(missing, 'missing', 'missing_revisions'))
	}

	/**
	 * Uses 'bzr tag [name]'.
	 * @param message ignored.
	 */
	@Override
	void createReleaseTag(String message = "") {
		cli.launcher {
            commands = ['bzr', 'tag', tagName()]
            errorMessage = 'Error creating tag'
            errorPattern = ERROR
		}.execute()
	}


	@Override
	void commit(String message) {
		cli.launcher {
            commands = ['bzr', 'ci', '-m', message]
            errorMessage = 'Error committing new version'
            errorPattern = ERROR
		}.execute()

		cli.launcher {
            commands = ['bzr', 'push', ':parent']
            errorMessage = 'Error committing new version'
            errorPattern = ERROR
		}.execute()
	}

	@Override
	void revert() {
		cli.launcher {
            commands = ['bzr', 'revert', findPropertiesFile().name]
            errorMessage = 'Error reverting changes made by the release plugin.'
            errorPattern = ERROR
		}.execute()
	}
}