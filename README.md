<hr>
<h1 style="color: #FF0000">Looking for maintainers, who want to take over the development!</h1>
<hr>
# Description

NetBeans plugin which pastes textual clipboard content into a new file. This is useful if you have to copy code samples (especially whole java classes) from online tutorials.

# How to install

## Install via Plugin Manager
![InstallFromPluginCenter](https://github.com/markiewb/nb-paste-to-new-file/blob/master/doc/InstallFromPluginCenter.png?raw=true)


## Download manually

* Core-Plugin: http://plugins.netbeans.org/plugin/57739/?show=true
* Java-Addon for the Core-Plugin: http://plugins.netbeans.org/plugin/57740/?show=true

# Usage

## Paste text from clipboard into a new file

Select a node in the project view and then invoke `Menu|Edit|Paste to new file`. A dialog will ask you for the file name and after confirming it a new file will be created with the given name and the content from the clipboard.
![PasteToNewFile](https://github.com/markiewb/nb-paste-to-new-file/blob/master/doc/PasteToNewFile.gif?raw=true)

## Paste Java code from clipboard into a new file

* The idea of this feature is taken from Eclipse IDE and IntelliJ Idea, but now it is also available for NetBeans IDE

* **Variant 1:** Select a "Source" or "Test Sources" node and paste Java code (incl. package declaration) via `Menu|Edit|Paste to new file` and a Java file is created in the package defined by the pasted source code.
* **Variant 2:** Select a package node below the "Source" or "Test Sources" node and paste Java code (incl. package declaration) via `Menu|Edit|Paste to new file` and a Java file is created in the selected package. The package declaration in the file is updated automatically.
![PasteToNewJavaFile](https://github.com/markiewb/nb-paste-to-new-file/blob/master/doc/PasteToNewJavaFile.gif?raw=true)
