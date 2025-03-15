package dev.balakumar.promptforge.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import dev.balakumar.promptforge.models.RelatedFile
import dev.balakumar.promptforge.settings.PromptForgeSettings
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import java.util.concurrent.atomic.AtomicInteger
import java.util.HashSet

class RelatedFilesCollector(private val project: Project) {
    private val LOG = Logger.getInstance(RelatedFilesCollector::class.java)
    private val settings = PromptForgeSettings.getInstance()
    private val fileIndex = ProjectRootManager.getInstance(project).fileIndex

    fun collectRelatedFiles(file: VirtualFile): List<RelatedFile> {
        if (!settings.state.includeRelatedFiles) {
            return emptyList()
        }
        val result = mutableListOf<RelatedFile>()
        val processedFiles = mutableSetOf<String>()
        val decompiledFilesCount = AtomicInteger(0)
        val directReferences = mutableSetOf<String>()
        val indirectReferences = mutableSetOf<String>()
        processedFiles.add(file.path)
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return emptyList()
        collectDirectReferences(psiFile, directReferences)
        processFileRecursively(
            psiFile,
            result,
            processedFiles,
            directReferences,
            indirectReferences,
            0,
            settings.state.maxDepth,
            settings.state.maxRelatedFiles,
            decompiledFilesCount
        )
        return result
    }

