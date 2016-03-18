package jgitWrap;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;

import jgitWrap.RepositoryWrapper.Branch;
import jgitWrap.RepositoryWrapper.Dir;
import jgitWrap.RepositoryWrapper.Ident;

import org.eclipse.jgit.api.Git;
import org.junit.Test;

public class RepositoryTest {
  
  private Ident ident = new RepositoryWrapper.Ident("Ident", "Ident@Ident.com");

  private File parepareDirectory() {
    File repoDir = new File(System.getProperty("java.io.tmpdir"), "tmpRepo.git");
    repoDir.mkdir();
    return repoDir;
  }
  
  private Git prepareGit(File repoDir) throws Exception {
    return Git.init().setGitDir(repoDir).setBare(true).call();
  }
  
  private void cleanUpRepo(Git git) throws Exception {
    org.apache.commons.io.FileUtils.deleteDirectory(git.getRepository().getDirectory());
  }
  
  @Test
  public void commitOnce() throws Exception {
    File repoDir = parepareDirectory();
    Git git = prepareGit(repoDir);
    
    RepositoryWrapper repo = new RepositoryWrapper(repoDir, ident).initializeRepo("README.md", "initial".getBytes(), "initial commit");
    
    Branch master  = repo.branch("master");
    Branch develop = master.newBranch("develop");
    
    Dir root = new Dir();
    String updateContent = "updated";
    root.addFile("README.md", updateContent.getBytes());
    develop.commit(root, "test commit");
    
    InputStream stream = develop.getStream("README.md");
    
    String contentFromGit = streamToString(stream);
    assertEquals(contentFromGit, updateContent);
    
    assertEquals(new HashSet<String>(repo.listBranches()), new HashSet<String>(Arrays.asList("master", "develop")));
    
    // clean up.
    cleanUpRepo(git);
  }
  
  @Test
  public void commitTwice() throws Exception {
    File repoDir = parepareDirectory();
    Git git = prepareGit(repoDir);
    
    RepositoryWrapper repo = new RepositoryWrapper(repoDir, ident).initializeRepo("README.md", "initial".getBytes(), "initial commit");
    
    Branch master  = repo.branch("master");
    Branch develop = master.newBranch("develop");
    
    Dir root = new Dir();
    String firstContent = "first";
    root.addFile("README.md", firstContent.getBytes());
    develop.commit(root, "first commit");
    
    String secondContent = "second";
    String anotherContent = "another";
    root.addFile("ANOTHER.md", anotherContent.getBytes());
    root.addFile("README.md", secondContent.getBytes());
    develop.commit(root, "second commit");
    
    assertEquals(streamToString(develop.getStream("README.md")), secondContent);
    assertEquals(streamToString(develop.getStream("ANOTHER.md")), anotherContent);
    
    assertEquals(streamToString(master.getStream("README.md")), "initial");
    
    // clean up.
    cleanUpRepo(git);
  }
  
  @Test
  public void commitNestedDirectory() throws Exception {
    File repoDir = parepareDirectory();
    Git git = prepareGit(repoDir);
    
    RepositoryWrapper repo = new RepositoryWrapper(repoDir, ident).initializeRepo("README.md", "initial".getBytes(), "initial commit");
    
    Branch master  = repo.branch("master");
    Branch develop = master.newBranch("develop");
    
    Dir root = new Dir();
    
    String firstContent = "dirctorieeeeeeeees";
    root.addFile("README.md", firstContent.getBytes());
    
    Dir dir1 = new Dir("child1");
    Dir dir2 = new Dir("child2");
    Dir dir1_1 = new Dir("child1-child1");
    Dir dir1_2 = new Dir("child1-child2");
    Dir dir2_1 = new Dir("child2-child1");
    Dir dir2_2 = new Dir("child2-child2");
    root.addDir(dir1).addDir(dir2);
    dir1.addDir(dir1_1).addDir(dir1_2);
    dir2.addDir(dir2_1).addDir(dir2_2);
    
    
    dir1.addFile("1.md", "1__1".getBytes());
    dir1.addFile("2.md", "1__2".getBytes());
    dir2.addFile("1.md", "2__1".getBytes());
    dir2.addFile("2.md", "2__2".getBytes());
    
    dir1_1.addFile("1.md", "1_1__1".getBytes());
    dir1_1.addFile("2.md", "1_1__2".getBytes());
    dir1_2.addFile("1.md", "1_2__1".getBytes());
    dir1_2.addFile("2.md", "1_2__2".getBytes());
    dir2_1.addFile("1.md", "2_1__1".getBytes());
    dir2_1.addFile("2.md", "2_1__2".getBytes());
    dir2_2.addFile("1.md", "2_2__1".getBytes());
    dir2_2.addFile("2.md", "2_2__2".getBytes());
    
    develop.commit(root, "dirctories commit");
    
    assertEquals(
      new HashSet<String>(develop.listFiles()),
      new HashSet<String>(Arrays.asList(
        "README.md",
        "child1/1.md",
        "child1/2.md",
        "child1/child1-child1/1.md",
        "child1/child1-child1/2.md",
        "child1/child1-child2/1.md",
        "child1/child1-child2/2.md",
        "child2/1.md",
        "child2/2.md",
        "child2/child2-child1/1.md",
        "child2/child2-child1/2.md",
        "child2/child2-child2/1.md",
        "child2/child2-child2/2.md"
      ))
    );
    
    // clean up.
    cleanUpRepo(git);
  }
  
  private String streamToString(InputStream stream) throws Exception {
    String contentFromGit = null;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))){
      StringBuilder sb = new StringBuilder();
      while(true){
        String line = br.readLine();
        if (line == null){
          break;
        }
        sb.append(line);
      }
      contentFromGit = sb.toString();
    }
    return contentFromGit;
  }
  
}
