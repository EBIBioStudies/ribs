package uk.ac.ebi.biostudies.service;

public interface IndexManagementService {
    void stopAcceptingSubmissionMessagesAndCloseIndices();
    void openIndicesWritersAndSearchersStartStomp();
    boolean isClosed();
    void closeWebsocket();
    void openWebsocket();
}
