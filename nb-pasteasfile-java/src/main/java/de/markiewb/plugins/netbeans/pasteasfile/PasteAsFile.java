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
package de.markiewb.plugins.netbeans.pasteasfile;

import de.markiewb.plugins.netbeans.pasteasfile.java.JavaHandler;
import de.markiewb.plugins.netbeans.pasteasfile.plain.PlainHandler;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.loaders.DataFolder;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.datatransfer.ExClipboard;

/**
 * Paste the text from the clipboard into a new file.
 * <p>
 * If the content is a Java source file, the packagename will be adapted to
 * prevent compile errors - eclipse-alike - by the following scheme:
 * <ul>
 * <li>pasting at source root (like src/main/java) will create the file in the
 * package defined by the packagename in the clipboard (=folders will be created
 * to match sourcecode's packagename)</li>
 * <li>pasting at non-source root (like src/main/java/com/foo) will create the
 * file in the package defined by the selected folder (=sourcecode's packagename
 * will be modified to match the folder structure)</li></li>
 * </ul>
 * Else the currently selected DataFolder will be used as folder and the user
 * will be prompted for a filename in this directory.
 * </p>
 *
 * @author markiewb
 */
@ActionID(category = "Editing", id = "sample.PasteAsFile")
@ActionRegistration(displayName = "#CTL_PasteAsFile")
@ActionReferences({
    @ActionReference(path = "Menu/Edit", position = 1325),
    @ActionReference(path = "Shortcuts", name = "DOS-V")
})
@Messages("CTL_PasteAsFile=Paste to new file")
public final class PasteAsFile implements ActionListener {

    private final DataFolder context;

    public PasteAsFile(DataFolder context) {
        this.context = context;
    }

    public PasteAsFile() {
        this.context = null;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        try {
            final String clipboardContent = getClipboard();

            final JavaHandler javaHandler = new JavaHandler(context.getPrimaryFile());
            final PlainHandler plainHandler = new PlainHandler(context.getPrimaryFile());

            if (javaHandler.supports(clipboardContent)) {
                //pasting into the project view
                javaHandler.handle(clipboardContent);
            } else {
                //a) pasting non java code everywhere
                //b) pasting java code in favorites view
                plainHandler.handle(clipboardContent);

            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public String getClipboard() {
        Transferable t = Lookup.getDefault().lookup(ExClipboard.class).getContents(null);
        try {
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return (String) t.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (UnsupportedFlavorException | IOException e) {
        }
        return null;
    }

}
