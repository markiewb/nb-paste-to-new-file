package de.markiewb.plugins.netbeans.pasteasfile.handler;

import org.openide.filesystems.FileObject;

/**
 *
 * @author markiewb
 */
public interface IPasteHandler {
    
    boolean supports(String clipboardContent, FileObject selectedDirectory);
    void handle(final String clipboardContent, FileObject selectedDirectory);
}
