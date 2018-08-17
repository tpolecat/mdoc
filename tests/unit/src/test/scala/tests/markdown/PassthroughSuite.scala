package tests.markdown

class PassthroughSuite extends BaseMarkdownSuite {

  check(
    "passthrough",
    """
      |```scala vork:passthrough
      |val x = println("# Header\n\nparagraph\n\n* bullet")
      |```
      """.stripMargin,
    """
      |# Header
      |
      |paragraph
      |
      |* bullet
      """.stripMargin
  )

  check(
    "no-val",
    """
      |```scala vork:passthrough
      |println("# Header")
      |```
    """.stripMargin,
    """
      |# Header
    """.stripMargin
  )

  check(
    "stripMargin",
    """
```scala vork:passthrough
println('''|* Bullet 1
           |* Bullet 2
     |* Bullet 3
        '''.stripMargin)
```
    """.replaceAllLiterally("'''", "\"\"\""),
    """
* Bullet 1
* Bullet 2
* Bullet 3
    """
  )

}