package de.markiewb.plugins.netbeans.pasteasfile;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.swing.JOptionPane;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.datatransfer.ExClipboard;

/**
 * Paste the text from the clipboard into a new file.
 * <p>
 * If the content is a Java source file, then the file will be created in the
 * file/path defined by the fully qualified name of the first top-level element.
 * Else the currently selected DataFolder will be used as folder and the user
 * will be prompted for a filename in this directory.
 * </p>
 *
 * @author markiewb
 */
@ActionID(category = "Editing", id = "sample.PasteAsFile")
@ActionRegistration(displayName = "#CTL_PasteAsFile")
@ActionReferences({
    @ActionReference(path = "Menu/Edit", position = 1400),})
@Messages("CTL_PasteAsFile=Paste as new file")
public final class PasteAsFile implements ActionListener {

    private final DataFolder context;

    public PasteAsFile(DataFolder context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        try {
            final String clipboardContent = getClipboard();
            //TODO java source: action is only action at src or test sourceroot to prevent compileerrors
            JavaSource js = getJavaSourceForString(clipboardContent);
            final boolean isJavaCodeInClipboard = js != null;

            if (isJavaCodeInClipboard) {

                js.runUserActionTask(new CancellableTask<CompilationController>() {

                    @Override
                    public void cancel() {
                        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }

                    @Override
                    public void run(CompilationController p) throws Exception {
                        p.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                        List<? extends TypeElement> topLevelElements = p.getTopLevelElements();
                        if (!topLevelElements.isEmpty()) {
                            TypeElement next = topLevelElements.iterator().next();
                            Name qualifiedName = next.getQualifiedName();
                            String toString = qualifiedName.toString();
                            int lastIndexOf = toString.lastIndexOf(".");
                            if (lastIndexOf < 0) {
                                return;
                            }
                            //FIXME support default package
                            String packageName = toString.substring(0, lastIndexOf);
                            String className = next.getSimpleName().toString();

                            final String fileNameWithExt = className + ".java";
                            try {
                                FileObject packageFolder = FileUtil.createFolder(context.getPrimaryFile(), packageName.replace('.', '/'));
                                FileObject file = FileUtil.createData(packageFolder, fileNameWithExt);
                                writeToFile(file, clipboardContent);
                                openFileInEditor(file);
                            } catch (IOException iOException) {
                                JOptionPane.showMessageDialog(null, "Cannot create " + fileNameWithExt + "\n" + iOException.getMessage());
                            }
                        }
                    }

                }, true);
            } else {
                //fallback to create arbitrary file
                String fileName = JOptionPane.showInputDialog("Name of the new file:", "FromClipboard.txt");
                if (null != fileName) {
                    FileObject file = FileUtil.createData(context.getPrimaryFile(), fileName);
                    writeToFile(file, clipboardContent);
                    //open newly created file in editor
                    openFileInEditor(file);
                }

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

    private JavaSource getJavaSourceForString(final String clipboardContent) throws IllegalArgumentException, IOException {
        //http://blogs.kiyut.com/tonny/2007/09/01/netbeans-platform-and-memory-file-system/#.VQgBHeHHk5w
        FileSystem fs = FileUtil.createMemoryFileSystem();
        FileObject fob = fs.getRoot().createData("dummy", "java");
        try (PrintWriter to = new PrintWriter(fob.getOutputStream())) {
            to.print(clipboardContent);
        }
        JavaSource js = JavaSource.forFileObject(fob);
        return js;
    }

    private void openFileInEditor(FileObject file) throws DataObjectNotFoundException {
        //open newly created file in editor
        DataObject.find(file).getLookup().lookup(OpenCookie.class).open();
    }

    private void writeToFile(FileObject file, String clipboardContent) {
        try (PrintWriter to = new PrintWriter(file.getOutputStream())) {
            to.print(clipboardContent);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
