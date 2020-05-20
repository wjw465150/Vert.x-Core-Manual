//构建SUMMARY.md里的章节

def parten = ~/^#{2,}\s{1}.{1,}[^}]$/

def filename=$/VertX核心手册Java版.md/$
def fullName=$/c:\WJW_E\白石-Markdown\VertX3\${filename}/$

int i=0;
new File(fullName).eachLine{line->
  java.util.regex.Matcher matcher = parten.matcher(line.trim())
  if (matcher.matches() ) {
    i++;
    String newStr = "${i}"+line.trim().substring(matcher.start(), matcher.end()).trim().replaceAll("(\\#)|( )|(\\?)|(\')|(-)|(/)|(\\()|(\\))|(\\.)|(’)|(,)|(!)","_")
    
    println "".padLeft((line.count("#")-1)*2)+"* ["+line.replaceAll("#","").trim()+"](${filename}#"+newStr+")"
  }
}
