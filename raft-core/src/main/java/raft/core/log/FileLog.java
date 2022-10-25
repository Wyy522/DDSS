package raft.core.log;

import com.google.common.eventbus.EventBus;
import raft.core.log.sequence.file.FileEntrySequence;

import java.io.File;

public class FileLog extends AbstractLog{
    private final RootDir rootDir;

    public FileLog(File baseDir, EventBus eventBus) {
        super(eventBus);
        rootDir = new RootDir(baseDir);

        LogGeneration latestGeneration = rootDir.getLatestGeneration();
        // TODO add log
        if (latestGeneration != null) {

            FileEntrySequence fileEntrySequence = new FileEntrySequence(latestGeneration, latestGeneration.getLastIncludedIndex()+1);

            // TODO apply last group config entry

        } else {
            LogGeneration firstGeneration = rootDir.createFirstGeneration();
            entrySequence = new FileEntrySequence(firstGeneration, 1);
        }
    }
}
