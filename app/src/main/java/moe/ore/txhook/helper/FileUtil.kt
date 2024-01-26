/*
 * English :
 *  The project is protected by the MPL open source agreement.
 * Open source agreement warning that prohibits deletion of project source code files.
 * The project is prohibited from acting in illegal areas.
 * All illegal activities arising from the use of this project are the responsibility of the second author, and the original author of the project is not responsible
 *
 *  中文：
 *  该项目由MPL开源协议保护。
 *  禁止删除项目源代码文件的开源协议警告内容。
 * 禁止使用该项目在非法领域行事。
 * 使用该项目产生的违法行为，由使用者或第二作者全责，原作者免责
 *
 * 日本语：
 * プロジェクトはMPLオープンソース契約によって保護されています。
 *  オープンソース契約プロジェクトソースコードファイルの削除を禁止する警告。
 * このプロジェクトは違法地域の演技を禁止しています。
 * このプロジェクトの使用から生じるすべての違法行為は、2番目の著者の責任であり、プロジェクトの元の著者は責任を負いません。
 *
 */

package moe.ore.txhook.helper

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.math.BigDecimal

object FileUtil {
    @JvmStatic
    fun fileSizeToString(size: Long): String {
        val kiloByte = size / 1024
        if (kiloByte < 1) {
            return size.toString() + "b"
        }
        val megaByte = kiloByte / 1024
        if (megaByte < 1) {
            val result1 = BigDecimal(kiloByte.toString())
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString().toString() + "kb"
        }
        val gigaByte = megaByte / 1024
        if (gigaByte < 1) {
            val result2 = BigDecimal(megaByte.toString())
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString().toString() + "mb"
        }
        val teraBytes = gigaByte / 1024
        if (teraBytes < 1) {
            val result3 = BigDecimal(gigaByte.toString())
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString().toString() + "gb"
        }
        val result4 = BigDecimal(teraBytes)
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString().toString() + "tb"
    }

    @JvmStatic
    fun has(filePath: String) = File(filePath).exists()

    fun delete(filePath: String) = File(filePath).delete()

    @JvmStatic
    fun readFileBytes(f: File): ByteArray = f.readBytes()

    @JvmStatic
    fun readFileBytes(filePath: String) = readFileBytes(File(filePath).also { f ->
        check(f.exists()) { "file not exits" }
        check(f.isFile) { "File is not a file" }
        check(f.canRead()) { "file can not read" }
    })

    @JvmStatic
    fun readFileString(f: File) = String(readFileBytes(f))

    @JvmStatic
    fun readFileString(path: String) = String(readFileBytes(path))

    @JvmStatic
    fun saveFile(path: String, content: String) = saveFile(path, content.toByteArray())

    @JvmStatic
    fun saveFile(path: String, content: ByteArray) {
        val file = File(path)
        if (!file.exists()) {
            file.parentFile?.let {
                if (!it.exists()) it.mkdirs()
            }
            file.createNewFile()
        }
        file.writeBytes(content)
    }

    @JvmStatic
    fun saveFile(path: String, content: InputStream) {
        val file = File(path)
        if (!file.exists()) {
            file.parentFile?.let {
                if (!it.exists()) it.mkdirs()
            }
            file.createNewFile()
        }
        FileOutputStream(file).use { out ->
            content.use {
                var len: Int
                val bytes = ByteArray(1024)
                while (true) {
                    len = it.read(bytes)
                    if (len != -1) {
                        out.write(bytes, 0, len)
                    } else {
                        break
                    }
                }
                out.flush()
            }
        }
    }

    @JvmStatic
    private fun checkFileExists(file: File, block: File.(Boolean) -> Unit) =
        block.invoke(file, file.exists())
}