# mdoc: compiled markdown documentation

[![Build Status](https://travis-ci.org/olafurpg/mdoc.svg?branch=master)](https://travis-ci.org/olafurpg/mdoc)
[![Join the chat at https://gitter.im/olafurpg/mdoc](https://badges.gitter.im/olafurpg/mdoc.svg)](https://gitter.im/olafurpg/mdoc)

mdoc is a documentation tool for Scala inspired by
[tut](http://tpolecat.github.io/tut/). Like tut, mdoc interprets Scala code
examples in markdown files allowing you to compile markdown documentation as
part of your build. Distinguishing features of mdoc include:

- [good performance](#performance): incremental, hot compilation gives you
  snappy feedback while writing documentation.
- [script semantics](#script-semantics): markdown documents are compiled into
  normal Scala programs making examples copy-paste friendly.
- [good error messages](#good-error-messages): compile errors and crashes are
  reported with positions of the original markdown source making it easy to
  track down where things went wrong.
- [link hygiene](#link-hygiene): catch broken links to non-existing sections
  while generating the site.
- [variable injection](#variable-injection): instead of hardcoding constants
  like versions numbers, use variables like `@VERSION@` to make sure the
  documentation stays up-to-date with new releases.
- [extensible](#extensible): library APIs expose hooks to customize rendering of
  code fences.

Table of contents:

<!-- TOC depthFrom:2 -->

- [Quickstart](#quickstart)
  - [Library](#library)
  - [sbt](#sbt)
  - [Command-line](#command-line)
- [Modifiers](#modifiers)
  - [Default](#default)
  - [Silent](#silent)
  - [Fail](#fail)
  - [Crash](#crash)
  - [Passthrough](#passthrough)
  - [Invisible](#invisible)
  - [Reset](#reset)
  - [PostModifier](#postmodifier)
  - [StringModifier](#stringmodifier)
  - [Scastie](#scastie)
- [Key features](#key-features)
  - [Performance](#performance)
  - [Good error messages](#good-error-messages)
  - [Link hygiene](#link-hygiene)
  - [Script semantics](#script-semantics)
  - [Variable injection](#variable-injection)
  - [Extensible](#extensible)
- [Team](#team)
- [--help](#--help)

<!-- /TOC -->

## Quickstart

Starting from an empty directory, let's create a `docs` directory and
`docs/readme.md` file with some basic documentation

````
$ tree
.
└── docs
    └── readme.md
$ cat docs/readme.md
# My Project

To install my project
```scala
libraryDependencies += "com" % "lib" % "@MY_VERSION@"
```

```scala mdoc
val x = 1
List(x, x)
```
````

Then we generate the site using either the [command-line](#command-line)
interface or [library API](#library). The resulting readme will look like this

````
# My Project

To install my project

```scala
libraryDependencies += "com" % "lib" % "1.0.0"
```

```scala
val x = 1
// x: Int = 1
List(x, x)
// res0: List[Int] = List(1, 1)
```
````

Observe that `MY_VERSION` has been replaced with `1.0.0` and that the
`scala mdoc` code fence has been interpreted by the Scala compiler.

### Library

Add the following dependency to your build

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.geirsson/mdoc_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.geirsson/mdoc_2.12)

```scala
// build.sbt
scalaVersion := "2.12.8" // Any version in 2.12.x works.
libraryDependencies += "com.geirsson" %% "mdoc" % "0.7.1"
```

Then write a main function that invokes mdoc as a library

```scala
object Main {
  def main(args: Array[String]): Unit = {
    // build arguments for mdoc
    val settings = mdoc.MainSettings()
      .withSiteVariables(Map("MY_VERSION" -> "1.0.0"))
      .withArgs(args.toList)
    // generate out/readme.md from working directory
    val exitCode = mdoc.Main.process(settings)
    // (optional) exit the main function with exit code 0 (success) or 1 (error)
    if (exitCode != 0) sys.exit(exitCode)
  }
}
```

Consult [--help](#--help) to see what arguments are valid for `withArgs`.

Consult the mdoc source to learn more how to use the library API. Scaladocs are
available
[here](https://www.javadoc.io/doc/com.geirsson/mdoc_2.12/0.7.1)
but beware there are limited docstrings for classes and methods. Keep in mind
that code in the package `mdoc.internal` is subject to binary and source
breaking changes between any release, including PATCH versions.

### sbt

There is no sbt plugin for mdoc, create a new project instead that uses the mdoc
[Library API](#library).

```scala
// build.sbt
lazy val docs = project
  .in(file("myproject-docs"))
  .settings(
    moduleName := "myproject-docs",
    scalaVersion := "2.12.8", // Any version in 2.12.x works.
    libraryDependencies += "com.geirsson" %% "mdoc" % "0.7.1",
    // (optional): enable compiler plugins and other flags
    resourceGenerators.in(Compile) += Def.task {
      val out = resourceDirectory.in(Compile).value / "mdoc.properties"
      val props = new java.util.Properties()
      props.put("scalacOptions", scalacOptions.in(Compile).value.mkString(" "))
      IO.write(props, "mdoc properties", out)
      List(out)
    }
  )
  .dependsOn(myproject)
```

From the sbt shell, run the mdoc main function

```scala
// sbt shell
> docs/runMain mdoc.Main --help
```

### Command-line

First, install the
[coursier command-line interface](https://github.com/coursier/coursier/#command-line).
Then run the following command:

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.geirsson/mdoc_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.geirsson/mdoc_2.12)

```
$ coursier launch com.geirsson:mdoc_2.12:0.7.1 -- --site.MY_VERSION 1.0.0
info: Compiling docs/readme.md
info:   done => out/readme.md (120 ms)
```

It's possible to customize the input and output directory with `--in` and
`--out`. There is also a `--watch` flag to watch for file changes and
incrementally re-generate pages on file save providing fast feedback while
Consult [`--help`](#--help) to learn more about the command-line interface.

## Modifiers

mdocs supports several modifiers to control the output of code fences.

### Default

The default modifier compiles and executes the code fence as normal


Before:

````
```scala mdoc
val x = 1
val y = 2
x + y
```
````

After:

````
```scala
val x = 1
// x: Int = 1
val y = 2
// y: Int = 2
x + y
// res0: Int = 3
```
````

### Silent

The `silent` modifier is identical to the default modifier except that it hides
the evaluated output. The input code fence renders unchanged.


Before:

````
```scala mdoc:silent
val x = 1
val y = 2
x + y
```
```scala mdoc
x + y
```
````

After:

````
```scala
val x = 1
val y = 2
x + y
```

```scala
x + y
// res1: Int = 3
```
````

### Fail

The `fail` modifier asserts that the code block will not compile


Before:

````
```scala mdoc:fail
val x: Int = ""
```
````

After:

````
```scala
val x: Int = ""
// type mismatch;
//  found   : String("")
//  required: Int
// val x: Int = ""
//              ^
```
````

Note that `fail` does not assert that the program compiles but crashes at
runtime. To assert runtime exceptions, use the `crash` modifier.

### Crash

The `crash` modifier asserts that the code block throws an exception at runtime


Before:

````
```scala mdoc:crash
val y = ???
```
````

After:

````
```scala
val y = ???
// scala.NotImplementedError: an implementation is missing
// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:288)
// 	at repl.Session.$anonfun$app$1(readme.md:8)
// 	at scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.java:23)
```
````

### Passthrough

The `passthrough` modifier collects the stdout and stderr output from the
program and embeds it verbatim in the markdown file.


Before:

````
```scala mdoc:passthrough
val matrix = Array.tabulate(4, 4) { (a, b) =>
  val multiplied = (a + 1) * (b + 1)
  f"$multiplied%2s"
}
val table = matrix.map(_.mkString("| ", " | ", " |")).mkString("\n")
println(s"""
This will be rendered as markdown.

* Bullet 1
* Bullet 2

Look at the table:

$table
""")
```
````

After:

````
This will be rendered as markdown.

* Bullet 1
* Bullet 2

Look at the table:

|  1 |  2 |  3 |  4 |
|  2 |  4 |  6 |  8 |
|  3 |  6 |  9 | 12 |
|  4 |  8 | 12 | 16 |
````

### Invisible

The `invisible` modifier evaluates the code but does not render anything. The
`invisible` modifier is equivalent to `passthrough` when the expression does not
print to stdout.


Before:

````
This is prose.
```scala mdoc:invisible
println("I am invisible")
```
More prose.
````

After:

````
This is prose.

More prose.
````

### Reset

The `reset` modifier starts a new scope where previous statements in the document are no longer available.
This can be helpful to clear existing imports or implicits in scope.


Before:

````
```scala mdoc
implicit val x: Int = 41
```

```scala mdoc:reset
implicit val y: Int = 42
implicitly[Int] // x is no longer in scope
```
```scala mdoc:fail
println(x)
```
````

After:

````
```scala
implicit val x: Int = 41
// x: Int = 41
```

```scala
implicit val y: Int = 42
// y: Int = 42
implicitly[Int] // x is no longer in scope
// res0: Int = 42
```

```scala
println(x)
// not found: value x
// println(x)
//         ^
```
````


### PostModifier

A `PostModifier` is a custom modifier that post-processes a compiled and
interpreted mdoc code fence. Post modifiers have access to the original code
fence text, the static types and runtime values of the evaluated Scala code, the
input and output file paths and other contextual information.

One example use-case for post modifiers is to render charts based on the runtime
value of the last expression in the code fence.

![](evilplot.gif)

Extend the `mdoc.PostModifier` trait to implement a post modifier.


File: [EvilplotModifier.scala](https://github.com/olafurpg/mdoc/blob/master/mdoc-docs/src/main/scala/mdoc/docs/EvilplotModifier.scala)

`````scala
package mdoc.docs

import com.cibo.evilplot.geometry.Drawable
import java.nio.file.Files
import mdoc._
import scala.meta.inputs.Position

class EvilplotModifier extends PostModifier {
  val name = "evilplot"
  def process(ctx: PostModifierContext): String = {
    val out = ctx.outputFile.resolveSibling(_ => ctx.info)
    ctx.lastValue match {
      case d: Drawable =>
        Files.createDirectories(out.toNIO.getParent)
        if (!out.isFile) {
          d.write(out.toFile)
        }
        s"![](${out.toNIO.getFileName})"
      case _ =>
        val (pos, obtained) = ctx.variables.lastOption match {
          case Some(variable) =>
            val prettyObtained =
              s"${variable.staticType} = ${variable.runtimeValue}"
            (variable.pos, prettyObtained)
          case None =>
            (Position.Range(ctx.originalCode, 0, 0), "nothing")
        }
        ctx.reporter.error(
          pos,
          s"""type mismatch:
  expected: com.cibo.evilplot.geometry.Drawable
  obtained: $obtained"""
        )
        ""
    }
  }
}

`````

Next, create a resource file `META-INF/services/mdoc.PostModifier` so the post
modififer is recognized by the JVM
[ServiceLoader](https://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html)
framework.


File: [mdoc.PostModifier](https://github.com/olafurpg/mdoc/blob/master/mdoc-docs/src/main/resources/META-INF/services/mdoc.PostModifier)

`````scala
mdoc.docs.EvilplotModifier

`````

As long as `EvilplotModifier` is available on the classpath, for example via
`libraryDependencies` in build.sbt, then you can use the modifier like this.


Before:

````
```scala mdoc:evilplot:scatterplot.png
import com.cibo.evilplot._
import com.cibo.evilplot.plot._
import com.cibo.evilplot.plot.aesthetics.DefaultTheme._
import com.cibo.evilplot.numeric.Point

val data = Seq.tabulate(90) { i =>
  val degree = i * 8
  val radian = math.toRadians(degree)
  Point(i.toDouble, math.sin(radian))
}

ScatterPlot(data)
  .xAxis()
  .yAxis()
  .frame()
  .xLabel("x")
  .yLabel("y")
  .render()
```
````

After:

````
![](scatterplot.png)
````

Which renders into a scatter plot like this:

![](scatterplot.png)

It's important that post modifiers present helpful error messages to the user in
case of failures. For example, if the last runtime value is not an EvilPlot
`Drawable` we can report the expected and obtained types with carets pointing to
the position of the last variable.


Before:

````
```scala mdoc:evilplot:scatterplot.png
val message = "hello world!"
```
````

Error:

````
error: readme.md:2:5: error: type mismatch:
  expected: com.cibo.evilplot.geometry.Drawable
  obtained: String = hello world!
val message = "hello world!"
    ^^^^^^^
````

### StringModifier

A `StringModifier` is a custom modifier that processes the plain text contents
of a code block, ignoring the compilation and interpretation of the Scala code.

```scala
import mdoc.StringModifier
import mdoc.Reporter
import scala.meta.Input
class FooModifier extends StringModifier {
  override val name = "foo"
  override def process(info: String, code: Input, reporter: Reporter): String = {
    val originalCodeFenceText = code.text
    val isCrash = info == "crash"
    if (isCrash) "BOOM"
    else "OK: " + originalCodeFenceText
  }
}
```

Pass the custom modifier to `MainSettings.withStringModifiers(List(...))`.

```scala
val settings = mdoc.MainSettings()
  .withStringModifiers(List(
    new FooModifier
  ))
```

Code blocks with the `mdoc:foo` modifier will then render as follows.


Before:

````
```scala mdoc:foo
Hello world!
```
````

After:

````
OK: Hello world!
````

We can also add the argument `:crash` to render "BOOM".


Before:

````
```scala mdoc:foo:crash
Hello world!
```
````

After:

````
BOOM
````

### Scastie

The `scastie` modifier transforms a Scala code block into a
[Scastie](https://scastie.scala-lang.org/) snippet.

> ℹ️ This modifier will work only in environments that support embedding a
> `<script>` tag. For example, it won't work in GitHub readmes, but it will work
> when building a static website from Markdown (e.g., with
> [Docusaurus](https://docusaurus.io/))

You can embed an existing Scastie snippet by its id:


Before:

````
```scala mdoc:scastie:xbrvky6fTjysG32zK6kzRQ

```
````

After:

````
<script src='https://scastie.scala-lang.org/xbrvky6fTjysG32zK6kzRQ.js?theme=light'></script>
````

or in case of a user's snippet:


Before:

````
```scala mdoc:scastie:MasseGuillaume/CpO2s8v2Q1qGdO3vROYjfg

```
````

After:

````
<script src='https://scastie.scala-lang.org/MasseGuillaume/CpO2s8v2Q1qGdO3vROYjfg.js?theme=light'></script>
````

> ⚠️ The empty line in the block can't be omitted due to how the Markdown parser
> works

Moreover, you can quickly translate any Scala code block block into a Scastie
snippet on the fly.


Before:

````
```scala mdoc:scastie
val x = 1 + 2
println(x)
```
````

After:

````
<script src="https://scastie.scala-lang.org/embedded.js"></script>

<pre class='scastie-snippet-<a_random_uuid>'></pre>

<script>window.addEventListener('load', function() {
scastie.Embedded('.scastie-snippet-<a_random_uuid>', {
code: `val x = 1 + 2
println(x)`,
theme: 'light',
isWorksheetMode: true,
targetType: 'jvm',
scalaVersion: '2.12.6'
})
})</script>
````

> ⚠️ Inline snippets are slower to run than embedded ones, since they won't be
> cached. You should prefer embedding existing snippets whenever possible.

You can choose the Scastie theme when initializing the Scastie modifier:

```scala
import mdoc.modifiers.ScastieModifier
new ScastieModifier(theme = "dark") // default is "light"
// res0: ScastieModifier = StringModifier(mdoc:scastie)
```

## Key features

### Performance

mdoc is designed to provide a tight edit/render/preview feedback loop while
writing documentation. mdoc achieves good performance through

- [script semantics](#script-semantics): each markdown file compiles into a
  single Scala program that executes in one run.
- being incremental: with `--watch`, mdoc compiles individual files as they
  change avoiding unnecessary work re-generating the full site.
- keeping the compiler hot: with `--watch`, mdoc re-uses the same Scala compiler
  instance for subsequent runs making compilation faster after a few iterations.
  A medium sized document can go from compiling in ~5 seconds with a cold
  compiler down to 500ms with a hot compiler.

### Good error messages

mdoc tries to report helpful error messages when things go wrong. Here below,
the program that is supposed to compile successfully but it has a type error so
the build is stopped with an error message from the Scala compiler.


Before:

````
```scala mdoc
val typeError: Int = "should be int"
```
````

Error:

````
error: readme.md:2:22: error: type mismatch;
 found   : String("should be int")
 required: Int
val typeError: Int = "should be int"
                     ^^^^^^^^^^^^^^^
````

Here below, the programs are supposed to fail due to the `fail` and `crash`
modifiers but they succeed so the build is stopped with an error message from
mdoc.


Before:

````
```scala mdoc:fail
val noFail = "success"
```
```scala mdoc:crash
val noCrash = "success"
```
````

Error:

````
error: readme.md:2:1: error: Expected compile error but statement type-checked successfully
val noFail = "success"
^^^^^^^^^^^^^^^^^^^^^^
error: readme.md:5:1: error: Expected runtime exception but program completed successfully
val noCrash = "success"
^^^^^^^^^^^^^^^^^^^^^^^
````

Observe that positions of the reported diagnostics point to line numbers and
columns in the original markdown document. Internally, mdoc instruments code
fences to extract metadata like variable types and runtime values. Positions of
error messages in the instrumented code are translated into positions in the
markdown document.

### Link hygiene

Docs get quickly out date, in particular links to different sections. After
generating a site, mdoc analyzes links for references to non-existent sections.
For the example below, mdoc reports a warning that the `doesnotexist` link is
invalid.


Before:

````
# My title

Link to [my title](#my-title).
Link to [typo section](#mytitle).
Link to [old section](#doesnotexist).
````

Error:

````
warning: readme.md:4:9: warning: Unknown link 'readme.md#mytitle', did you mean 'readme.md#my-title'?
Link to [typo section](#mytitle).
        ^^^^^^^^^^^^^^^^^^^^^^^^
warning: readme.md:5:9: warning: Unknown link 'readme.md#doesnotexist'.
Link to [old section](#doesnotexist).
        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
````

Observe that mdoc suggests a fix if there exists a header that is similar to the
unknown link.

### Script semantics

mdoc interprets code fences as normal Scala programs instead of as if they're
evaluated in the REPL. This behavior is different from tut that interprets
statements as if they were typed in a REPL session. Using "script semantics"
instead of "repl semantics" has both benefits and downsides.

**Downside**: It's not possible to bind the same variable twice, for example the
code below input fails compilation with mdoc but compiles successfully with tut

````
```scala mdoc
val x = 1
val x = 1
```
````

**Upside**: Code examples can be copy-pasted into normal Scala programs and
compile.

**Upside**: Companion objects Just Work™️


Before:

````
```scala mdoc
case class User(name: String)
object User {
  implicit val ordering: Ordering[User] = Ordering.by(_.name)
}
List(User("John"), User("Susan")).sorted
```
````

After:

````
```scala
case class User(name: String)
object User {
  implicit val ordering: Ordering[User] = Ordering.by(_.name)
}
List(User("John"), User("Susan")).sorted
// res0: List[User] = List(User("John"), User("Susan"))
```
````

### Variable injection

mdoc renders constants like `0.7.1` in markdown with variables provided at
runtime. This makes it easy to keep documentation up-to-date as new releases are
published. Variables can be passed from the command-line interface with the
syntax

```
mdoc --site.VERSION 1.0.0 --site.SCALA_VERSION 2.12.8
```

When using the library API, variables are passed with the
`MainSettings.withSiteVariables(Map[String, String])` method

```scala
val settings = mdoc.MainSettings()
  .withSiteVariables(Map(
    "VERSION" -> "1.0.0",
    "SCALA_VERSION" -> "2.12.8"
  ))
```

Variables are replaced with the regular expression `@(\w+)@`. An error is
reported when a variable that matches this pattern is not provided.


Before:

````
Install version @DOES_NOT_EXIST@
````

Error:

````
error: readme.md:1:17: error: key not found: DOES_NOT_EXIST
Install version @DOES_NOT_EXIST@
                ^^^^^^^^^^^^^^^^
````

Use double `@@` to escape variable injection.


Before:

````
Install version @@OLD_VERSION@
````

After:

````
Install version @OLD_VERSION@
````

### Extensible

The mdoc library API enables users to implement "modifiers" that customize the
rendering of mdoc code fences. There are two kinds of modifiers:

- [PostModifier](#postmodifier): post-process a compiled and interpreted mdoc
  code fence.
- [StringModifier](#stringmodifier): process code fences as plain string values
  without mdoc compilation or intepretation.

⚠️ This feature is under development and is likely to have breaking changes in
future releases.

## Team

The current maintainers (people who can merge pull requests) are:

- Jorge Vicente Cantero - [`@jvican`](https://github.com/jvican)
- Ólafur Páll Geirsson - [`@olafurpg`](https://github.com/olafurpg)

Contributions are welcome!

## --help

```
mdoc v0.7.1
Usage:   mdoc [<option> ...]
Example: mdoc --in <path> --out <path> (customize input/output directories)
         mdoc --watch                  (watch for file changes)
         mdoc --site.VERSION 1.0.0     (pass in site variables)
         mdoc --include **/example.md  (process only files named example.md)
         mdoc --exclude node_modules   (don't process node_modules directory)

mdoc is a documentation tool that interprets Scala code examples within markdown
code fences allowing you to compile and test documentation as part your build. 

Common options:

  --in | -i <path> (default: "docs")
    The input directory containing markdown and other documentation sources.
    Markdown files will be processed by mdoc while other files will be copied
    verbatim to the output directory.

  --out | -o <path> (default: "out")
    The output directory to generate the mdoc site.

  --watch | -w
    Start a file watcher and incrementally re-generate the site on file save.

  --check
    Instead of generating a new site, report an error if generating the site would
    produce a diff against an existing site. Useful for asserting in CI that a
    site is up-to-date.

  --verbose
    Include additional diagnostics for debugging potential problems.

  --site Map[String, String] (default: {})
    Key/value pairs of variables to replace through @VAR@. For example, the flag
    '--site.VERSION 1.0.0' will replace appearances of '@VERSION@' in markdown
    files with the string 1.0.0

Compiler options:

  --classpath String (default: "")
    Classpath to use when compiling Scala code examples. Defaults to the current
    thread's classpath.

  --scalac-options String (default: "")
    Compiler flags such as compiler plugins '-Xplugin:kind-projector.jar' or custom
    options '-deprecated'. Formatted as a single string with space separated
    values. To pass multiple values: --scalac-options "-Yrangepos -deprecated".
    Defaults to the value of 'scalacOptions' in the 'mdoc.properties' resource
    file, if any.

  --clean-target
    Remove all files in the outout directory before generating a new site.

LiveReload options:

  --no-livereload
    Don't start a LiveReload server

  --port Int (default: 4000)
    Which port the LiveReload server should listen to. If the port is not free,
    another free port close to this number is used.

  --host String (default: "localhost")
    Which hostname the LiveReload server should listen to

Less common options:

  --help
    Print out a help message and exit

  --usage
    Print out usage instructions and exit

  --version
    Print out the version number and exit

  --include [<glob> ...] (default: [])
    Glob to filter which files to process. Defaults to all files. Example: --include
    **/example.md will process only files with the name example.md.

  --exclude [<glob> ...] (default: [])
    Glob to filter which files from exclude from processing. Defaults to no files.
    Example: --include users/**.md --exclude **/example.md will process all
    files in the users/ directory excluding files named example.md.

  --report-relative-paths
    Use relative filenames when reporting error messages. Useful for producing
    consistent docs on a local machine and CI. 

  --charset Charset (default: "UTF-8")
    The encoding to use when reading and writing files.

  --cwd <path> (default: "<current working directory>")
    The working directory to use for making relative paths absolute.


```

