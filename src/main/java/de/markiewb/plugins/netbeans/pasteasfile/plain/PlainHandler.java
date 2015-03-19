package de.markiewb.plugins.netbeans.pasteasfile.plain;

import static de.markiewb.plugins.netbeans.pasteasfile.Util.openFileInEditor;
import static de.markiewb.plugins.netbeans.pasteasfile.Util.writeToFile;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author markiewb
 */
public class PlainHandler {

    private final FileObject selectedDir;

    public PlainHandler(FileObject selectedDir) {
        this.selectedDir = selectedDir;
    }

    public void handle(final String clipboardContent) throws IOException {
        //fallback to create arbitrary file in current folder
        String fileName = JOptionPane.showInputDialog("Name of the new file:", "FromClipboard.txt");
        if (null != fileName) {
            final File fileToCreate = new File(FileUtil.toFile(selectedDir), fileName);
            if (fileToCreate.exists()) {
                JOptionPane.showMessageDialog(null, String.format("Cannot create a file from clipboard content.\nFile %s already exists.", fileToCreate.getAbsolutePath()), "Paste to new file", JOptionPane.WARNING_MESSAGE);
                return;
            }

            FileObject file = FileUtil.createData(selectedDir, fileName);
            writeToFile(file, clipboardContent);
            //open newly created file in editor
            openFileInEditor(file);
        }
    }
}
