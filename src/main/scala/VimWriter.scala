/* sxr -- Scala X-Ray
 * Copyright 2009 Mark Harrah
 */

package sxr

import java.io.File
import FileUtil.{withReader, withWriter}

object VimWriter
{
	val TagsFileName = "tags"
	val VimExtension = ".txt"
}
import VimWriter._
/** Outputs a set of files used by scala_sxr.vim.
 * These consist in: for each source file, a text file listing each token with its offset range,
 * its type, and the optional tag locating its declaration; and a global 'tags' file listing the
 * name, file and offset of each of these tags. */
class VimWriter(outputDirectory: File, encoding: String) extends OutputWriter {

	val info = new OutputInfo(outputDirectory, VimExtension)
	import info._

	val ctags = wrap.Wrappers.treeSet[Ctag]

	def writeStart() {
		// Nothing to do
	}

	def writeUnit(sourceFile: File, relativeSourcePath: String, tokenList: List[Token]) {
		val outputFile = getOutputFile(relativeSourcePath)
		withWriter(outputFile) { output =>
			for (token <- tokenList) token.tpe match {
				case Some(t) => {
					// Fill the text file
					val declarationTag = token.reference match {
						case Some(l) => l.target toString
						case None => ""
					}
					output.write(token.start + "\t" +
						(token.start + token.length - 1) + "\t" +
						t.name + "\t" +
						declarationTag + "\n")

					// Store tag information (to be output in writeEnd)
					require(token.definitions.size <= 1, "Definitions were not collapsed for " + token)
					token.definitions.foreach((i: Int) => ctags += new Ctag(i.toString, sourceFile, token.start))
				}
				case _ => // Nothing to do
			}
		}
	}
	
	def writeEnd() {
		val ctagsFile = new File(outputDirectory, TagsFileName);
		withWriter(ctagsFile) { output =>
			for (ctag <- ctags) output.write(ctag.toString)
		}
	}
}

/** The information of an occurrence in the tags file */
class Ctag(val name: String, val file: File, offset: Int) extends Comparable[Ctag] {
	override def compareTo(that: Ctag) = this.name.compareTo(that.name)
	override def toString = name + "\t" + file.getAbsolutePath + "\t" + ":goto " + (offset + 1) + "\n"
} 