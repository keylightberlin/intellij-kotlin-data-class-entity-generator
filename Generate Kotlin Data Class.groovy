import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = "de.keylight.subscription.products.entities"
typeMapping = [
  (~/(?i)tinyint/)                  : "Boolean",
  (~/(?i)int/)                      : "Int",
  (~/(?i)float|double|decimal|real/): "BigDecimal",
  (~/(?i)datetime|timestamp/)       : "Date",
  (~/(?i)date/)                     : "Date",
  (~/(?i)time/)                     : "Time",
  (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def fields = calcFields(table)
  new File(dir, className + ".kt").withPrintWriter { out -> generate(out, className, fields) }
}

def generate(out, className, fields) {
  out.println "package $packageName"
  out.println ""
  out.println ""
  out.println "import java.util.*"
  out.println "import javax.persistence.*"
  out.println ""
  out.println "@Entity"
  out.println "data class $className("
  out.println ""
  fields.each() {
    if (it.annos != "") out.println "  ${it.annos}"

    if (it.primary) {
      out.println "        @Id"
      out.println "        val ${it.name}: ${it.type} = 0,"
    } else {
      out.println "        @Column${it.orgType == "text" ? "(columnDefinition=\"TEXT\", name=${col.getName()})" : "(name=${col.getName()})"}"
      out.println "        val ${it.name}: ${it.type}${it.nullable ? "?" : ""}${it == fields.last() ? "" : ","}"
    }

    out.println ""

  }
  out.println ")"
}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
                 name : javaName(col.getName(), false),
                 type : typeStr,
                 orgType: spec,
                 nullable: !col.isNotNull(),
                 primary: DasUtil.isPrimary(col),
                 annos: ""]]
  }
}

def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
    .collect { Case.LOWER.apply(it).capitalize() }
    .join("")
    .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

