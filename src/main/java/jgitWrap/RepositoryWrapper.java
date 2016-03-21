package jgitWrap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.Merger;
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
  
  private static final String MASTER = "master";
  
  /** Repository */
  private Repository repo;
  private Ident ident;
  
  /**
   * Constructor
   * @param dir git workdirectory
   * @param ident 
   * @throws IOException
   */
  public RepositoryWrapper(File dir, Ident ident) throws IOException {
    this.repo = new FileRepositoryBuilder()
                .setMustExist(true)
                .setGitDir(dir).build();
    this.ident = ident;
  }
  
  public RepositoryWrapper initializeRepo(String filename, byte[] initialReadme, String comment) throws IOException {
    return this.initializeRepo(MASTER, filename, initialReadme, comment);
  }
  
  public RepositoryWrapper initializeRepo(String mastername, String filename, byte[] initialReadme, String comment) throws IOException {
    Branch master = this.branch(mastername);
    Dir root = new Dir();
    root.addFile(filename, initialReadme);
    master.commit(root, comment);
    return this;
  }
  
  public RepositoryWrapper initializeRepo(String comment) throws IOException {
    return this.initializeRepo(MASTER, comment);
  }
  
  public RepositoryWrapper initializeRepo(String mastername, String comment) throws IOException {
    Branch master = this.branch(mastername);
    Dir root = new Dir();
    master.commit(root, comment);
    return this;
  }
  
  /**
   * List all branches of this repo.
   * @return all branches.
   * @throws IOException
   */
  public List<String> listBranches() throws IOException {
    Collection<Ref> values = this.repo.getRefDatabase().getRefs(Constants.R_HEADS).values();
    
    List<String> list = new ArrayList<String>();
    for (Ref ref : values) {
      list.add(ref.getName().substring(Constants.R_HEADS.length()));
    }
    
    return list;
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
    
    private final Repository repo = RepositoryWrapper.this.repo;
    
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
     * Find head ref
     * @return
     * @throws IOException
     */
    public Ref findHeadRef() throws IOException {
      return this.repo.exactRef(Constants.R_HEADS + this.branchName);
    }
    
    /**
     * List all commits of this branch.
     * @return all commits.
     * @throws MissingObjectException
     * @throws IncorrectObjectTypeException
     * @throws IOException
     */
    public List<RevCommit> listCommits() throws MissingObjectException, IncorrectObjectTypeException, IOException {
      Ref head = this.findHeadRef();
      
      try (RevWalk walk = new RevWalk(this.repo)) {
        RevCommit commit = walk.parseCommit(head.getObjectId());
        
        walk.markStart(commit);
        
        List<RevCommit> revs = new ArrayList<RevCommit>();
        for (RevCommit rev : walk) {
          revs.add(rev);
        }
        walk.dispose();
        
        return revs;
      }
    }
    
    /**
     * List all filepaths of head
     * @return
     * @throws IOException
     */
    public List<String> listFiles() throws IOException {
      return listFiles(null);
    }
    
    /**
     * List all filepaths of specified revision.
     * @return
     * @throws IOException
     */
    public List<String> listFiles(RevCommit rev) throws IOException {
      List<String> list = new ArrayList<String>();
      
      try (RevWalk revWalk = new RevWalk(this.repo)) {
        RevCommit commit = rev == null ? revWalk.parseCommit(this.findHeadRef().getObjectId()) : rev;
        RevTree tree = revWalk.parseTree(commit.getTree().getId());
        
        try (TreeWalk treeWalk = new TreeWalk(this.repo)){
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
     * Format entries recursively.
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
     * @return result
     * @throws IOException
     */
    public RefUpdate.Result commit(Dir dir, String message) throws IOException {
      try (ObjectInserter inserter = this.repo.newObjectInserter()) {
        TreeFormatter formatter = formatDir(dir, inserter);
        
        ObjectId treeId = inserter.insert(formatter);
        
        CommitBuilder newCommit = new CommitBuilder();
        newCommit.setCommitter(ident.toPersonIdent());
        newCommit.setAuthor(ident.toPersonIdent());
        newCommit.setMessage(message);
        
        Ref headRef = this.findHeadRef();
        if (headRef != null){
          newCommit.setParentId(headRef.getObjectId());
        }
        newCommit.setTreeId(treeId);
        
        ObjectId newHeadId = inserter.insert(newCommit);
        inserter.flush();
        inserter.close();
        
        RefUpdate refUpdate = this.repo.updateRef(Constants.R_HEADS + this.branchName);
        refUpdate.setNewObjectId(newHeadId);
        refUpdate.setExpectedOldObjectId(headRef.getObjectId());
        
        Result updateResult = refUpdate.update();
        
        if (updateResult == Result.FAST_FORWARD){
          repo.writeMergeCommitMsg(null);
          repo.writeMergeHeads(null);
        }
        
        return updateResult;
      }
    }
    
    private MergeStrategy mergeStrategy = MergeStrategy.RECURSIVE;
    public boolean mergeTo(Branch toBranch) throws IOException {
      ObjectInserter inserter = this.repo.newObjectInserter();
      
      try (RevWalk revWalk = new RevWalk(this.repo)) {
        RevCommit srcCommit = revWalk.parseCommit(this.findHeadRef().getObjectId());
        
        Ref toHeadRef = toBranch.findHeadRef();
        RevCommit toCommit  = revWalk.parseCommit(toHeadRef.getObjectId());
        
        //this.repo.writeMergeCommitMsg("mergeMessage");
        //this.repo.writeMergeHeads(Arrays.asList(repo.exactRef(Constants.HEAD).getObjectId()));
        
        Merger merger = mergeStrategy.newMerger(this.repo);
        merger.merge(srcCommit, toCommit);
        ObjectId mergeResultTreeId = merger.getResultTreeId();
        
        CommitBuilder newCommit = new CommitBuilder();
        newCommit.setCommitter(ident.toPersonIdent());
        newCommit.setAuthor(ident.toPersonIdent());
        newCommit.setMessage("merge commit message");
        newCommit.setParentIds(toCommit.getId(), srcCommit.getId());
        newCommit.setTreeId(mergeResultTreeId);
        
        ObjectId newHeadId = inserter.insert(newCommit);
        inserter.flush();
        inserter.close();
        
//        RefUpdate refUpdateSrc = this.repo.updateRef(Constants.R_HEADS + this.branchName);
//        refUpdateSrc.setNewObjectId(newHeadId);
//        refUpdateSrc.update();
        
        RefUpdate refUpdateTo = this.repo.updateRef(Constants.R_HEADS + toBranch.branchName);
        refUpdateTo.setNewObjectId(newHeadId);
        refUpdateTo.update();
        
        /*
        formatter.append(name, tree);
        
        ObjectId newHeadId = inserter.insert(mergeResultTreeId);
        inserter.flush();
        inserter.close();
        
        RefUpdate refUpdate = this.repo.updateRef(Constants.R_HEADS + toBranch.branchName);
        refUpdate.setNewObjectId(mergeResultTreeId);
        RefUpdate.Result updateResult = refUpdate.update();
        */
        
        return true;
      }
    }
    
    /**
     * Create new branch from this branch.
     * @param newBranchName name of new branch
     * @return instance of new branch
     * @throws IOException
     */
    public Branch newBranch(String newBranchName) throws IOException {
      Branch newBranch = RepositoryWrapper.this.branch(newBranchName);
      
      RefUpdate refUpdate = this.repo.updateRef(Constants.R_HEADS + newBranchName);
      
      Ref findHeadRef = this.findHeadRef();
      refUpdate.setNewObjectId(findHeadRef.getObjectId());
      refUpdate.setRefLogMessage("refLogMessage", false);
      refUpdate.update();
      
      return newBranch;
    }
    
    /**
     * Returns inputstream of file contained by head of this brach.
     * @param path
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    public InputStream getStream(String path) throws IOException, FileNotFoundException {
      try (RevWalk revWalk = new RevWalk(this.repo)){
        RevCommit commit = revWalk.parseCommit(this.findHeadRef().getObjectId());
        RevTree tree = revWalk.parseTree(commit.getTree().getId());
        
        try (TreeWalk treeWalk = new TreeWalk(this.repo)){
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
  
  /** Ident */
  static class Ident {
    private String name;
    private String mail;
    private PersonIdent personIdent;
    
    Ident(String name, String mail){
      this.name = name;
      this.mail = mail;
    }
    
    /** Convert to jgit navtive ident */
    public PersonIdent toPersonIdent(){
      if (personIdent == null)
        personIdent = new PersonIdent(this.name, this.mail);
      return personIdent;
    }
  }
  
  /** Direcotry object for commting */
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
