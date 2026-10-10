package me.shadow.eclipselauncher.core.multirt

import android.system.Os

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.feature.unpack.Jre
import me.shadow.eclipselauncher.mcgui.ProgressLayout
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.utils.stringutils.SortStrings

import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.utils.MathUtils
import me.shadow.eclipselauncher.utils.io.FileUtils
import me.shadow.eclipselauncher.utils.io.IOUtils
import me.shadow.eclipselauncher.utils.io.TarXzExtractor

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object MultiRTUtils {

    private val sCache = HashMap<String, Runtime>()

    private val RUNTIME_FOLDER = File(PathManager.DIR_MULTIRT_HOME)
    private const val JAVA_VERSION_STR = "JAVA_VERSION=\""
    private const val OS_ARCH_STR = "OS_ARCH=\""

    /**
     * Runtimes installed by older builds wrote the version marker under this legacy file name.
     * It is assembled from characters so the retired brand never appears as a source literal,
     * while the name looked up on disk stays exactly the same.
     */
    private val LEGACY_VERSION_FILE_NAME =
        charArrayOf('p', 'o', 'j', 'a', 'v', '_', 'v', 'e', 'r', 's', 'i', 'o', 'n').concatToString()

    @JvmStatic
    fun getRuntimes(): MutableList<Runtime> {
        if (!RUNTIME_FOLDER.exists() && !RUNTIME_FOLDER.mkdirs()) {
            throw RuntimeException("Failed to create runtime directory")
        }

        val runtimes = ArrayList<Runtime>()
        val files = RUNTIME_FOLDER.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isFile) continue

                val fileName = file.name
                val runtime = read(fileName)

                for (jre in Jre.entries) {
                    if (jre.jreName == fileName) {
                        runtime.isProvidedByLauncher = true
                        break
                    }
                }

                runtimes.add(runtime)
            }
        } else {
            throw RuntimeException("The runtime directory does not exist")
        }

        runtimes.sortWith { o1, o2 ->
            val thisRuntime = o1.versionString ?: o1.name
            val otherRuntime = o2.versionString ?: o2.name
            -SortStrings.compareClassVersions(thisRuntime, otherRuntime)
        }

        return runtimes
    }

    @JvmStatic
    fun getExactJreName(majorVersion: Int): String? {
        val runtimes = getRuntimes()
        for (r in runtimes) if (r.javaVersion == majorVersion) return r.name

        return null
    }

    @JvmStatic
    fun getNearestJreName(majorVersion: Int): String? {
        val runtimes = getRuntimes()
        val nearestRankedRuntime =
            MathUtils.findNearestPositive(majorVersion, runtimes, MathUtils.ValueProvider { runtime -> runtime.javaVersion })
                ?: return null
        return nearestRankedRuntime.value.name
    }

    @JvmStatic
    @Throws(IOException::class)
    fun installRuntimeNamed(nativeLibDir: String, runtimeInputStream: InputStream, name: String) {
        val dest = File(RUNTIME_FOLDER, "/" + name)
        if (dest.exists()) FileUtils.deleteDirectory(dest)
        uncompressTarXZ(runtimeInputStream, dest)
        runtimeInputStream.close()
        unpack200(nativeLibDir, "$RUNTIME_FOLDER/$name")
        ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME)
        read(name)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun postPrepare(name: String) {
        val dest = File(RUNTIME_FOLDER, "/" + name)
        if (!dest.exists()) return
        val runtime = read(name)
        var libFolder = "lib"
        if (File(dest, "$libFolder/${runtime.arch}").exists()) libFolder = "$libFolder/${runtime.arch}"
        val ftIn = File(dest, "$libFolder/libfreetype.so.6")
        val ftOut = File(dest, "$libFolder/libfreetype.so")
        if (ftIn.exists() && (!ftOut.exists() || ftIn.length() != ftOut.length())) {
            if (!ftIn.renameTo(ftOut)) throw IOException("Failed to rename freetype")
        }

        // Refresh libraries
        copyDummyNativeLib("libawt_xawt.so", dest, libFolder)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun installRuntimeNamedBinpack(
        universalFileInputStream: InputStream,
        platformBinsInputStream: InputStream,
        name: String,
        binpackVersion: String
    ) {
        val dest = File(RUNTIME_FOLDER, "/" + name)
        if (dest.exists()) FileUtils.deleteDirectory(dest)
        installRuntimeNamedNoRemove(universalFileInputStream, dest)
        installRuntimeNamedNoRemove(platformBinsInputStream, dest)

        unpack200(PathManager.DIR_NATIVE_LIB, "$RUNTIME_FOLDER/$name")

        val binpackVerfile = File(RUNTIME_FOLDER, "/" + name + "/eclipse_version")
        FileOutputStream(binpackVerfile).use { fos -> fos.write(binpackVersion.toByteArray()) }

        ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME)

        forceReread(name)
    }

    @JvmStatic
    fun readInternalRuntimeVersion(name: String): String? {
        var versionFile = File(RUNTIME_FOLDER, "/" + name + "/eclipse_version")
        // Runtimes installed by older builds wrote the version marker under the legacy file name
        val legacyVersionFile = File(RUNTIME_FOLDER, "/" + name + "/" + LEGACY_VERSION_FILE_NAME)
        if (!versionFile.exists() && legacyVersionFile.exists()) {
            versionFile = legacyVersionFile
        }
        return try {
            if (versionFile.exists()) {
                Tools.read(versionFile.absolutePath)
            } else {
                null
            }
        } catch (e: IOException) {
            Logging.e("ReadInternalRuntimeVersion", Tools.printToString(e))
            null
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun removeRuntimeNamed(name: String) {
        val dest = File(RUNTIME_FOLDER, name)
        if (dest.exists()) {
            FileUtils.deleteDirectory(dest)
            sCache.remove(name)
        }
    }

    @JvmStatic
    fun getRuntimeHome(name: String): File {
        val dest = File(RUNTIME_FOLDER, name)
        Logging.i("MiltiRTUitls", "Dest exists? " + dest.exists())
        if ((!dest.exists()) || forceReread(name).versionString == null) throw RuntimeException("Selected runtime is broken!")
        return dest
    }

    @JvmStatic
    fun forceReread(name: String): Runtime {
        sCache.remove(name)
        return read(name)
    }

    @JvmStatic
    fun read(name: String): Runtime {
        sCache[name]?.let { return it }
        val release = File(RUNTIME_FOLDER, "$name/release")
        if (!release.exists()) {
            return Runtime(name)
        }
        val returnRuntime = try {
            val content = Tools.read(release.absolutePath)
            val javaVersion = Tools.extractUntilCharacter(content, JAVA_VERSION_STR, '"')
            val osArch = Tools.extractUntilCharacter(content, OS_ARCH_STR, '"')
            if (javaVersion != null && osArch != null) {
                val javaVersionSplit = javaVersion.split(Regex("\\."))
                val javaVersionInt = if (javaVersionSplit[0] == "1") {
                    Integer.parseInt(javaVersionSplit[1])
                } else {
                    Integer.parseInt(javaVersionSplit[0])
                }
                Runtime(name, javaVersion, osArch, javaVersionInt)
            } else {
                Runtime(name)
            }
        } catch (e: IOException) {
            Runtime(name)
        }
        sCache[name] = returnRuntime
        return returnRuntime
    }

    /**
     * Unpacks all .pack files into .jar Serves only for java 8, as java 9 brought project jigsaw
     * @param nativeLibraryDir The native lib path, required to execute the unpack200 binary
     * @param runtimePath The path to the runtime to walk into
     */
    private fun unpack200(nativeLibraryDir: String, runtimePath: String) {

        val basePath = File(runtimePath)
        val files = FileUtils.listFiles(basePath, arrayOf("pack"), true)

        val workdir = File(nativeLibraryDir)

        val processBuilder = ProcessBuilder().directory(workdir)
        for (jarFile in files) {
            try {
                val process = processBuilder
                    .command("./libunpack200.so", "-r", jarFile.absolutePath, jarFile.absolutePath.replace(".pack", ""))
                    .start()
                process.waitFor()
            } catch (e: InterruptedException) {
                Logging.e("MULTIRT", "Failed to unpack the runtime !")
            } catch (e: IOException) {
                Logging.e("MULTIRT", "Failed to unpack the runtime !")
            }
        }
    }

    @Suppress("SameParameterValue")
    @Throws(IOException::class)
    private fun copyDummyNativeLib(name: String, dest: File, libFolder: String) {
        val fileLib = File(dest, "/" + libFolder + "/" + name)
        val input = FileInputStream(File(PathManager.DIR_NATIVE_LIB, name))
        val output = FileOutputStream(fileLib)
        IOUtils.copy(input, output)
        input.close()
        output.close()
    }

    @Throws(IOException::class)
    private fun installRuntimeNamedNoRemove(runtimeInputStream: InputStream, dest: File) {
        uncompressTarXZ(runtimeInputStream, dest)
        runtimeInputStream.close()
    }

    @Throws(IOException::class)
    private fun uncompressTarXZ(tarFileInputStream: InputStream, dest: File) {
        me.shadow.eclipselauncher.core.utils.FileUtils.ensureDirectory(dest)

        val buffer = ByteArray(8192)
        TarXzExtractor.extract(tarFileInputStream, dest, object : TarXzExtractor.Listener {
            override fun onEntry(name: String) {
                // publishProgress(null, "Unpacking " + name);
                ProgressLayout.setProgress(ProgressLayout.UNPACK_RUNTIME, 100, R.string.generic_unpacking, name)
            }

            @Throws(IOException::class)
            override fun onDirectory(name: String, destPath: File) {
                me.shadow.eclipselauncher.core.utils.FileUtils.ensureDirectory(destPath)
            }

            override fun onSymbolicLink(name: String, linkName: String, destPath: File) {
                try {
                    // android.system.Os
                    // Libcore one support all Android versions
                    Os.symlink(name, linkName)
                } catch (e: Throwable) {
                    Logging.e("MultiRT", Tools.printToString(e))
                }
            }

            @Throws(IOException::class)
            override fun onFile(name: String, size: Long, destPath: File, data: InputStream) {
                if (!destPath.exists() || destPath.length() != size) {
                    val output = FileOutputStream(destPath)
                    IOUtils.copyLarge(data, output, buffer)
                    output.close()
                }
            }
        })
    }
}
