package jgitWrap

import org.eclipse.jgit.lib.{Repository => JRepository}
import org.eclipse.jgit.lib._
import org.eclipse.jgit.storage.file._
import org.eclipse.jgit.api._
import org.eclipse.jgit.revwalk._
import org.eclipse.jgit.treewalk._
import org.eclipse.jgit.treewalk.filter._
import org.eclipse.jgit.dircache._

import java.io.{File, InputStream}

case class Ident(name: String, mail: String) {
  lazy val asJava = new PersonIdent(name, mail)
}

class Repository(dir: File) {
  val repo: JRepository = new FileRepositoryBuilder()
      .setMustExist( true )
      .findGitDir( dir ).build()
  
  def findHead(branch: String) = repo.exactRef(Constants.R_HEADS + branch)
  
  def branch(branchName: String): Branch = new Branch(branchName)
  
  class Branch(branchName: String) {
    def head = findHead(branchName)
    
    def commiter: Committer = new Committer()
    
    class Committer {
      val inserter = repo.newObjectInserter()
      val root = Tree("root")
      
      def tree(name: String) = new Tree(name)
      
      def addFile(name: String, content: String): Committer = {
        root.addFile(name, content)
        this
      }
      
      def addTree(tree: Tree): Committer = {
        root.addTree(tree)
        this
      }
      
      def commit(message: String, user: Ident): Unit = {
        val treeId = this.inserter.insert(this.root.formatter)
        
        val newCommit = new CommitBuilder()
        newCommit.setCommitter(user.asJava)
        newCommit.setAuthor(user.asJava)
        newCommit.setMessage(message)
        newCommit.setParentId(head.getObjectId())
        newCommit.setTreeId(treeId)
        
        val newHeadId = this.inserter.insert(newCommit)
        this.inserter.flush()
        this.inserter.close()
        
        val refUpdate = repo.updateRef(Constants.R_HEADS + branchName)
        refUpdate.setNewObjectId(newHeadId)
        refUpdate.update()
        
        new Branch(branchName)
      }
      
      case class Tree(name: String) {
        val formatter = new TreeFormatter()
        
        def addFile(filename: String, content: String): Tree = {
          val objId = inserter.insert(Constants.OBJ_BLOB, content.getBytes("utf-8"))
          this.formatter.append(filename, FileMode.REGULAR_FILE, objId)
          this
        }
      
        def addTree(tree: Tree): Tree = {
          val objId = inserter.insert(tree.formatter)
          this.formatter.append(tree.name, FileMode.TREE, objId)
          this
        }
      }
    }
    
    def newBranch(newBranchName: String): Branch = {
      val refUpdate = repo.updateRef(Constants.R_HEADS + newBranchName)
      refUpdate.setNewObjectId(head.getObjectId())
      val updateResult = refUpdate.update()

      repo.exactRef(Constants.R_HEADS + newBranchName)
      new Branch(newBranchName)
    }
    
    def getStream(path: String): InputStream = {
      val walk = new RevWalk(repo)
      
      val commit = walk.parseCommit(head.getObjectId())
      val tree = walk.parseTree(commit.getTree().getId())
      
      val treeWalk = new TreeWalk(repo)
      treeWalk.addTree(tree)
      treeWalk.setFilter(PathFilter.create(path))
      if(!treeWalk.next()){
        throw new Exception("Couldnt find file.");
      }
      
      val loader = repo.open(treeWalk.getObjectId(0));
      
      walk.dispose()
      
      loader.openStream()
    }
  }
}
