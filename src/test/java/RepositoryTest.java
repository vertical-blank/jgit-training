package jgitWrap;

import org.eclipse.jgit.api.*;
import java.io.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.eclipse.jgit.lib.*;
import jgitWrap.RepositoryWrapper.*;
import jgitWrap.RepositoryWrapper.Branch.*;
import jgitWrap.RepositoryWrapper.Dir;

public class RepositoryTest {
  
  private File parepareDirectory() {
    File repoDir = new File(System.getProperty("java.io.tmpdir"), "tmpRepo");
    repoDir.mkdir();
    return repoDir;
  }
  
  private Git prepareGit(File repoDir) throws Exception {
    return Git.init().setDirectory(repoDir).setBare(false).call();
  }
  
  private void cleanUpRepo(Git git) throws Exception {
    org.apache.commons.io.FileUtils.deleteDirectory(git.getRepository().getDirectory());
  }
  
  @Test
  public void commitOnce() throws Exception {
    File repoDir = parepareDirectory();
    Git git = prepareGit(repoDir);
    
    File readme = new File(repoDir, "README.md");
    Writer writer = new PrintWriter(readme);
    writer.write("initial content");
    writer.close();
    
    git.add().addFilepattern("README.md").call();
    
    git.commit().setAll(true).setMessage("initial commit").setCommitter("y", "y@yy.y").call();
    
    RepositoryWrapper repo = new RepositoryWrapper(repoDir);
    
    Branch master  = repo.branch("master");
    Branch develop = master.newBranch("develop");
    
    Committer committer = develop.commiter();
    String updateContent = "updated";
    committer.addFile("README.md", updateContent.getBytes());
    committer.commit("test commit", new RepositoryWrapper.Ident("Ident", "Ident@Ident.com"));
    
    InputStream stream = develop.getStream("README.md");
    
    String contentFromGit = streamToString(stream);
    assertEquals(contentFromGit, updateContent);
    
    Set<String> branchNames = new HashSet<String>();
    for(Ref branchRef: git.branchList().call()){
      branchNames.add(branchRef.getName());
    }
    assertEquals(branchNames, new HashSet(Arrays.asList(Constants.R_HEADS + "master", Constants.R_HEADS + "develop")));
    
    // clean up.
    cleanUpRepo(git);
  }
  
  @Test
  public void commitTwice() throws Exception {
    File repoDir = parepareDirectory();
    Git git = prepareGit(repoDir);
    
    File readme = new File(repoDir, "README.md");
    Writer writer = new PrintWriter(readme);
    writer.write("initial content");
    writer.close();
    
    git.add().addFilepattern("README.md").call();
    
    git.commit().setAll(true).setMessage("initial commit").setCommitter("y", "y@yy.y").call();
    
    RepositoryWrapper repo = new RepositoryWrapper(repoDir);
    
    Branch master  = repo.branch("master");
    Branch develop = master.newBranch("develop");
    
    Committer committer = develop.commiter();
    RepositoryWrapper.Ident ident = new RepositoryWrapper.Ident("Ident", "Ident@Ident.com");
    String firstContent = "first";
    committer.addFile("README.md", firstContent.getBytes());
    committer.commit("first commit", ident);
    
    String secondContent = "second";
    String anotherContent = "another";
    committer.addFile("ANOTHER.md", anotherContent.getBytes());
    committer.addFile("README.md", secondContent.getBytes());
    committer.commit("second commit", ident);
    
    InputStream readmeStream = develop.getStream("README.md");
    assertEquals(streamToString(readmeStream), secondContent);
    
    InputStream anotherStream = develop.getStream("ANOTHER.md");
    assertEquals(streamToString(anotherStream), anotherContent);
    
    Set<String> branchNames = new HashSet<String>();
    for(Ref branchRef: git.branchList().call()){
      branchNames.add(branchRef.getName());
    }
    assertEquals(branchNames, new HashSet(Arrays.asList(Constants.R_HEADS + "master", Constants.R_HEADS + "develop")));
    
    // clean up.
    cleanUpRepo(git);
  }
  
  @Test
  public void commitDirectory() throws Exception {
    File repoDir = parepareDirectory();
    Git git = prepareGit(repoDir);
    
    File readme = new File(repoDir, "README.md");
    Writer writer = new PrintWriter(readme);
    writer.write("initial content");
    writer.close();
    
    git.add().addFilepattern("README.md").call();
    
    git.commit().setAll(true).setMessage("initial commit").setCommitter("y", "y@yy.y").call();
    
    RepositoryWrapper repo = new RepositoryWrapper(repoDir);
    
    Branch master  = repo.branch("master");
    Branch develop = master.newBranch("develop");
    
    
    Committer committer = develop.commiter();
    RepositoryWrapper.Ident ident = new RepositoryWrapper.Ident("Ident", "Ident@Ident.com");
    
    String firstContent = "dirctorieeeeeeeees";
    committer.addFile("README.md", firstContent.getBytes());
    
    Dir dir1 = new Dir("child1");
    Dir dir2 = new Dir("child2");
    Dir dir1_1 = new Dir("child1-child1");
    Dir dir1_2 = new Dir("child1-child2");
    Dir dir2_1 = new Dir("child2-child1");
    Dir dir2_2 = new Dir("child2-child2");
    committer.addDir(dir1).addDir(dir2);
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
    
    committer.commit("dirctories commit", ident);
    
    assertEquals(
      new HashSet(develop.listFiles()), 
      new HashSet(Arrays.asList(
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
