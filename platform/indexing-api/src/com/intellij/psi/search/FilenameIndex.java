// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.psi.search;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.util.*;
import com.intellij.util.containers.CollectionFactory;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashingStrategy;
import com.intellij.util.containers.Java11Shim;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.IdFilter;
import com.intellij.util.indexing.ProcessorWithThrottledCancellationCheck;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class FilenameIndex {
  /** @deprecated Use {@link FilenameIndex} methods instead **/
  @Deprecated(forRemoval = true)
  @ApiStatus.Internal
  public static final ID<String, Void> NAME = ID.create("FilenameIndex");

  private FilenameIndex() {
  }

  public static @NotNull String @NotNull [] getAllFilenames(@NotNull Project project) {
    Set<String> names = CollectionFactory.createSmallMemoryFootprintSet();
    processAllFileNames((String s) -> {
      names.add(s);
      return true;
    }, GlobalSearchScope.allScope(project), null);
    return ArrayUtilRt.toStringArray(names);
  }

  public static void processAllFileNames(@NotNull Processor<? super String> processor,
                                         @NotNull GlobalSearchScope scope,
                                         @Nullable IdFilter filter) {
    var withThrottledCancellationCheck = new ProcessorWithThrottledCancellationCheck<>(
      (CharSequence s) -> processor.process(s.toString())
    );
    processAllFileNameCharSequences(withThrottledCancellationCheck, scope, filter);
  }

  private static void processAllFileNameCharSequences(@NotNull Processor<? super CharSequence> processor,
                                                      @NotNull GlobalSearchScope scope,
                                                      @Nullable IdFilter filter) {
    FileBasedIndex.getInstance().processAllKeys(NAME, processor, scope, filter);
  }

  /** @deprecated Use {@link FilenameIndex#getVirtualFilesByName(String, GlobalSearchScope)} */
  @SuppressWarnings("unused")
  @Deprecated
  public static @NotNull @Unmodifiable Collection<VirtualFile> getVirtualFilesByName(Project project,
                                                                                     @NotNull String name,
                                                                                     @NotNull GlobalSearchScope scope) {
    return getVirtualFilesByName(name, scope);
  }

  public static @NotNull @Unmodifiable Collection<VirtualFile> getVirtualFilesByName(@NotNull String name, @NotNull GlobalSearchScope scope) {
    return getVirtualFilesByNames(Set.of(name), scope, null);
  }

  /** @deprecated Use {@link FilenameIndex#getVirtualFilesByName(String, boolean, GlobalSearchScope)} */
  @SuppressWarnings("unused")
  @Deprecated
  public static @NotNull @Unmodifiable Collection<VirtualFile> getVirtualFilesByName(Project project,
                                                                                     @NotNull String name,
                                                                                     boolean caseSensitively,
                                                                                     @NotNull GlobalSearchScope scope) {
    return getVirtualFilesByName(name, caseSensitively, scope);
  }

  public static @NotNull @Unmodifiable Collection<VirtualFile> getVirtualFilesByName(@NotNull String name,
                                                                                     boolean caseSensitively,
                                                                                     @NotNull GlobalSearchScope scope) {
    if (caseSensitively) return getVirtualFilesByName(name, scope);
    return getVirtualFilesByNamesIgnoringCase(Set.of(name), scope, null);
  }

  /** @deprecated Use {@link #getVirtualFilesByName(String, GlobalSearchScope)} **/
  @Deprecated
  public static @NotNull PsiFile @NotNull [] getFilesByName(@NotNull Project project,
                                                            @NotNull String name,
                                                            @NotNull GlobalSearchScope scope) {
    return (PsiFile[])getFilesByName(project, name, scope, false);
  }

  /** @deprecated Use {@link #processFilesByName(String, boolean, GlobalSearchScope, Processor)} **/
  @Deprecated(forRemoval = true)
  public static boolean processFilesByName(@NotNull String name,
                                           boolean directories,
                                           @NotNull Processor<? super PsiFileSystemItem> processor,
                                           @NotNull GlobalSearchScope scope,
                                           @NotNull Project project) {
    return processFilesByName(name, directories, processor, scope, project, null);
  }

  /** @deprecated Use {@link #processFilesByName(String, boolean, GlobalSearchScope, Processor)} **/
  @Deprecated
  public static boolean processFilesByName(@NotNull String name,
                                           boolean directories,
                                           @NotNull Processor<? super PsiFileSystemItem> processor,
                                           @NotNull GlobalSearchScope scope,
                                           @NotNull Project project,
                                           @Nullable IdFilter idFilter) {
    PsiManager psiManager = PsiManager.getInstance(project);
    boolean[] result = {false}; // keep old semantics
    processFilesByNames(Set.of(name), true, scope, idFilter, file -> {
      if (!file.isValid()) return true;
      if (directories != file.isDirectory()) return true;
      PsiFileSystemItem psi = directories ? psiManager.findDirectory(file) : psiManager.findFile(file);
      if (psi == null) return true;
      result[0] = true;
      return processor.process(psi);
    });
    return result[0];
  }

  public static boolean processFilesByName(@NotNull String name,
                                           boolean caseSensitively,
                                           @NotNull GlobalSearchScope scope,
                                           @NotNull Processor<? super VirtualFile> processor) {
    return processFilesByNames(Set.of(name), caseSensitively, scope, null, processor);
  }

  public static boolean processFilesByNames(@NotNull Set<String> names,
                                            boolean caseSensitively,
                                            @NotNull GlobalSearchScope scope,
                                            @Nullable IdFilter idFilter,
                                            @NotNull Processor<? super VirtualFile> processor) {
    if (names.isEmpty()) return true;
    Collection<VirtualFile> files = caseSensitively ? getVirtualFilesByNames(names, scope, idFilter) :
                                    getVirtualFilesByNamesIgnoringCase(names, scope, idFilter);
    return ContainerUtil.process(files, processor);
  }

  private static @NotNull @Unmodifiable Set<VirtualFile> getVirtualFilesByNamesIgnoringCase(@NotNull Set<String> names,
                                                                                            @NotNull GlobalSearchScope scope,
                                                                                            @Nullable IdFilter idFilter) {
    Set<String> nameSet = CollectionFactory.createCustomHashingStrategySet(HashingStrategy.caseInsensitive());
    nameSet.addAll(names);
    Set<String> keys = CollectionFactory.createSmallMemoryFootprintSet();
    processAllFileNames(value -> {
      if (nameSet.contains(value)) {
        keys.add(value);
      }
      return true;
    }, scope, idFilter);
    return getVirtualFilesByNames(keys, scope, idFilter);
  }

  /** @deprecated Use {@link #getVirtualFilesByName(String, GlobalSearchScope)} **/
  @Deprecated
  public static @NotNull PsiFileSystemItem @NotNull [] getFilesByName(@NotNull Project project,
                                                                      @NotNull String name,
                                                                      final @NotNull GlobalSearchScope scope,
                                                                      boolean directories) {
    SmartList<PsiFileSystemItem> result = new SmartList<>();
    Processor<PsiFileSystemItem> processor = Processors.cancelableCollectProcessor(result);
    processFilesByName(name, directories, processor, scope, project);

    if (directories) {
      return result.toArray(new PsiFileSystemItem[0]);
    }
    //noinspection SuspiciousToArrayCall
    return result.toArray(PsiFile.EMPTY_ARRAY);
  }

  /**
   * Returns all files in the project by extension
   *
   * @param project current project
   * @param ext     file extension without leading dot e.q. "txt", "wsdl"
   * @return all files with provided extension
   * @author Konstantin Bulenkov
   */
  public static @NotNull @Unmodifiable Collection<VirtualFile> getAllFilesByExt(@NotNull Project project, @NotNull String ext) {
    return getAllFilesByExt(project, ext, GlobalSearchScope.allScope(project));
  }

  public static @NotNull @Unmodifiable Collection<VirtualFile> getAllFilesByExt(@NotNull Project project,
                                                                                @NotNull String ext,
                                                                                @NotNull GlobalSearchScope searchScope) {
    if (ext.isEmpty()) {
      return Java11Shim.INSTANCE.listOf();
    }

    String dotExt = "." + ext;
    int len = ext.length() + 1;

    Set<String> names = new HashSet<>();
    processAllFileNames(name -> {
      int length = name.length();
      if (length > len && name.regionMatches(true, length - len, dotExt, 0, len)) {
        names.add(name);
      }
      return true;
    }, GlobalSearchScope.allScope(project), null);
    return getVirtualFilesByNames(names, searchScope, null);
  }

  private static @NotNull @Unmodifiable Set<VirtualFile> getVirtualFilesByNames(@NotNull Set<String> names,
                                                                                @NotNull GlobalSearchScope scope,
                                                                                @Nullable IdFilter filter) {
    Set<VirtualFile> files = CollectionFactory.createSmallMemoryFootprintSet();
    FileBasedIndex.getInstance().processFilesContainingAnyKey(
      NAME, names, scope, filter, null,
      new ProcessorWithThrottledCancellationCheck<>(file -> {
        files.add(file);
        return true;
      })
    );
    return files;
  }
}
