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

class BranchCreator {
  File dir
  Repository repo
  
  BranchCreator(File dir){
    this.dir = dir
    this.repo = new FileRepositoryBuilder()
      .setMustExist( true )
      .findGitDir( dir ).build()
  }
  
  def create(String branch, String newBranch){
    // find head-ref of from branch
    def head = repo.exactRef(Constants.R_HEADS + branch)

    // create head-ref of new branch
    def refUpdate = repo.updateRef(Constants.R_HEADS + newBranch)
    refUpdate.setNewObjectId(head.getObjectId())
    def updateResult = refUpdate.update()

    def newBranchHead = repo.exactRef(Constants.R_HEADS + newBranch)
  }
  
}

def name = args[0]
def branch = args[1]
def newBranch = args[2]

def dir = new File(new File('.'), name)

def creator = new BranchCreator(dir)
creator.create(branch, newBranch)

