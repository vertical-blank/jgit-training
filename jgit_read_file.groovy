@Grab('org.eclipse.jgit:org.eclipse.jgit:+')
@Grab('org.slf4j:slf4j-jdk14:1.7.18')

import org.eclipse.jgit.*
import org.eclipse.jgit.storage.file.*
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.api.*
import org.eclipse.jgit.revwalk.*
import org.eclipse.jgit.treewalk.*
import org.eclipse.jgit.treewalk.filter.*

class Reader {
  File dir
  Repository repo
  
  Reader(File dir){
    this.dir = dir
    this.repo = new FileRepositoryBuilder()
      .setMustExist( true )
      .findGitDir( dir ).build()
  }
  
  def read(String branch, String filePath){
    def head = repo.exactRef(Constants.R_HEADS + branch)

    def walk = new RevWalk(repo)

    def commit = walk.parseCommit(head.getObjectId())
    def tree = walk.parseTree(commit.getTree().getId())

    def treeWalk = new TreeWalk(repo)
    treeWalk.addTree(tree)
    treeWalk.setFilter(PathFilter.create(filePath))
    if(!treeWalk.next()){
      throw new Exception("Couldnt find file.");
    }

    def loader = repo.open(treeWalk.getObjectId(0));

    walk.dispose()
    
    return loader.openStream()
  }
  
}


def name = args[0]
def branch = args[1]

def dir = new File(new File('.'), name)

def reader = new Reader(dir)
def stream = reader.read(branch, "README.md")

(stream as Closeable).withCloseable {
  (new java.io.BufferedReader(new java.io.InputStreamReader(stream)) as Closeable).withCloseable {
    while (true) {
      def line = it.readLine()
      if (line == null) break
      println line
    }
  }
}

