package com.crzsc.plugin.listener

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class Image3xListener(private val project: Project) : BulkFileListener {

    override fun after(events: List<VFileEvent>) {
        for (event in events) {
            if (!event.path.startsWith(project.basePath.toString())) continue
            if (event is VFileDeleteEvent) continue
            var file = event.file ?: continue
            if (file.path.split("/").any { it.startsWith(".") }) continue
            if (event is VFileContentChangeEvent) continue
            file = when (event) {
                is VFileCopyEvent -> LocalFileSystem.getInstance().refreshAndFindFileByPath(event.path)
                else -> event.file
            } ?: continue

            val ext = file.extension?.lowercase() ?: continue
            if (ext !in listOf("webp", "png", "jpg", "jpeg")) continue
            // 必须在 3.0x 目录中
            if (!file.path.contains("/3.0x/")) continue

            generateScaledImages(file, ext)
        }
    }

    private fun generateScaledImages(file: VirtualFile, ext: String) {
        val inputFile = File(file.path)
        val name = file.name // logo.webp
        val parentDir = file.parent.path // .../3.0x
        val baseDir = File(parentDir).parentFile // .../assets/image

        val image = ImageIO.read(inputFile) ?: return

        val targets = listOf(
            Triple("2.0x", 2.0 / 3, File(baseDir, "2.0x/$name")),
            Triple("", 1.0 / 3, File(baseDir, name))
        )

        for ((subdir, scale, outFile) in targets) {
            try {
                if (outFile.exists()) {
                    // 不覆盖, 存在就跳过
                    continue
                }
                val width = (image.width * scale).toInt()
                val height = (image.height * scale).toInt()

                val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                val g = scaled.createGraphics()
                g.drawImage(image, 0, 0, width, height, null)
                g.dispose()

                // Ensure directory exists
                outFile.parentFile.mkdirs()

                val format = if (ext == "jpeg") "jpg" else ext
                ImageIO.write(scaled, format, outFile)

                println("✅ 生成 ${outFile.path}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
