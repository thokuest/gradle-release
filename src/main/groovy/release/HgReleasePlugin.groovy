package release

import org.gradle.api.Project

/**
 * @author elberry
 * @author evgenyg
 */
class HgReleasePlugin extends BaseScmPlugin {
	private static final String ERROR = 'abort:'

    HgReleasePlugin(Project project) {
        super(project)
    }

	@Override
	void checkCommitNeeded() {
		def modifications = ['A': [], 'M': [], 'R': [], '?': []]

		cli.launcher {
            commands = ['hg', 'status']
		}.out().eachLine { line ->
			def mods = modifications[line[0]]
			if (mods != null) { mods << line }
		}

        assertNoModifications(modifications['?'],
            "You have ${modifications['?'].size()} un-versioned files.")

        def format = { collection, label ->
            def count = collection.size()

            count ? "$count $label" : ''
        }

        assertNoModifications(modifications.count { k, v -> v },
             'You have '
                 + format(modifications['A'], 'added')
                 + format(modifications['M'], 'modified')
                 + format(modifications['R'], 'removed'));
	}


	@Override
	void checkUpdateNeeded() {
		def modifications = ['in': [], 'out': []]

		cli.launcher {
            commands = ['hg', 'in', '-q']
		}.out().eachLine { line ->
			modifications['in'] << line
		}
		cli.launcher {
            commands = ['hg', 'out', '-q']
        }.out().eachLine { line ->
			modifications['out'] << line
		}

        assertNoPendingCommits(modifications['out'], "You have ${modifications['out'].size()} outgoing changes")
        assertUpToDate(modifications['in'], "You have ${modifications['in'].size()} incoming changes")
	}


	@Override
	void createReleaseTag(String message = "") {
		def tagName = tagName()

		cli.launcher {
            commands = ['hg', 'tag', "-m", message ?: "Created by Release Plugin: ${tagName}", tagName]
            errorMessage = 'Error creating tag'
            errorPattern = ERROR
		}.execute()
	}

	@Override
	void commit(String message) {
		cli.launcher {
            commands = ['hg', 'ci', '-m', message]
            errorMessage = 'Error committing new version'
            errorPattern = ERROR
		}.execute()

		cli.launcher {
            commands = ['hg', 'push']
            errorMessage = 'Error committing new version'
            errorPattern = ERROR
		}.execute()
	}

    @Override
	void revert() {
		cli.launcher {
            commands = ['hg', 'revert', findPropertiesFile().name]
            errorMessage = 'Error reverting changes made by the release plugin.'
            errorPattern = ERROR
		}.execute()
	}
}