package uk.ac.ebi.biostudies.service.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.*;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FileSystemIndexTransferer implements IndexTransferer {
    private final static Logger LOGGER = LoggerFactory.getLogger(FileSystemIndexTransferer.class);


    @Override
    public void copyIndex(String srcIndex, String destIndex) {
        try {
            File indexDirectory = new File(srcIndex);
            File[] allFilesToCopy = indexDirectory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return !name.contains(".lock");
                }
            });
            if (allFilesToCopy == null)
                return;
            for (File myFile : allFilesToCopy) {
                Files.copy(myFile.toPath(), Paths.get(destIndex, myFile.getName()), StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (Exception ex) {
            LOGGER.error("Problem in copying file", ex);
        }
    }

    @Override
    public void copyIndexFromSnapShot(Collection<String> indexFiles, String srcDir, String destDir) {
        if (indexFiles == null)
            return;
        File segmentFile = findSegmentFile(destDir);
        try {
            if (segmentFile != null && segmentFile.exists())
                segmentFile.delete();
        }catch (Exception ex){
            LOGGER.error("Problem in removing segment file from destination", ex);

        }
        Path destination = Path.of(destDir);
        try {
            Files.createDirectories(destination);
            FileUtils.cleanDirectory(destination.toFile());
        }catch (Exception exception){
            LOGGER.debug("Was not able to create directories", exception);
        }
        for (String curFile : indexFiles) {
            try {
                Files.copy(Paths.get(srcDir + "/" + curFile), Paths.get(destDir + "/" + curFile), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ex) {
                LOGGER.error("Problem in copying file", ex);
            }
        }

    }

    public void copyIndexFromNetworkFileSystemToLocal(String srcDirectory, String destDirectory) throws Exception{
        File segmentFile = findSegmentFile(destDirectory);
        if (segmentFile != null && segmentFile.exists())
            segmentFile.delete();
        File[] files = Paths.get(srcDirectory).toFile().listFiles();
        if(files == null)
            return;
        for(File file:files) {
            try {
                Files.copy(file.toPath(), Paths.get(destDirectory + "/" + file.getName()), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ex) {
                LOGGER.error("", ex);
            }
        }

    }

    private File findSegmentFile (String destinationIndexPath){
        File dir = new File(destinationIndexPath);
        File[] matches = dir.listFiles((directory, name) -> name.startsWith("segments"));
        if (matches != null && matches.length > 0)
            return matches[0];
        return null;
    }
}