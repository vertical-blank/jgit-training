@Grab('org.eclipse.jgit:org.eclipse.jgit:+')
@Grab('org.slf4j:slf4j-jdk14:1.7.18')

import org.eclipse.jgit.*
import org.eclipse.jgit.storage.file.*
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.api.*
import org.eclipse.jgit.revwalk.*
import org.eclipse.jgit.treewalk.*
import org.eclipse.jgit.treewalk.filter.*
import org.eclipse.jgit.dircache.*


println args[0]

def name = args[0]
def branch = args[1]

def user = new PersonIdent('y', 'y@yy')
def dir = new File(new File('.'), name)

def repository = new FileRepositoryBuilder()
  .setMustExist( true )
  .findGitDir( dir ).build()

// find head-ref of specified branch
def head = repository.exactRef("refs/heads/" + branch)
println("Ref of refs/heads/" + branch + ": " + head)

// show head info.
def loader = repository.open(head.getObjectId())
loader.copyTo(System.out)

// insert file content.
def inserter = repository.newObjectInserter()
def readmeId = inserter.insert(Constants.OBJ_BLOB, "Groooooooooooooooovy".getBytes('utf-8'))
inserter.flush()

// insert tree that contains changed file.
TreeFormatter treeFormatter = new TreeFormatter()
treeFormatter.append( "Readme.md", FileMode.REGULAR_FILE, readmeId );
def treeId = inserter.insert( treeFormatter )
inserter.flush()

// construct and insert commit.
def newCommit = new CommitBuilder()
newCommit.setCommitter(user)
newCommit.setAuthor(user)
newCommit.setMessage('From Groovy!')
newCommit.setParentIds([head.getObjectId()])
newCommit.setTreeId(treeId)
def newHeadId = inserter.insert(newCommit)
inserter.flush()
inserter.close()

// update head-ref to latest commit.
def refUpdate = repository.updateRef('refs/heads/' + branch)
refUpdate.setNewObjectId(newHeadId)
refUpdate.update()




