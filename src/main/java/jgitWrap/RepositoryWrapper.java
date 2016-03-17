package jgitWrap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

/**
 * Jgit Lowlevel-api repository Wrapper.
 * 
 * @author yohei224
 */
class RepositoryWrapper {
  
  /** Repository */
  private Repository repo;
  
  /**
   * Constructor
   * @param dir git workdirectory
   * @throws IOException
   */
  public RepositoryWrapper(File dir) throws IOException {
    this.repo = new FileRepositoryBuilder()
                .setMustExist(true)
                .findGitDir(dir).build();
  }
  
  /**
   * get branch instance by name.
   * @param branchName
   * @return
   */
  public Branch branch(String branchName){
    return new Branch(branchName);
  }
  
  /** Branch */
  class Branch {
    
    /** branchName */
    public final String branchName;
    
    /**
     * Constructor
     * @param branchName
     */
    public Branch(String branchName) {
      this.branchName = branchName;
    }
    
    /**
     * find head ref
     * @return
     * @throws IOException
     */
    private Ref findHeadRef() throws IOException {
      return RepositoryWrapper.this.repo.exactRef(Constants.R_HEADS + this.branchName);
    }
    
    /**
     * list all filepaths of head
     * @return
     * @throws IOException
     */
    public List<String> listFiles() throws IOException {
      List<String> list = new ArrayList<String>();
      
      try (RevWalk revWalk = new RevWalk(RepositoryWrapper.this.repo)) {
        RevCommit commit = revWalk.parseCommit(this.findHeadRef().getObjectId());
        RevTree tree = revWalk.parseTree(commit.getTree().getId());
        
        try (TreeWalk treeWalk = new TreeWalk(RepositoryWrapper.this.repo)){
          treeWalk.addTree(tree);
          treeWalk.setRecursive(true);
          
          while(treeWalk.next()){
            list.add(treeWalk.getPathString());
          }
        }
      }
      
      return list;
    }
    
    /**
     * format entries recursively.
     * @param dir dir instance
     * @param inserter ObjectInserter
     * @return treeFormatter contains all entries.
     * @throws IOException
     */
    private TreeFormatter formatDir(Dir dir, ObjectInserter inserter) throws IOException {
      TreeFormatter formatter = new TreeFormatter();
      
      for (Map.Entry<String, Object> entry : dir.files.entrySet()){
        ObjectId objId = inserter.insert(Constants.OBJ_BLOB, (byte[]) entry.getValue());
        formatter.append(entry.getKey(), FileMode.REGULAR_FILE, objId);
      }
      
      for (Map.Entry<String, Dir> entry : dir.dirs.entrySet()){
        TreeFormatter dirFormatter = formatDir(entry.getValue(), inserter);
        ObjectId objId = inserter.insert(dirFormatter);
        formatter.append(entry.getKey(), FileMode.TREE, objId);
      }
      
      return formatter;
    }
    
    /**
     * Execute commit to repo.
     * @param message commit message
     * @param ident committer ident
     * @return result
     * @throws IOException
     */
    public RefUpdate.Result commit(Dir dir, String message, Ident ident) throws IOException {
      ObjectInserter inserter = RepositoryWrapper.this.repo.newObjectInserter();
  
      TreeFormatter formatter = formatDir(dir, inserter);
      
      ObjectId treeId = inserter.insert(formatter);
      
      CommitBuilder newCommit = new CommitBuilder();
      newCommit.setCommitter(ident.toPersonIdent());
      newCommit.setAuthor(ident.toPersonIdent());
      newCommit.setMessage(message);
      newCommit.setParentId(Branch.this.findHeadRef().getObjectId());
      newCommit.setTreeId(treeId);
      
      ObjectId newHeadId = inserter.insert(newCommit);
      inserter.flush();
      inserter.close();
      
      RefUpdate refUpdate = RepositoryWrapper.this.repo.updateRef(Constants.R_HEADS + Branch.this.branchName);
      refUpdate.setNewObjectId(newHeadId);
      RefUpdate.Result updateResult = refUpdate.update();
      
      return updateResult;
    }
    
    /**
     * Create new branch from this branch.
     * @param newBranchName name of new branch
     * @return instance of new branch
     * @throws IOException
     */
    public Branch newBranch(String newBranchName) throws IOException {
      Branch newBranch = RepositoryWrapper.this.branch(newBranchName);
      
      RefUpdate refUpdate = RepositoryWrapper.this.repo.updateRef(Constants.R_HEADS + newBranchName);
      refUpdate.setNewObjectId(this.findHeadRef().getObjectId());
      refUpdate.update();
      
      return newBranch;
    }
    
    /**
     * returns inputstream of file contained by head of this brach.
     * @param path
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    public InputStream getStream(String path) throws IOException, FileNotFoundException {
      try (RevWalk revWalk = new RevWalk(RepositoryWrapper.this.repo)){
        RevCommit commit = revWalk.parseCommit(this.findHeadRef().getObjectId());
        RevTree tree = revWalk.parseTree(commit.getTree().getId());
        
        try (TreeWalk treeWalk = new TreeWalk(RepositoryWrapper.this.repo)){
          treeWalk.addTree(tree);
          treeWalk.setFilter(PathFilter.create(path));
          if(!treeWalk.next()){
            throw new FileNotFoundException("Couldnt find file.");
          }
          
          return repo.open(treeWalk.getObjectId(0)).openStream();
        }
      }
    }
  }
  
  /** ident */
  static class Ident {
    private String name;
    private String mail;
    
    Ident(String name, String mail){
      this.name = name;
      this.mail = mail;
    }
    
    /** convert to jgit navtive ident */
    public PersonIdent toPersonIdent(){
      return new PersonIdent(this.name, this.mail);
    }
  }
  
  /** direcotry object for commting */
  static class Dir {
    final String name;
    
    private Map<String, Dir>    dirs  = new HashMap<String, Dir>();
    private Map<String, Object> files = new HashMap<String, Object>();
    
    public Dir(){
      this.name = "root";
    }
    
    public Dir(String name){
      this.name = name;
    }
    
    public Dir addFile(String filename, byte[] content) throws IOException {
      this.files.put(filename, content);
      return this;
    }
    
    public Dir addDir(Dir dir) throws IOException {
      this.dirs.put(dir.name, dir);
      return this;
    }
  }
}
