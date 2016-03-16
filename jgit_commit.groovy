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

class Comitter {
  File dir
  Repository repo
  ObjectInserter inserter
  
  Tree root = new Tree("root")
  
  Comitter(File dir){
    this.dir = dir
    this.repo = new FileRepositoryBuilder()
      .setMustExist( true )
      .findGitDir( dir ).build()
    this.inserter = repo.newObjectInserter()
  }
  
  class Tree {
    def formatter = new TreeFormatter()
    
    String name
    Tree(String name){
      this.name = name
    }
    
    def addFile(String name, String content) {
      def objId = inserter.insert(Constants.OBJ_BLOB, content.getBytes('utf-8'))
      this.formatter.append(name, FileMode.REGULAR_FILE, objId)
      return this
    }
  
    def addTree(Tree tree) {
      def objId = inserter.insert(tree.formatter)
      this.formatter.append(tree.name, FileMode.TREE, objId)
      return this
    }
  }
  
  def tree(String name){
    return new Tree(name)
  }
  
  def addFile(String name, String content) {
    return root.addFile(name, content)
  }
  def addTree(Tree tree) {
    return root.addTree(tree)
  }
  
  def commit(String branch, String message, PersonIdent user){
    def head = repo.exactRef(Constants.R_HEADS + branch)
    
    def treeId = this.inserter.insert(this.root.formatter)
    
    def newCommit = new CommitBuilder()
    newCommit.setCommitter(user)
    newCommit.setAuthor(user)
    newCommit.setMessage(message)
    newCommit.setParentIds([head.getObjectId()])
    newCommit.setTreeId(treeId)
    
    def newHeadId = this.inserter.insert(newCommit)
    this.inserter.flush()
    this.inserter.close()
    
    def refUpdate = repo.updateRef(Constants.R_HEADS + branch)
    refUpdate.setNewObjectId(newHeadId)
    refUpdate.update()
  }
  
}

def name = args[0]
def branch = args[1]

def user = new PersonIdent('y', 'y@yy')
def dir = new File(new File('.'), name)

def comitter = new Comitter(dir)

comitter.addFile("README.md", "Groooooooooooooooovy")
comitter.addFile("test.md", "teeeeeeeeeeeeeeeeeeest")
comitter.addFile("add.md", "addddddddddddddddddddd")

def tree = comitter.tree("tree")

tree.addFile("child.md", "Im a child")

comitter.addTree(tree)

comitter.commit(branch, "From Groovy", user)

