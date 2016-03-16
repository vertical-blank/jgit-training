package jgitWrap

import org.scalatest.FunSuite

import org.eclipse.jgit.api._
import java.io._
import scalax.file.Path

class RepositorySpec extends FunSuite {
  test("Branch,Commit,AndRead") {
    
    val repoDir = new File(System.getProperty("java.io.tmpdir"), "tmpRepo")
    println("testDir: " + repoDir)
    repoDir.mkdir()
    
    val git = Git.init().setDirectory(repoDir).setBare(false).call()
    val readme = new File(repoDir, "README.md")
    val writer = new PrintWriter(readme)
    writer.write("initial content")
    writer.close()
    git.add().addFilepattern("README.md").call()
    
    git.commit().setAll(true).setMessage("initial commit").setCommitter("y", "y@yy.y").call()
    
    
    val repo = new Repository(repoDir)
    
    val master  = repo.branch("master")
    val develop = master.newBranch("develop")
    
    val committer = develop.commiter
    val updateContent = "updated"
    committer.addFile("README.md", updateContent)
    committer.commit("test commit", Ident("Ident", "Ident@Ident.com"))
    
    val stream = develop.getStream("README.md")
    val contentFromGit = scala.io.Source.fromInputStream(stream).mkString
    assert(contentFromGit == updateContent)
    
    Path(repoDir).deleteRecursively(true)

  }
}