    private fun collectDirectReferences(psiFile: PsiFile, directReferences: MutableSet<String>) {
        psiFile.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                when (element) {
                    is PsiJavaCodeReferenceElement -> {
                        val resolved = element.resolve()
                        if (resolved is PsiClass) {
                            val qualifiedName = resolved.qualifiedName ?: return
                            if (!shouldSkipPackage(qualifiedName)) {
                                directReferences.add(qualifiedName)
                            }
                        }
                    }
                    is PsiImportStatement -> {
                        val importedClass = element.resolve()
                        if (importedClass is PsiClass) {
                            val qualifiedName = importedClass.qualifiedName ?: return
                            if (!shouldSkipPackage(qualifiedName)) {
                                directReferences.add(qualifiedName)
                            }
                        }
                    }
                    is KtImportDirective -> {
                        val importPath = element.importPath?.pathStr ?: return
                        if (!shouldSkipPackage(importPath)) {
                            directReferences.add(importPath)
                        }
                    }
                }
            }
        })
    }

    private fun processFileRecursively(
        psiFile: PsiFile,
        result: MutableList<RelatedFile>,
        processedFiles: MutableSet<String>,
        directReferences: MutableSet<String>,
        indirectReferences: MutableSet<String>,
        currentDepth: Int,
        maxDepth: Int,
        maxFiles: Int,
        decompiledFilesCount: AtomicInteger
    ) {
        if (currentDepth > maxDepth || result.size >= maxFiles) {
            return
        }
        val referencesToProcess = mutableSetOf<PsiFile>()
        val currentFileReferences = mutableSetOf<String>()

        // Only collect references if we're below the max depth
        if (currentDepth < maxDepth) {
            psiFile.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (result.size >= maxFiles) return
                    super.visitElement(element)
                    when (element) {
                        is PsiJavaCodeReferenceElement -> {
                            collectReference(element, currentFileReferences, referencesToProcess, directReferences, indirectReferences, currentDepth)
                        }
                        is PsiImportStatement -> {
                            collectImport(element, currentFileReferences, referencesToProcess, directReferences, indirectReferences, currentDepth)
                        }
                        is KtImportDirective -> {
                            collectKotlinImport(element, currentFileReferences, referencesToProcess, directReferences, indirectReferences, currentDepth)
                        }
                    }
                }

                private fun collectReference(
                    reference: PsiJavaCodeReferenceElement,
                    currentRefs: MutableSet<String>,
                    refsToProcess: MutableSet<PsiFile>,
                    directRefs: MutableSet<String>,
                    indirectRefs: MutableSet<String>,
                    depth: Int
                ) {
                    val resolved = reference.resolve()
                    if (resolved is PsiClass) {
                        val qualifiedName = resolved.qualifiedName ?: return
                        if (shouldSkipPackage(qualifiedName)) {
                            return
                        }
                        if (!currentRefs.contains(qualifiedName)) {
                            currentRefs.add(qualifiedName)
                            if (depth > 0 && !directRefs.contains(qualifiedName)) {
                                indirectRefs.add(qualifiedName)
                            }
                            if (directRefs.contains(qualifiedName) || (depth <= 1 && indirectRefs.size <= settings.state.maxRelatedFiles)) {
                                val containingFile = resolved.containingFile
                                if (containingFile != null && containingFile.virtualFile != null && !processedFiles.contains(containingFile.virtualFile.path)) {
                                    refsToProcess.add(containingFile)
                                }
                                if (settings.state.includeImplementations && resolved.isInterface) {
                                    findImplementations(resolved).forEach { implClass ->
                                        val implFile = implClass.containingFile
                                        if (implFile != null && implFile.virtualFile != null && !processedFiles.contains(implFile.virtualFile.path)) {
                                            refsToProcess.add(implFile)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                private fun collectImport(
                    importStmt: PsiImportStatement,
                    currentRefs: MutableSet<String>,
                    refsToProcess: MutableSet<PsiFile>,
                    directRefs: MutableSet<String>,
                    indirectRefs: MutableSet<String>,
                    depth: Int
                ) {
                    val importedClass = importStmt.resolve()
                    if (importedClass is PsiClass) {
                        val qualifiedName = importedClass.qualifiedName ?: return
                        if (shouldSkipPackage(qualifiedName)) {
                            return
                        }
                        if (!currentRefs.contains(qualifiedName)) {
                            currentRefs.add(qualifiedName)
                            if (depth > 0 && !directRefs.contains(qualifiedName)) {
                                indirectRefs.add(qualifiedName)
                            }
                            if (directRefs.contains(qualifiedName) || (depth <= 1 && indirectRefs.size <= settings.state.maxRelatedFiles)) {
                                val containingFile = importedClass.containingFile
                                if (containingFile != null && containingFile.virtualFile != null && !processedFiles.contains(containingFile.virtualFile.path)) {
                                    refsToProcess.add(containingFile)
                                }
                                if (settings.state.includeImplementations && importedClass.isInterface) {
                                    findImplementations(importedClass).forEach { implClass ->
                                        val implFile = implClass.containingFile
                                        if (implFile != null && implFile.virtualFile != null && !processedFiles.contains(implFile.virtualFile.path)) {
                                            refsToProcess.add(implFile)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                private fun collectKotlinImport(
                    importDirective: KtImportDirective,
                    currentRefs: MutableSet<String>,
                    refsToProcess: MutableSet<PsiFile>,
                    directRefs: MutableSet<String>,
                    indirectRefs: MutableSet<String>,
                    depth: Int
                ) {
                    val importPath = importDirective.importPath?.pathStr ?: return
                    if (shouldSkipPackage(importPath)) {
                        return
                    }
                    if (!currentRefs.contains(importPath)) {
                        currentRefs.add(importPath)
                        if (depth > 0 && !directRefs.contains(importPath)) {
                            indirectRefs.add(importPath)
                        }
                        if (directRefs.contains(importPath) || (depth <= 1 && indirectRefs.size <= settings.state.maxRelatedFiles)) {
                            val importedClass = resolveKotlinImport(importPath)
                            if (importedClass != null) {
                                val containingFile = importedClass.containingFile
                                if (containingFile != null && containingFile.virtualFile != null && !processedFiles.contains(containingFile.virtualFile.path)) {
                                    refsToProcess.add(containingFile)
                                }
                                if (settings.state.includeImplementations && importedClass.isInterface) {
                                    findImplementations(importedClass).forEach { implClass ->
                                        val implFile = implClass.containingFile
                                        if (implFile != null && implFile.virtualFile != null && !processedFiles.contains(implFile.virtualFile.path)) {
                                            refsToProcess.add(implFile)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            })
        }

        for (referencedFile in referencesToProcess) {
            val virtualFile = referencedFile.virtualFile ?: continue
            if (processedFiles.add(virtualFile.path)) {
                val isInContent = fileIndex.isInContent(virtualFile)
                val isClsFile = referencedFile is ClsFileImpl
                if (isInContent || (settings.state.includeDependencies && (!isClsFile || (settings.state.includeDecompiled && decompiledFilesCount.get() < settings.state.maxDecompiledFiles)))) {
                    if (isClsFile) {
                        decompiledFilesCount.incrementAndGet()
                    }
                    try {
                        var content = if (settings.state.smartPruning) {
                            extractRelevantParts(referencedFile)
                        } else {
                            String(virtualFile.contentsToByteArray())
                        }
                        if (settings.state.trimWhitespace) {
                            content = FileUtils.trimWhitespace(content)
                        }
                        result.add(
                            RelatedFile(
                                path = virtualFile.path,
                                content = content,
                                isDecompiled = isClsFile,
                                isImplementation = false
                            )
                        )
                        processFileRecursively(
                            referencedFile,
                            result,
                            processedFiles,
                            directReferences,
                            indirectReferences,
                            currentDepth + 1,
                            maxDepth,
                            maxFiles,
                            decompiledFilesCount
                        )
                    } catch (e: Exception) {
                        LOG.error("Error processing related file: ${virtualFile.path}", e)
                    }
                }
            }
        }

        if (settings.state.includeDecompiled && decompiledFilesCount.get() < settings.state.maxDecompiledFiles) {
            // We would process decompiled classes here, but for simplicity
            // we'll skip the detailed implementation
        }
    }

    private fun shouldSkipPackage(qualifiedName: String): Boolean {
        if (!settings.state.skipJavaPackages) {
            return false
        }
        for (excludedPackage in settings.state.excludedPackages) {
            if (excludedPackage.isNotEmpty() && qualifiedName.startsWith(excludedPackage)) {
                return true
            }
        }
        return false
    }

    private fun findImplementations(interfaceClass: PsiClass): List<PsiClass> {
        val implementations = mutableListOf<PsiClass>()
        val searchScope = if (settings.state.includeDependencies) {
            GlobalSearchScope.allScope(project)
        } else {
            GlobalSearchScope.projectScope(project)
        }
        val inheritors = ClassInheritorsSearch.search(interfaceClass, searchScope, true)
        for (inheritor in inheritors) {
            val qualifiedName = inheritor.qualifiedName
            if (qualifiedName != null && !shouldSkipPackage(qualifiedName)) {
                implementations.add(inheritor)
            }
        }
        return implementations
    }

    private fun resolveKotlinImport(importPath: String): PsiClass? {
        val psiFacade = JavaPsiFacade.getInstance(project)
        val searchScope = GlobalSearchScope.allScope(project)
        return psiFacade.findClass(importPath, searchScope)
    }

    private fun extractRelevantParts(psiFile: PsiFile): String {
        val sb = StringBuilder()
        when (psiFile) {
            is PsiJavaFile -> {
                val packageStatement = psiFile.packageStatement
                if (packageStatement != null) {
                    sb.append(packageStatement.text).append("\n\n")
                }
                if (!settings.state.excludeImportsInRelatedFiles) {
                    val importList = psiFile.importList
                    if (importList != null) {
                        sb.append(importList.text).append("\n\n")
                    }
                }
                for (psiClass in psiFile.classes) {
                    if (settings.state.includeJavadoc) {
                        val javadoc = psiClass.docComment
                        if (javadoc != null) {
                            sb.append(javadoc.text).append("\n")
                        }
                    }
                    sb.append(psiClass.text).append("\n")
                }
            }
            is KtFile -> {
                val packageDirective = psiFile.packageDirective
                if (packageDirective != null) {
                    sb.append(packageDirective.text).append("\n\n")
                }
                if (!settings.state.excludeImportsInRelatedFiles) {
                    val importList = psiFile.importList
                    if (importList != null) {
                        for (importDirective in importList.imports) {
                            sb.append(importDirective.text).append("\n")
                        }
                        sb.append("\n")
                    }
                }
                for (declaration in psiFile.declarations) {
                    sb.append(declaration.text).append("\n")
                }
            }
            is ClsFileImpl -> {
                try {
                    ApplicationManager.getApplication().runReadAction {
                        val mirror = psiFile.mirror
                        if (mirror != null) {
                            sb.append(mirror.text)
                        } else {
                            sb.append("// Unable to decompile file: mirror is null")
                        }
                    }
                } catch (e: Exception) {
                    LOG.error("Error decompiling file", e)
                    sb.append("// Error decompiling file: ${e.message}")
                }
            }
            else -> {
                sb.append(psiFile.text)
            }
        }
        return sb.toString()
    }
}