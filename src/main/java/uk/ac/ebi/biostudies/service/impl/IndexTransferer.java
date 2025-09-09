package uk.ac.ebi.biostudies.service.impl;

import java.util.Collection;

public interface IndexTransferer {
    void copyIndexFromSnapShot(Collection<String> indexFiles, String srcDir, String destDir);
    void copyIndex(String srcIndexDir, String destIndexDir);
    void copyIndexFromNetworkFileSystemToLocal(String srcDirectory, String destDirectory) throws Exception;

}
