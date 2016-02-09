/*
 * Copyright 2016 Carlos Ballesteros Velasco
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

package com.jtransc.vfs

import com.jtransc.env.OS
import com.jtransc.error.InvalidArgumentException
import com.jtransc.error.InvalidOperationException
import com.jtransc.error.NotImplementedException
import com.jtransc.text.ToString
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

data class SyncVfsStat(val file: SyncVfsFile, val size: Long, val mtime: Date, val isDirectory: Boolean, val exists: Boolean) {
	val name: String get() = file.name
	val path: String get() = file.path
	val isFile: Boolean get() = !isDirectory
}

class SyncVfsFile(internal val vfs: SyncVfs, public val path: String) {
	val size: Long get() = stat().size
	val mtime: Date get() = stat().mtime
	fun setMtime(time: Date) = vfs.setMtime(path, time)
	fun stat(): SyncVfsStat = vfs.stat(path)
	fun read(): ByteBuffer = vfs.read(path)
	fun readBytes(): ByteArray = read().getBytes()
	fun readOrNull(): ByteBuffer? = if (exists) read() else null
	inline fun <reified T : Any> readSpecial(): T = readSpecial(T::class.java)
	fun <T> readSpecial(clazz: Class<T>): T = vfs.readSpecial(clazz, path)
	fun write(data: ByteBuffer): Unit = vfs.write(path, data)
	fun write(data: ByteArray): Unit = this.write(data.toBuffer())
	fun readString(encoding: Charset = Charsets.UTF_8): String = encoding.toString(vfs.read(path))
	val exists: Boolean get() = vfs.exists(path)
	val isDirectory: Boolean get() = stat().isDirectory
	fun remove(): Unit = vfs.remove(path)
	fun removeIfExists(): Unit = if (exists) remove() else {
	}

	fun exec(cmd: String, args: List<String>, options: ExecOptions): ProcessResult = vfs.exec(path, cmd, args, options)
	fun exec(cmd: String, args: List<String>): ProcessResult = exec(cmd, args, ExecOptions(passthru = false))
	fun exec(cmd: String, vararg args: String): ProcessResult = exec(cmd, args.toList(), ExecOptions(passthru = false))
	fun passthru(cmd: String, args: List<String>, filter: ((line: String) -> Boolean)? = null): ProcessResult = exec(cmd, args, ExecOptions(passthru = true, filter = filter))
	fun passthru(cmd: String, vararg args: String, filter: ((line: String) -> Boolean)? = null): ProcessResult = exec(cmd, args.toList(), ExecOptions(passthru = true, filter = filter))
	val name: String get() = path.substringAfterLast('/')
	val realpath: String get() = jailCombinePath(vfs.absolutePath, path)
	val realpathOS: String get() = if (OS.isWindows) realpath.replace('/', '\\') else realpath
	val realfile: File get() = File(realpath)
	fun listdir(): Iterable<SyncVfsStat> = vfs.listdir(path)
	fun listdirRecursive(): Iterable<SyncVfsStat> = listdirRecursive({ true })
	fun listdirRecursive(filter: (stat: SyncVfsStat) -> Boolean): Iterable<SyncVfsStat> {
		//log("REALPATH: ${this.realpath}")
		return listdir().flatMap {
			//log("item:${it.path}")
			if (filter(it)) {
				if (it.isDirectory) {
					//log("directory! ${it.path}")
					it.file.listdirRecursive()
				} else {
					//log("file! ${it.path}")
					listOf(it)
				}
			} else {
				listOf()
			}
		}
	}

	fun mkdir(): Unit = vfs.mkdir(path)
	fun rmdir(): Unit = vfs.rmdir(path)
	fun ensuredir(): SyncVfsFile {
		if (path == "") return this
		if (!parent.exists) parent.ensuredir()
		mkdir()
		return this
	}

	fun ensureParentDir(): SyncVfsFile {
		parent.ensuredir()
		return this
	}

	fun rmdirRecursively(): Unit {
		for (item in this.listdir()) {
			if (item.isDirectory) {
				item.file.rmdirRecursively()
			} else {
				item.file.remove()
			}
		}
		this.rmdir()
	}

	fun rmdirRecursivelyIfExists(): Unit {
		if (exists) return rmdirRecursively();
	}

	fun access(path: String): SyncVfsFile = SyncVfsFile(vfs, combinePaths(this.path, path));

	operator fun get(path: String): SyncVfsFile = access(path)
	operator fun set(path: String, content: String) = access(path).ensureParentDir().write(content)
	operator fun set(path: String, content: ToString) = access(path).ensureParentDir().write(content.toString())
	operator fun set(path: String, content: ByteBuffer) = access(path).ensureParentDir().write(content)
	operator fun set(path: String, content: ByteArray) = access(path).ensureParentDir().write(content.toBuffer())
	operator fun contains(path: String): Boolean = access(path).exists

	fun jailAccess(path: String): SyncVfsFile = access(path).jail()
	fun jail(): SyncVfsFile = AccessSyncVfs(vfs, path).root();
	override fun toString() = "SyncVfsFile($vfs, '$path')"

	fun write(data: String, encoding: Charset = UTF8): SyncVfsFile = writeString(data, encoding)

	fun writeString(data: String, encoding: Charset = UTF8): SyncVfsFile {
		write(data.toBuffer(encoding))
		return this
	}

	fun dumpTree() {
		println("<dump>")
		for (path in listdirRecursive()) {
			println(path)
		}
		println("</dump>")
	}

	fun copyTo(that: SyncVfsFile): Unit = that.ensureParentDir().write(this.read())

	fun copyTreeTo(that: SyncVfsFile): Unit {
		println("copyTreeTo " + this.realpath + " -> " + that.realpath)
		val stat = this.stat()
		if (stat.isDirectory) {
			that.mkdir()
			for (node in this.listdir()) {
				node.file.copyTreeTo(that[node.name])
			}
		} else {
			this.copyTo(that)
		}
	}


	fun toDumpString(): String {
		return listdirRecursive().filter { it.isFile }.map { "// ${it.path}:\n${it.file.readString()}" }.joinToString("\n")
	}
}

data class ExecOptions(val passthru: Boolean = false, val filter: ((line: String) -> Boolean)? = null)

open class SyncVfs {
	final fun root() = SyncVfsFile(this, "")

	open val absolutePath: String = ""
	open fun read(path: String): ByteBuffer {
		throw NotImplementedException()
	}

	open fun <T> readSpecial(clazz: Class<T>, path: String): T {
		throw NotImplementedException()
	}

	open fun write(path: String, data: ByteBuffer): Unit {
		throw NotImplementedException()
	}

	open fun listdir(path: String): Iterable<SyncVfsStat> {
		throw NotImplementedException()
	}

	open fun mkdir(path: String): Unit {
		throw NotImplementedException()
	}

	open fun rmdir(path: String): Unit {
		throw NotImplementedException()
	}

	open fun exec(path: String, cmd: String, args: List<String>, options: ExecOptions): ProcessResult {
		throw NotImplementedException()
	}

	open fun exists(path: String): Boolean {
		try {
			return stat(path).exists
		} catch (e: Throwable) {
			return false
		}
	}

	open fun remove(path: String): Unit {
		throw NotImplementedException()
	}

	open fun stat(path: String): SyncVfsStat {
		throw NotImplementedException()
	}

	open fun setMtime(path: String, time: Date) {
		throw NotImplementedException()
	}
}

fun FileNode.toSyncStat(vfs: SyncVfs, path: String): SyncVfsStat {
	return SyncVfsStat(SyncVfsFile(vfs, path), this.size(), this.mtime(), this.isDirectory(), true)
}

private class _MemoryVfs : SyncVfs() {
	val tree = FileNodeTree()
	private val _root = tree.root

	override val absolutePath: String get() = ""
	override fun read(path: String): ByteBuffer {
		return _root.access(path).io?.read()!!
	}

	override fun write(path: String, data: ByteBuffer): Unit {
		val item = _root.access(path, true)
		var writtenData = data
		var writtenTime = Date()
		item.type = FileNodeType.FILE
		item.io = object : FileNodeIO {
			override fun mtime(): Date = writtenTime
			override fun read(): ByteBuffer = writtenData
			override fun write(data: ByteBuffer) {
				writtenTime = Date()
				writtenData = data
			}

			override fun size(): Long = writtenData.length().toLong()

		}
	}

	override fun listdir(path: String): Iterable<SyncVfsStat> {
		return _root.access(path).map {
			it.toSyncStat(this, "${path}/${it.name}")
		}
	}

	override fun mkdir(path: String): Unit {
		_root.access(path, true)
	}

	override fun rmdir(path: String): Unit {
		try {
			val node = _root.access(path, false)
			node.remove()
		} catch (e: Throwable) {

		}
	}

	override fun exists(path: String): Boolean {
		try {
			_root.access(path)
			return true
		} catch(e: Throwable) {
			return false
		}
	}

	override fun remove(path: String): Unit = _root.access(path).remove()
	override fun stat(path: String): SyncVfsStat = _root.access(path).toSyncStat(this, path)

	override fun setMtime(path: String, time: Date) {
		// @TODO!
	}
}

private class _LocalVfs : SyncVfs() {
	override val absolutePath: String get() = ""
	override fun read(path: String): ByteBuffer = RawIo.fileRead(path)
	override fun write(path: String, data: ByteBuffer): Unit = RawIo.fileWrite(path, data)
	override fun listdir(path: String): Iterable<SyncVfsStat> = RawIo.listdir(path).map { it.toSyncStat(this, "$path/${it.name}") }
	override fun mkdir(path: String): Unit = RawIo.mkdir(path)
	override fun rmdir(path: String): Unit = RawIo.rmdir(path)
	override fun exec(path: String, cmd: String, args: List<String>, options: ExecOptions): ProcessResult = RawIo.execOrPassthruSync(path, cmd, args, options)
	override fun exists(path: String): Boolean = RawIo.fileExists(path)
	override fun remove(path: String): Unit = RawIo.fileRemove(path)
	override fun stat(path: String): SyncVfsStat = RawIo.fileStat(path).toSyncStat(this, path)
	override fun setMtime(path: String, time: Date) = RawIo.setMtime(path, time)
}

fun File.toSyncStat(vfs: SyncVfs, path: String) = SyncVfsStat(SyncVfsFile(vfs, path), this.length(), Date(this.lastModified()), this.isDirectory, true)

abstract class ProxySyncVfs : SyncVfs() {
	abstract protected fun transform(path: String): SyncVfsFile
	open protected fun transformStat(stat: SyncVfsStat): SyncVfsStat = stat

	override val absolutePath: String get() = transform("").realpath
	override fun read(path: String): ByteBuffer = transform(path).read()
	override fun <T> readSpecial(clazz: Class<T>, path: String): T = transform(path).readSpecial(clazz)
	override fun write(path: String, data: ByteBuffer): Unit = transform(path).write(data)
	// @TODO: Probably transform SyncVfsStat!
	override fun listdir(path: String): Iterable<SyncVfsStat> = transform(path).listdir().map { transformStat(it) }

	override fun mkdir(path: String): Unit {
		transform(path).mkdir()
	}

	override fun rmdir(path: String): Unit {
		transform(path).rmdir()
	}

	override fun exec(path: String, cmd: String, args: List<String>, options: ExecOptions): ProcessResult = transform(path).exec(cmd, args, options)
	override fun exists(path: String): Boolean = transform(path).exists
	override fun remove(path: String): Unit = transform(path).remove()
	override fun stat(path: String): SyncVfsStat = transformStat(transform(path).stat())
	override fun setMtime(path: String, time: Date) = transform(path).setMtime(time)
}

// @TODO: paths should not start with "/"
private class AccessSyncVfs(val parent: SyncVfs, val path: String) : ProxySyncVfs() {
	override protected fun transform(path: String): SyncVfsFile = SyncVfsFile(parent, jailCombinePath(this.path, path))
	override protected fun transformStat(stat: SyncVfsStat): SyncVfsStat {
		// @TODO: Do this better!
		val statFilePath = "/" + stat.file.path.trimStart('/')
		val thisPath = "/" + this.path.trimStart('/')
		if (!statFilePath.startsWith(thisPath)) {
			throw InvalidOperationException("Assertion failed $statFilePath must start with $thisPath")
		}

		return SyncVfsStat(SyncVfsFile(this, "/" + statFilePath.removePrefix(thisPath)), stat.size, stat.mtime, stat.isDirectory, true)
	}
}

private class _LogSyncVfs(val parent: SyncVfs) : ProxySyncVfs() {
	override protected fun transform(path: String): SyncVfsFile = SyncVfsFile(parent, path)

	override fun write(path: String, data: ByteBuffer): Unit {
		println("Writting $parent($path) with ${data.toString(UTF8)}")
		super.write(path, data)
	}
}

fun RootLocalVfs(): SyncVfsFile = _LocalVfs().root()
fun MergeVfs(nodes: List<SyncVfsFile>): SyncVfsFile = MergedSyncVfs(nodes).root()
fun ZipVfs(path: String): SyncVfsFile = ZipSyncVfs(ZipFile(path)).root()
fun LocalVfs(path: String): SyncVfsFile = _LocalVfs().root().access(path).jail()
fun CwdVfs(): SyncVfsFile = LocalVfs(RawIo.cwd())
fun CwdVfs(path: String): SyncVfsFile = CwdVfs().jailAccess(path)
fun ScriptVfs(): SyncVfsFile = LocalVfs(RawIo.script())
fun MemoryVfs(): SyncVfsFile = _MemoryVfs().root()
fun MemoryVfs(vararg files: Pair<String, String>): SyncVfsFile {
	val vfs = _MemoryVfs().root()
	for (file in files) {
		vfs.access(file.first).ensureParentDir().writeString(file.second)
	}
	return vfs
}

fun MemoryVfsBin(vararg files: Pair<String, ByteArray>): SyncVfsFile {
	val vfs = _MemoryVfs().root()
	for (file in files) {
		vfs.access(file.first).ensureParentDir().write(file.second.toBuffer())
	}
	return vfs
}

fun MemoryVfsFile(content: String, name: String = "file"): SyncVfsFile {
	return MemoryVfs(name to content).access(name)
}

fun MemoryVfsFileBin(content: ByteArray, name: String = "file"): SyncVfsFile {
	return MemoryVfsBin(name to content).access(name)
}

fun LogVfs(parent: SyncVfsFile): SyncVfsFile = _LogSyncVfs(parent.jail().vfs).root()
fun SyncVfsFile.log() = LogVfs(this)


public fun normalizePath(path: String): String {
	val out = ArrayList<String>();
	for (chunk in path.replace('\\', '/').split('/')) {
		when (chunk) {
			".." -> if (out.size > 0) out.removeAt(0)
			"." -> {
			}
			"" -> {
				if (out.size == 0) out.add("")
			}
			else -> out.add(chunk)
		}
	}
	return out.joinToString("/")
}

public fun combinePaths(vararg paths: String): String {
	return normalizePath(paths.filter { it != "" }.joinToString("/"))
}

public fun jailCombinePath(base: String, access: String): String {
	return combinePaths(base, normalizePath(access))
}

class VfsPath(val path: String)


open class FileNodeTree {
	val root = FileNode(this, null, "", FileNodeType.ROOT)
}

enum class FileNodeType { ROOT, DIRECTORY, FILE }

interface FileNodeIO {
	fun read(): ByteBuffer
	fun write(data: ByteBuffer): Unit
	fun size(): Long
	fun mtime(): Date
}

data class UserKey<T>(val name: String) {}

class UserData {
	private val dict = hashMapOf<Any, Any>()

	fun <T : Any> get(key: UserKey<T>): T = dict[key] as T
	fun <T : Any> set(key: UserKey<T>, value: T) {
		dict.put(key, value)
	}
}

open class FileNode(val tree: FileNodeTree, val parent: FileNode?, val name: String, var type: FileNodeType) : Iterable<FileNode> {
	private val children = arrayListOf<FileNode>()
	val userData = UserData()
	var io: FileNodeIO? = null

	override fun toString(): String = "FileNode($path, type=$type leaf=$isLeaf)"

	val path: String get() = if (parent != null) "${parent.path}/$name" else name
	val root: FileNode get() = if (parent != null) parent.root else this
	val isLeaf: Boolean get() = children.size == 0

	init {
		if (parent != null) parent.children.add(this)
	}

	fun size(): Long = io?.size() ?: 0
	fun mtime(): Date = io?.mtime() ?: Date()
	fun isDirectory(): Boolean = (type == FileNodeType.ROOT) || (type == FileNodeType.DIRECTORY)

	fun getChildren(): List<FileNode> = children

	override fun iterator(): Iterator<FileNode> {
		return children.iterator()
	}

	fun leafs(): Iterable<FileNode> {
		return descendants().filter { it.isLeaf }
	}

	fun descendants(): Iterable<FileNode> {
		return children + children.flatMap { it.descendants() }
	}

	fun child(name: String): FileNode? {
		return when (name) {
			"" -> this
			"." -> this
			".." -> parent
			else -> children.firstOrNull { it.name == name }
		}
	}

	open protected fun _createChild(name: String, type: FileNodeType): FileNode {
		return FileNode(tree, this, name, type)
	}

	fun createChild(name: String, type: FileNodeType): FileNode {
		if (child(name) != null) throw FileAlreadyExistsException(File(name), reason = "Child $name already exists")
		return _createChild(name, type)
	}

	fun access(path: String, mustCreate: Boolean = false): FileNode {
		var node: FileNode = this
		var first = true

		for (name in path.split("/")) {
			var child = if (name == "" && first) root else node.child(name)
			if (child == null && mustCreate) {
				child = FileNode(tree, node, name, FileNodeType.DIRECTORY)
			}
			if (child == null) throw NoSuchFileException(File(path), reason = "Can't access '$path' (missing '$name')")
			node = child
			first = false
		}

		return node
	}

	fun remove() {
		if (parent != null) {
			parent.children.remove(this)
		}
	}
}

object Path {
	fun parent(path: String): String = if (path.contains('/')) path.substringBeforeLast('/') else ""
	fun withoutExtension(path: String): String = path.substringBeforeLast('.')
	fun withExtension(path: String, ext: String): String = withoutExtension(path) + ".$ext"
	fun withBaseName(path: String, name: String): String = "${parent(path)}/$name"
}

val SyncVfsFile.parent: SyncVfsFile get() {
	//println("Path: '${path}', Parent: '${Path.parent(path)}'")
	return SyncVfsFile(vfs, Path.parent(path))
}
val SyncVfsFile.withoutExtension: SyncVfsFile get() = SyncVfsFile(vfs, Path.withoutExtension(path))
fun SyncVfsFile.withExtension(ext: String): SyncVfsFile = SyncVfsFile(vfs, Path.withExtension(path, ext))
fun SyncVfsFile.withBaseName(baseName: String): SyncVfsFile = parent.access(baseName)

private class MergedSyncVfs(private val nodes: List<SyncVfsFile>) : SyncVfs() {
	init {
		if (nodes.isEmpty()) throw InvalidArgumentException("Nodes can't be empty")
	}

	override val absolutePath: String = "#merged#"

	private fun <T> op(path: String, act: String, action: (node: SyncVfsFile) -> T): T {
		var lastError: Throwable? = null
		for (node in nodes) {
			try {
				return action(node)
			} catch(t: Throwable) {
				lastError = t
			}
		}
		throw RuntimeException("Can't $act file '$path'")
	}

	override fun read(path: String): ByteBuffer = op(path, "read") { it[path].read() }
	override fun write(path: String, data: ByteBuffer) = op(path, "write") { it[path].write(data) }
	override fun <T> readSpecial(clazz: Class<T>, path: String): T = op(path, "readSpecial") { it[path].readSpecial(clazz) }
	override fun listdir(path: String): Iterable<SyncVfsStat> = op(path, "listdir") { it[path].listdir() }
	override fun mkdir(path: String) = op(path, "mkdir") { it[path].mkdir() }
	override fun rmdir(path: String) = op(path, "rmdir") { it[path].rmdir() }
	override fun exec(path: String, cmd: String, args: List<String>, options: ExecOptions): ProcessResult {
		return op(path, "exec") { it[path].exec(cmd, args, options) }
	}

	override fun remove(path: String) = op(path, "remove") { it[path].remove() }
	override fun stat(path: String): SyncVfsStat {
		var lastStat: SyncVfsStat? = null
		for (node in nodes) {
			val stat = node[path].stat()
			lastStat = stat
			if (stat.exists) break
			//println(stat)
		}
		return lastStat!!
	}

	override fun setMtime(path: String, time: Date) = op(path, "setMtime") { it[path].setMtime(time) }
}

private class ZipSyncVfs(val zip: ZipFile) : SyncVfs() {
	constructor(path: String) : this(ZipFile(path))

	constructor(file: File) : this(ZipFile(file))

	override val absolutePath: String = "#zip#"

	override fun read(path: String): ByteBuffer {
		val entry = zip.getEntry(path)
		return zip.getInputStream(entry).readBytes().toBuffer()
	}

	override fun listdir(path: String): Iterable<SyncVfsStat> {
		throw NotImplementedException()
	}

	override fun stat(path: String) = try {
		entryToStat(zip.getEntry(path))
	} catch (e: Throwable) {
		SyncVfsStat(
			file = SyncVfsFile(this, path),
			size = 0,
			mtime = Date(0L),
			isDirectory = false,
			exists = false
		)
	}

	private fun entryToStat(entry: ZipEntry) = SyncVfsStat(
		file = SyncVfsFile(this, entry.name),
		size = entry.size,
		mtime = Date(entry.time),
		isDirectory = entry.isDirectory,
		exists = true
	)
}