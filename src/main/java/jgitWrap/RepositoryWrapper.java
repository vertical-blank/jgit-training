package jgitWrap;

import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.storage.file.*;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.treewalk.*;
import org.eclipse.jgit.treewalk.filter.*;
import org.eclipse.jgit.dircache.*;

import java.io.*;
import java.util.*;

class RepositoryWrapper {
  private Repository repo;
  
  public RepositoryWrapper(File dir) throws IOException {
    this.repo = new FileRepositoryBuilder()
                .setMustExist(true)
                .findGitDir(dir).build();
  }
  
  public Branch branch(String branchName){
    return new Branch(branchName);
  }
  
  class Branch {
    public final String branchName;
    
    public Branch(String branchName) {
      this.branchName = branchName;
    }
    
    private Ref findHead() throws IOException {
      return RepositoryWrapper.this.repo.exactRef(Constants.R_HEADS + this.branchName);
    }
    
    public List<String> listFiles() throws IOException {
      List<String> list = new ArrayList<String>();
      
      try (RevWalk revWalk = new RevWalk(RepositoryWrapper.this.repo)) {
        RevCommit commit = revWalk.parseCommit(this.findHead().getObjectId());
        RevTree tree = revWalk.parseTree(commit.getTree().getId());
        
        TreeWalk treeWalk = new TreeWalk(RepositoryWrapper.this.repo);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        
        while(treeWalk.next()){
          list.add(treeWalk.getPathString());
        }
      }
      
      return list;
    }
    
    public Committer commiter() {
      return new Committer();
    }
    
    class Committer {
      private Dir root = new Dir("root");
      
      public Committer addFile(String name, byte[] content) throws IOException {
        this.root.addFile(name, content);
        return this;
      }
      
      public Committer addDir(Dir dir) throws IOException {
        this.root.addDir(dir);
        return this;
      }
      
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
      
      public RefUpdate.Result commit(String message, Ident ident) throws IOException {
        ObjectInserter inserter = RepositoryWrapper.this.repo.newObjectInserter();
    
        TreeFormatter formatter = formatDir(this.root, inserter);
        
        ObjectId treeId = inserter.insert(formatter);
        
        CommitBuilder newCommit = new CommitBuilder();
        newCommit.setCommitter(ident.toPersonIdent());
        newCommit.setAuthor(ident.toPersonIdent());
        newCommit.setMessage(message);
        newCommit.setParentId(Branch.this.findHead().getObjectId());
        newCommit.setTreeId(treeId);
        
        ObjectId newHeadId = inserter.insert(newCommit);
        inserter.flush();
        inserter.close();
        
        RefUpdate refUpdate = RepositoryWrapper.this.repo.updateRef(Constants.R_HEADS + Branch.this.branchName);
        refUpdate.setNewObjectId(newHeadId);
        RefUpdate.Result updateResult = refUpdate.update();
        
        this.reset();
        
        return updateResult;
      }
      
      private void reset(){
        this.root = new Dir("root");
      }
    }
    
    public Branch newBranch(String newBranchName) throws IOException {
      Branch newBranch = RepositoryWrapper.this.branch(newBranchName);
      
      RefUpdate refUpdate = RepositoryWrapper.this.repo.updateRef(Constants.R_HEADS + newBranchName);
      refUpdate.setNewObjectId(this.findHead().getObjectId());
      RefUpdate.Result updateResult = refUpdate.update();
      
      return newBranch;
    }
    
    public InputStream getStream(String path) throws IOException, FileNotFoundException {
      RevWalk revWalk = new RevWalk(RepositoryWrapper.this.repo);
      
      RevCommit commit = revWalk.parseCommit(this.findHead().getObjectId());
      RevTree tree = revWalk.parseTree(commit.getTree().getId());
      
      TreeWalk treeWalk = new TreeWalk(RepositoryWrapper.this.repo);
      treeWalk.addTree(tree);
      treeWalk.setFilter(PathFilter.create(path));
      if(!treeWalk.next()){
        throw new FileNotFoundException("Couldnt find file.");
      }
      
      ObjectLoader loader = repo.open(treeWalk.getObjectId(0));
      
      revWalk.dispose();
      
      return loader.openStream();
    }
  }
  
  static class Ident {
    private String name;
    private String mail;
    
    Ident(String name, String mail){
      this.name = name;
      this.mail = mail;
    }
    
    public PersonIdent toPersonIdent(){
      return new PersonIdent(this.name, this.mail);
    }
  }
      
  static class Dir {
    final String name;
    
    private Map<String, Dir>    dirs  = new HashMap<String, Dir>();
    private Map<String, Object> files = new HashMap<String, Object>();
    
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
