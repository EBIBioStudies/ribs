package uk.ac.ebi.biostudies.api.util;

import java.io.IOException;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy;
import org.apache.lucene.index.SnapshotDeletionPolicy;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;

public class SnapshotAwareDirectoryTaxonomyWriter extends DirectoryTaxonomyWriter {

    private SnapshotDeletionPolicy facetIndexSnapShot;

    public SnapshotAwareDirectoryTaxonomyWriter(Directory facetDirectory, IndexWriterConfig.OpenMode openMode) throws IOException{
            super(facetDirectory, openMode);
    }

    @Override
    protected IndexWriterConfig	createIndexWriterConfig(IndexWriterConfig.OpenMode openMode){
        facetIndexSnapShot = new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
        IndexWriterConfig modifiedConfig = super.createIndexWriterConfig(openMode).setIndexDeletionPolicy(facetIndexSnapShot);
        return modifiedConfig;
    }

    public SnapshotDeletionPolicy getDeletionPolicy(){
        return facetIndexSnapShot;
    }

    public boolean isOpen(){
        try {
            super.ensureOpen();
            return true;
        }catch (AlreadyClosedException exception){
            return false;
        }
    }


}
