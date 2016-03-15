@Grab('org.eclipse.jgit:org.eclipse.jgit:+')
@Grab('org.slf4j:slf4j-jdk14:1.7.18')

import org.eclipse.jgit.*
import org.eclipse.jgit.storage.file.*
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.api.*


println args[0]

def name = args[0]

def dir = new File(new File('.'), name)
dir.mkdir()

def git = Git.init().setDirectory(dir).setBare(false).call()

def readme = new File(dir, 'Readme.md')
readme.write('aaaa')
git.add().addFilepattern('Readme.md').call()

git.commit().setAll(true).setMessage('initial commit').setCommitter('y', 'y@yy.y').call()

git.branchCreate().setName('draft').call()
git.checkout().setName('draft').call()






