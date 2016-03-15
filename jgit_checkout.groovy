@Grab('org.eclipse.jgit:org.eclipse.jgit:+')
@Grab('org.slf4j:slf4j-jdk14:1.7.18')

import org.eclipse.jgit.*
import org.eclipse.jgit.storage.file.*
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.api.*


println args[0]

def name = args[0]
def branch = args[1]

def dir = new File(new File('.'), name)

def repository = new FileRepositoryBuilder()
  .setMustExist( true )
  .findGitDir( dir ).build()

def git = Git.wrap(repository)

git.checkout().setName(branch).call()




