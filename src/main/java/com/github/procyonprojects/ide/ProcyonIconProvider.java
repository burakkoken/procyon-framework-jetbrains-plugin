package com.github.procyonprojects.ide;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Optional;

public class ProcyonIconProvider extends IconProvider {

    @Override
    public @Nullable Icon getIcon(@NotNull PsiElement element, int flags) {
        if (element instanceof PsiDirectory) {
            final VirtualFile directory = ((PsiDirectory)element).getVirtualFile();

            if (directory.getParent().getPath().equals(element.getProject().getBasePath()) && containsApplicationPropertyFile(directory.getParent())) {
                final String directoryName = directory.getName();

                switch (directoryName) {
                    case "resources":
                        return ProcyonIcons.Folder.Resources;
                    case "app":
                        return ProcyonIcons.Folder.App;
                    case "generated":
                        return ProcyonIcons.Folder.Generated;
                    case "web":
                        return ProcyonIcons.Folder.Web;
                    case "configs":
                        return ProcyonIcons.Folder.Configs;
                    case "pkg":
                        return ProcyonIcons.Folder.Pkg;
                    case "cmd":
                        return ProcyonIcons.Folder.Cmd;
                    case "internal":
                        return ProcyonIcons.Folder.Internal;
                }
            } else {
                final String directoryName = directory.getPath().replace(element.getProject().getBasePath(), "");
                if (directoryName.startsWith("/app") || directoryName.startsWith("/pkg") || directoryName.startsWith("/internal")) {
                    return ProcyonIcons.Folder.Module;
                }
            }
        }
        return null;
    }

    private boolean containsApplicationPropertyFile(VirtualFile rootDirectory) {
        final VirtualFile resourcesDirectory = rootDirectory.findChild("resources");
        if (resourcesDirectory == null || !resourcesDirectory.isValid() || !resourcesDirectory.isDirectory()) {
            return false;
        }

        final Optional<VirtualFile> propertiesFile = Arrays.stream(resourcesDirectory.getChildren())
                .filter(virtualFile -> virtualFile.getName().startsWith("procyon") && virtualFile.getName().endsWith("yaml"))
                .findFirst();
        return propertiesFile.isPresent();
    }
}
