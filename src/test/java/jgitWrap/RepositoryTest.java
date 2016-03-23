package jgitWrap;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
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
    
    RepositoryWrapper repo = new RepositoryWrapper(repoDir, ident).initializeRepo("initial commit");
    
    Branch master  = repo.branch("master");
    Branch develop = master.newBranch("develop");
    
    Dir root = new Dir();
    String updateContent = "updated";
    root.put("README.md", updateContent.getBytes());
    develop.commit(root, "test commit");
    
    InputStream stream = develop.getStream("README.md");
    
    String contentFromGit = streamToString(stream);
    assertEquals(contentFromGit, updateContent);
    
    assertEquals(new HashSet<String>(repo.listBranches()), new HashSet<String>(Arrays.asList("master", "develop")));
    
    assertEquals(master.getHead().listFiles(), Collections.emptyList());
    
    // clean up.
    cleanUpRepo(git);
  }
  
  @Test
  public void commitTwice() throws Exception {
    File repoDir = new File(System.getProperty("java.io.tmpdir"), "commitTwice");
    
    Git git = Git.init().setDirectory(repoDir).setBare(false).call();
    git.close();
    
    RepositoryWrapper repo = new RepositoryWrapper(new File(repoDir, ".git"), ident).initializeRepo("README.md", "initial".getBytes(), "initial commit");
    
    Branch master  = repo.branch("master");
    Branch develop = master.newBranch("develop");
    Thread.sleep(1000);
    
    Dir root = new Dir();
    String firstContent = "first";
    root.put("README.md", firstContent.getBytes());
    develop.commit(root, "first commit");
    Thread.sleep(1000);
    
    root = new Dir();
    
    String secondContent = "second";
    String anotherContent = "another";
    root.put("ANOTHER.md", anotherContent.getBytes());
    root.put("README.md", secondContent.getBytes());
    develop.commit(root, "second commit");
    Thread.sleep(1000);
    
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
    root.put("README.md", firstContent.getBytes());
    
    Dir dir1 = new Dir("child1");
    Dir dir2 = new Dir("child2");
    Dir dir1_1 = new Dir("child1-child1");
    Dir dir1_2 = new Dir("child1-child2");
    Dir dir2_1 = new Dir("child2-child1");
    Dir dir2_2 = new Dir("child2-child2");
    root.put(dir1).put(dir2);
    dir1.put(dir1_1).put(dir1_2);
    dir2.put(dir2_1).put(dir2_2);
    
    
    dir1.put("1.md", "1__1".getBytes());
    dir1.put("2.md", "1__2".getBytes());
    dir2.put("1.md", "2__1".getBytes());
    dir2.put("2.md", "2__2".getBytes());
    
    dir1_1.put("1.md", "1_1__1".getBytes());
    dir1_1.put("2.md", "1_1__2".getBytes());
    dir1_2.put("1.md", "1_2__1".getBytes());
    dir1_2.put("2.md", "1_2__2".getBytes());
    dir2_1.put("1.md", "2_1__1".getBytes());
    dir2_1.put("2.md", "2_1__2".getBytes());
    dir2_2.put("1.md", "2_2__1".getBytes());
    dir2_2.put("2.md", "2_2__2".getBytes());
    
    develop.commit(root, "dirctories commit");
    
    assertEquals(
      new HashSet<String>(develop.getHead().listFiles()),
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

    assertEquals(streamToString(develop.getStream("README.md")), "dirctorieeeeeeeees");
    assertEquals(streamToString(develop.getStream("child1/1.md")), "1__1");
    assertEquals(streamToString(develop.getStream("child2/2.md")), "2__2");
    assertEquals(streamToString(develop.getStream("child1/child1-child1/1.md")), "1_1__1");
    assertEquals(streamToString(develop.getStream("child2/child2-child2/2.md")), "2_2__2");
    
    Dir dir = develop.getHead().getDir();
    
    assertEquals(new String(dir.file("README.md")), "dirctorieeeeeeeees");
    assertEquals(new String(dir.dir("child1").file("1.md")), "1__1");
    assertEquals(new String(dir.dir("child2").file("2.md")), "2__2");
    assertEquals(new String(dir.dir("child1").dir("child1-child1").file("1.md")), "1_1__1");
    assertEquals(new String(dir.dir("child2").dir("child2-child2").file("2.md")), "2_2__2");
    
    // clean up.
    cleanUpRepo(git);
  }
  
  @Test
  public void commitAndMerge() throws Exception {
    
    File repoDir = new File(System.getProperty("java.io.tmpdir"), "commitAndMerge");
    repoDir.mkdir();
    
    Git git = Git.init().setDirectory(repoDir).setBare(false).call();
    git.close();
    
    RepositoryWrapper repo = new RepositoryWrapper(new File(repoDir, ".git"), ident).initializeRepo("README.md", "initial".getBytes(), "initial commit");
    
    Branch master  = repo.branch("master");
    
    Branch develop = master.newBranch("develop");
    
    Dir root = new Dir();
    String updateContent = "updated";
    root.put("README.md", updateContent.getBytes());
    develop.commit(root, "test commit");
    
    InputStream stream = develop.getStream("README.md");
    String contentFromGit = streamToString(stream);
    assertEquals(contentFromGit, updateContent);

    develop.mergeTo(master);
    
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
