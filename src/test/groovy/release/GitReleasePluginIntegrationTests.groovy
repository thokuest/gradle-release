package release

import org.gradle.testfixtures.ProjectBuilder

import static org.eclipse.jgit.lib.Repository.shortenRefName

@Mixin(PluginHelper)
class GitReleasePluginIntegrationTests extends GitSpecification {

    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localGit.repository.workTree).build()
        project.apply plugin: 'java'
        project.apply plugin: ReleasePlugin
    }

    def cleanup() {
        gitCheckoutBranch(localGit)
        gitCheckoutBranch(remoteGit)
    }

    def 'integration test'() {
        given: 'setting project version to 1.1'
        project.version = '1.1'
        project.setProperty('gradle.release.useAutomaticVersion', "true")
        gitAddAndCommit(localGit, "gradle.properties") { it << "version=$project.version" }
        localGit.push().setForce(true).call()
        when: 'calling release task indirectly'
        project.release.tasks.each {
            project.tasks[it].execute()
        }
        def st = localGit.status().call()
        gitHardReset(remoteGit)
        then: 'project version updated'
        project.version == '1.2'
        and: 'mo modified files in local repo'
        st.modified.size() == 0 && st.added.size() == 0 && st.changed.size() == 0
        and: 'tag with old version 1.1 created in local repo'
        localGit.tagList().call().any { shortenRefName(it.name) == '1.1' }
        and: 'property file updated to new version in local repo'
        localGit.repository.workTree.listFiles().any { it.name == 'gradle.properties' && it.text.contains("version=1.2") }
        and: 'property file with new version pushed to remote repo'
        remoteGit.repository.workTree.listFiles().any { it.name == 'gradle.properties' && it.text.contains("version=1.2") }
        and: 'tag with old version 1.1 pushed to remote repo'
        remoteGit.tagList().call().any { shortenRefName(it.name) == '1.1' }
    }
}
