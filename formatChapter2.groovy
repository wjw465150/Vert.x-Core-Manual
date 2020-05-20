// 先把英文章节的行格式化成"<a name="standard_chapter"></a>\r\n## chapter name"格式

def parten = ~/^#{2,}\s{1}.{1,}[^}]$/

def name=$/c:\WJW_E\白石-Markdown\VertX3\VertX核心手册Java版.md/$

File file = new File($/${name}.FFF/$)
file.write("") 

int i=0;
new File(name).eachLine{line->
  java.util.regex.Matcher matcher = parten.matcher(line.trim())
  if (matcher.matches() ) {
    i++;
    String newStr = "${i}"+line.trim().substring(matcher.start(), matcher.end()).trim().replaceAll("(\\#)|( )|(\\?)|(\')|(-)|(/)|(\\()|(\\))|(\\.)|(’)|(,)|(!)","_")
    
    println "${line} <a name=\"${newStr}\"></a>"
    file.append("<a name=\"${newStr}\"></a>\r\n${line}\r\n","UTF-8")
  } else {
    file.append("${line}\r\n","UTF-8")
  }
}
