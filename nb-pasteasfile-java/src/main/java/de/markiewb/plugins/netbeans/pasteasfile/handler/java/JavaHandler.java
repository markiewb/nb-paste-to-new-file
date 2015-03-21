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
package de.markiewb.plugins.netbeans.pasteasfile.handler.java;

import com.sun.source.tree.CompilationUnitTree;
import de.markiewb.plugins.netbeans.pasteasfile.handler.IPasteHandler;
import static de.markiewb.plugins.netbeans.pasteasfile.handler.Util.openFileInEditor;
import static de.markiewb.plugins.netbeans.pasteasfile.handler.Util.writeToFile;
import java.awt.HeadlessException;
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
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author markiewb
 */
@ServiceProvider(service = IPasteHandler.class)
public class JavaHandler implements IPasteHandler{

    @Override
    public boolean supports(String clipboardContent, org.openide.filesystems.FileObject selectedPackage) {
        try {
            JavaSource js = getJavaSourceForString(clipboardContent);
            if (null==js){
                return false;
            }
            Project pr = FileOwnerQuery.getOwner(selectedPackage);
            if (null==pr) {
                return false;
            }
            final HasToplevelElementsTask task = new HasToplevelElementsTask();
            js.runUserActionTask(task, true);
            
            final boolean isJavaCodeInClipboard = task.hasTopLevelElements;
            
            return isJavaCodeInClipboard;
        } catch (IllegalArgumentException | IOException illegalArgumentException) {
        }
        return false;
    
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

    private static class GetFQNFromTopmostElement implements CancellableTask<CompilationController> {

        private String className;

        private String packageNameFromClipboard;

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
                String fqn = qualifiedName.toString();

                int lastIndexOf = fqn.lastIndexOf(".");

                //support packages and default package
                this.packageNameFromClipboard = (lastIndexOf > 0) ? fqn.substring(0, lastIndexOf) : "";
                this.className = next.getSimpleName().toString();
            }
        }

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

    @Override
    public void handle(final String clipboardContent, FileObject selectedPackage){
        try {
            JavaSource js = getJavaSourceForString(clipboardContent);
            Project pr = FileOwnerQuery.getOwner(selectedPackage);
            
            final GetFQNFromTopmostElement task = new GetFQNFromTopmostElement();
            js.runUserActionTask(task, true);
            String packageNameFromClipboard = task.packageNameFromClipboard;
            if (null != packageNameFromClipboard) {

                //support packages and default package
                String className = task.className;
                final String fileNameWithExt = String.format("%s.java", className);
                
                try {
                    
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
                        folder = selectedPackage;
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
        } catch (IllegalArgumentException | IOException | HeadlessException exception) {
            throw new RuntimeException(exception);
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

    private class HasToplevelElementsTask implements CancellableTask<CompilationController> {

        public HasToplevelElementsTask() {
        }
        private boolean hasTopLevelElements;

        @Override
        public void cancel() {
//                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void run(CompilationController cc) throws Exception {
            cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
            List<? extends TypeElement> topLevelElements = cc.getTopLevelElements();
            hasTopLevelElements=!topLevelElements.isEmpty();
        }
    }
}
