package de.markiewb.plugins.netbeans.pasteasfile;

import com.sun.source.tree.CompilationUnitTree;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.swing.JOptionPane;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
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

    @Override
    public void actionPerformed(ActionEvent ev) {
        try {
            final String clipboardContent = getClipboard();
            //TODO java source: action is only action at src or test sourceroot to prevent compileerrors
            JavaSource js = getJavaSourceForString(clipboardContent);
            final boolean isJavaCodeInClipboard = js != null;
            Project pr = FileOwnerQuery.getOwner(context.getPrimaryFile());

            if (isJavaCodeInClipboard && pr != null) {
                //pasting into the project view
                handeJavaCode(js, pr, clipboardContent);
            } else {
                //a) pasting non java code everywhere
                //b) pasting java code in favorites view
                handleArbitraryText(clipboardContent);

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

    /**
     * Taken from http://hg.netbeans.org/main/rev/045508faade4
     *
     * @param file
     */
    private void fixPackagename(FileObject file, final String newPackage) {
        try {
            JavaSource js = JavaSource.forFileObject(file);
            ModificationResult runModificationTask = js.runModificationTask(new CancellableTask<WorkingCopy>() {

                @Override
                public void cancel() {
                }

                @Override
                public void run(WorkingCopy p) throws Exception {
                    p.toPhase(JavaSource.Phase.RESOLVED);
                    TreeMaker treeMaker = p.getTreeMaker();

                    CompilationUnitTree cut = p.getCompilationUnit();
                    if (cut.getPackageName() != null && !"".equals(newPackage)) { // NOI18N
                        p.rewrite(cut.getPackageName(), treeMaker.Identifier(newPackage));

                    } else {
                        // in order to handle default package, we have to rewrite whole
                        // compilation unit:
                        CompilationUnitTree newCut = treeMaker.CompilationUnit(
                                "".equals(newPackage) ? null : treeMaker.Identifier(newPackage), // NOI18N
                                cut.getImports(),
                                cut.getTypeDecls(),
                                cut.getSourceFile()
                        );
                        p.rewrite(cut, newCut);
                    }
                }
            });
            runModificationTask.commit();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    private void handeJavaCode(JavaSource js, final Project pr, final String clipboardContent) throws IOException {
        js.runUserActionTask(new CancellableTask<CompilationController>() {

            @Override
            public void cancel() {
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

                    //support packages and default package
                    String packageNameFromClipboard = (lastIndexOf > 0) ? toString.substring(0, lastIndexOf) : "";
                    String className = next.getSimpleName().toString();
                    final String fileNameWithExt = String.format("%s.java", className);

                    try {
                        final FileObject selectedPackage = context.getPrimaryFile();

                        String newPackageName;
                        String packageFromSelectedFolder = getPackageNameFromFolder(pr, selectedPackage);
                        FileObject srcRootForFolder = getSrcRootForFolder(pr, selectedPackage);
                        final boolean isSourceRootSelected = "".equals(packageFromSelectedFolder);
                        if (isSourceRootSelected) {
                            //selected source root, so use package from clipboard content
                            newPackageName = packageNameFromClipboard;

                        } else {
                            //non-source root selected, use package from selected folder
                            newPackageName = packageFromSelectedFolder;
                        }

                        final boolean isDefaultPackage = null == newPackageName || newPackageName.isEmpty();
                        FileObject folder;
                        if (isDefaultPackage) {
                            //default package, so create in current folder
                            folder = context.getPrimaryFile();
                        } else {
                            //is in different package, so create the folders to match the packagename
                            folder = FileUtil.createFolder(srcRootForFolder, newPackageName.replace('.', '/'));
                        }

                        final File fileToCreate = new File(FileUtil.toFile(folder), fileNameWithExt);
                        if (fileToCreate.exists()) {
                            JOptionPane.showMessageDialog(null, String.format("Cannot create a file from clipboard content.\nFile %s already exists.", fileToCreate.getAbsolutePath()), "Paste to new file", JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        FileObject file = FileUtil.createData(folder, fileNameWithExt);
                        writeToFile(file, clipboardContent);

                        fixPackagename(file, newPackageName);
                        openFileInEditor(file);
                    } catch (IOException iOException) {
                        JOptionPane.showMessageDialog(null, String.format("Cannot create %s\n%s", fileNameWithExt, iOException.getMessage()));
                    }
                }
            }

            private String getPackageNameFromFolder(Project pr, final FileObject selectedPackage) {
                FileObject srcRootForFolder = getSrcRootForFolder(pr, selectedPackage);
                if (null != srcRootForFolder) {
                    String relativePath = FileUtil.getRelativePath(srcRootForFolder, selectedPackage);
                    return relativePath.replace('/', '.');
                } else {
                    return "";
                }
            }

            private FileObject getSrcRootForFolder(Project pr, final FileObject selectedPackage) {
                for (String type : new String[]{JavaProjectConstants.SOURCES_TYPE_JAVA, JavaProjectConstants.SOURCES_TYPE_RESOURCES}) {
                    for (SourceGroup sg : ProjectUtils.getSources(pr).getSourceGroups(type)) {
                        if (selectedPackage == sg.getRootFolder() || (FileUtil.isParentOf(sg.getRootFolder(), selectedPackage) /*&& sg.contains(file)*/)) {
                            return sg.getRootFolder();
                        }
                    }
                }
                return null;
            }

        }, true);
    }

    private void handleArbitraryText(final String clipboardContent) throws IOException {
        //fallback to create arbitrary file in current folder
        String fileName = JOptionPane.showInputDialog("Name of the new file:", "FromClipboard.txt");
        if (null != fileName) {
            FileObject file = FileUtil.createData(context.getPrimaryFile(), fileName);
            writeToFile(file, clipboardContent);
            //open newly created file in editor
            openFileInEditor(file);
        }
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
