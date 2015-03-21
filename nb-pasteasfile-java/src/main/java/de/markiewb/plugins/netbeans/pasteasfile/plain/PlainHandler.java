/*
 * Copyright 2015 Benno Markiewicz (benno.markiewicz@googlemail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
