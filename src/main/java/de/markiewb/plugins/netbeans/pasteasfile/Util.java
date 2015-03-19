package de.markiewb.plugins.netbeans.pasteasfile;

import java.io.IOException;
import java.io.PrintWriter;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;

/**
 *
 * @author markiewb
 */
public class Util {

    public static void openFileInEditor(FileObject file) throws DataObjectNotFoundException {
        //open newly created file in editor
        DataObject.find(file).getLookup().lookup(OpenCookie.class).open();
    }

    public static void writeToFile(FileObject file, String clipboardContent) {
        try (PrintWriter to = new PrintWriter(file.getOutputStream())) {
            to.print(clipboardContent);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
