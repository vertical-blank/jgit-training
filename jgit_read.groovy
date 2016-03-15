@Grab('org.eclipse.jgit:org.eclipse.jgit:+')
@Grab('org.slf4j:slf4j-jdk14:1.7.18')

import org.eclipse.jgit.*
import org.eclipse.jgit.storage.file.*
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.api.*
import org.eclipse.jgit.revwalk.*
import org.eclipse.jgit.treewalk.*
import org.eclipse.jgit.treewalk.filter.*

println args[0]

def name = args[0]
def branch = args[1]

def dir = new File(new File('.'), name)

def repository = new FileRepositoryBuilder()
  .setMustExist( true )
  .findGitDir( dir ).build()

def git = Git.wrap(repository)

def head = repository.exactRef("refs/heads/" + branch)
println("Ref of refs/heads/" + branch + ": " + head)

def loader = repository.open(head.getObjectId());
loader.copyTo(System.out);


def walk = new RevWalk(repository)

def commit = walk.parseCommit(head.getObjectId());
def tree = walk.parseTree(commit.getTree().getId());
println("Found Tree: " + tree);

def treeWalk = new TreeWalk(repository)
treeWalk.addTree(tree)
treeWalk.setFilter(PathFilter.create("Readme.md"))
if(!treeWalk.next()){
  throw new Exception("Couldnt find file.");
}


loader = repository.open(treeWalk.getObjectId(0));
loader.copyTo(System.out);

walk.dispose();



