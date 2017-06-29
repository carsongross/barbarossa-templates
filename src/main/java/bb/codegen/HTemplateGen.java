package bb.codegen;

import bb.tokenizer.HTokenizer;
import bb.tokenizer.Token;

import java.io.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static bb.codegen.HTemplateGen.Directive.DirType.*;
import static bb.tokenizer.Token.TokenType.DIRECTIVE;
import static bb.tokenizer.Token.TokenType.STATEMENT;


public class HTemplateGen {
    private static final String baseClassName = "extends bb.runtime.BaseBBTemplate";

    private static class fileTypeChecker implements BiPredicate {
        public boolean test(Object path, Object attr){
            String regexStr = ".*\\.bb\\..*";
            return path.toString().matches(regexStr);
        }
    }

    private static class Directive {
        int tokenPos;
        Token dir;

        enum DirType {
            IMPORT,     //className
            EXTENDS,    //className
            PARAMS,     //           params, paramsList
            INCLUDE,     //className, params
            SECTION,    //className, params, paramsList
            END_SECTION//
            }

        DirType dirType;

        //import "[class_name]"
        //extends "[class_name]"
        //params ([paramType paramName], [paramType paramName],...)                  <---nothing stored for params or end section
        //include "[templateName]"([paramVal], [paramVal],...)
        //section "[sectionName]"([paramType paramName], [paramType paramName],...)
        //end section
        String className;

        //iff section, params, and include (empty string if params not given for include)
        String params;

        //iff section and params only (include doesn't need it broken down bc types aren't given)
        String[][] paramsList;

        Directive(int tokenPos, Token dir) {
            assert(dir.getType() == DIRECTIVE);
            this.tokenPos = tokenPos;
            this.dir = dir;
        }

        private DirType identifyType() {
            String content = dir.getContent();

            if (content.matches("import.*")) {
                dirType = IMPORT;
            } else if (content.matches("extends.*")) {
                dirType = EXTENDS;
            } else if (content.matches("params.*")) {
                dirType = PARAMS;
            } else if (content.matches("include.*")) {
                dirType = INCLUDE;
            } else if (content.matches("section.*")) {
                dirType = SECTION;
            } else if (content.trim().matches("end section")) {
                dirType = END_SECTION;
            } else {
                throw new RuntimeException("Unsupported Directive Type on Line " + dir.getLine());
            }
        }

        private void fillVars() {
            switch (dirType) {

                case IMPORT:
                    className = dir.getContent().substring(6).trim();
                    break;
                case EXTENDS:
                    className = dir.getContent().substring(7).trim();
                    break;
                case PARAMS:
                    String content = dir.getContent().substring(6);
                    params = content.trim().substring(1, content.length());
                    paramsList = splitParamsList(params);
                    break;
                case INCLUDE:
                    String[] parts = dir.getContent().substring(8).trim().split("\\(", 2);
                    className = parts[0];

                    if (parts.length == 2) {
                        params = parts[1].substring(0, parts[1].length() - 1).trim();
                    } else {
                        params = "";
                    }

                    break;
                case SECTION:
                    String[] temp = dir.getContent().substring(7).trim().split("\\(", 2);
                    className = temp[0];

                    if (temp.length == 2) {
                        params = temp[1].substring(0, temp[1].length() - 1).trim();
                        paramsList = splitParamsList(params);
                        findParamTypes(paramsList);
                    } else {
                        params = "";
                    }
                    break;
                case END_SECTION:
                    break;
            }
        }
    }

    private static class Name {
        String inputDir;
        String outputDir;
        String fileName;
        String relativePath;
        String javaWholePath;


        Name(String inputDir, String outputDir, Path bbFile) {

            this.inputDir = inputDir;
            this.outputDir = outputDir;

            fileName = bbFile.toFile().getName().split("\\.bb\\.")[0];
//            String regexString = ".*" + fileName;
//            Pattern pat = Pattern.compile(regexString);
//            Matcher mat = pat.matcher(bbFile.toString());
//            mat.find();
//            String withoutFileType = mat.group(0);
            String withoutFileType = bbFile.toString().split(fileName + "\\.bb\\.")[0];
            //@TODO: \bb\hgen is temporary
            relativePath = "bb\\hgen" + withoutFileType.substring(inputDir.length(), withoutFileType.length() - 1);
            javaWholePath = outputDir + "\\" + relativePath + "\\" + fileName + ".java";

        }

    }
    private static class State {
        Name name;
        int tokenPos = 0;
        int classDepth = 0;
        List<Token> tokens;
        Iterator<Token> tokenIterator;
        StringBuilder header = new StringBuilder();

        State(Name name, List<Token> tokens) {
            this.name = name;
            this.tokens = tokens;
            tokenIterator = tokens.iterator();
        }

    }

//    //@TODO: \bb\hgen is temporary
//    private static String getNewFileName(String inputDir, String outputDir, String bbFileLoc) {
//        String regexString = "(.*\\.bb\\.)";
//        Pattern pat = Pattern.compile(regexString);
//        Matcher mat = pat.matcher(bbFileLoc);
//        mat.find();
//        String withoutFileType = mat.group(0);
//        String extra = withoutFileType.substring(inputDir.length(), withoutFileType.length() - 4);
//        return outputDir + "\\bb\\hgen" + extra + ".java";
//    }


    private static String getJavaContent(Name name, String bbContent) {
        HTokenizer tokenizer = new HTokenizer();
        List<Token> tokens = tokenizer.tokenize(bbContent);
        State state = new State(name, tokens);
        state.header.append("package " + name.relativePath.replaceAll("\\\\", ".") + ";\n\n");
        state.header.append("import java.io.IOException;\n\n");

        StringBuilder classContent = makeClassContent(state);


        return state.header.append(classContent).toString();
    }

    //given a trimmed string of variables,
    // returns a list with a string list per variable with the type and variable name (when both are given)
    // or just the name if both aren't given
    private static String[][] splitParamsList(String params) {
        params = params.replaceAll(" ,", ",").replace(", ", ",");
        String[] parameters = params.split(",");
        String[][] paramsList = new String[parameters.length][2];
        for (int i = 0; i < parameters.length; i++) {
            paramsList[i] = parameters[i].split(" ", 2);
        }
        return paramsList;
    }

    //given a list of 2 element String lists (0th elem is type and 1st elem is value), returns the string form
    //ex. [[String, str],[int,5]] returns "String str, int 5"
    private static String makeParamsString(String[][] paramsList) {
        String params = "" + paramsList[0][0] + " " + paramsList[0][1];
        for (int i = 1; i < paramsList.length; i++) {
            params += ", " + paramsList[i][0] + " " + paramsList[i][1];
        }
        return params;
    }


    //@TODO: seems resource heavy, should fix
    private static String findType(String name, State state) {

        for (int i = state.tokenPos - 1; i >= 0; i--) {
            Token t = state.tokens.get(i);
            if (t.getType() == STATEMENT) {
                String[] content = t.getContent().split("\\s+");
                for (int j = content.length - 1; j >= 0; j--) {
                    if (content[j].matches(name + "(.*)")) {
                        if (content[j].length() == name.length() || content[j].charAt(name.length()) == ';') {
                            return content[j - 1];
                        }
                    }
                }
            }
        }
        throw new RuntimeException("variable " + name + " not found");
    }


    private static void findParamTypes(String[][] params, State state) {
        for (int i = 0; i < params.length; i++) {
            if (params[i].length == 1) {
                    String name = params[i][0];
                    params[i] = new String[2];
                    params[i][0] = findType(name, state);
                    params[i][1] = name;
            }
        }
    }

    private static StringBuilder makeClassContent(State state) {
        return makeClassContent(state.name.fileName, state, null);
    }



    private static StringBuilder makeClassContent(String name, State state, String [][] paramsList) {
        StringBuilder classHeader = new StringBuilder();
        StringBuilder innerClass = new StringBuilder();
        StringBuilder jspContent = new StringBuilder();
        String superClass = baseClassName;
        String params = null;

        outerloop:
        while (state.tokenIterator.hasNext()) {
            Token token = state.tokenIterator.next();
            state.tokenPos++;
            switch (token.getType()) {
                case STRING_CONTENT:
                    jspContent.append("            buffer.append(\"" + token.getContent().replaceAll("\"", "\\\\\"").replaceAll("\r\n", "\\\\n") + "\");\n");
                    break;
                case STATEMENT:
                    jspContent.append("            " + token.getContent() + "\n");
                    break;
                case EXPRESSION:
                    jspContent.append("            buffer.append(toS(" + token.getContent() + "));\n");
                    break;
                case COMMENT:
                    break;
                case DIRECTIVE:
                    if (token.getContent().matches("import.*")) {
                        //@TODO: deal with import not having a space after it
                        state.header.append(token.getContent() + ";\n");
                    } else if (token.getContent().matches("extends.*")) {
                        //@TODO: deal with extends not having a space after it
                        if (superClass == baseClassName) {
                            superClass = token.getContent();
                        } else {
                            throw new RuntimeException("Cannot extend 2 classes:" + superClass + " and " + token.getContent());
                        }
                    } else if (token.getContent().matches("section.*")) {
                        state.classDepth++;
                        String[] content = token.getContent().substring(7).trim().split("\\(", 2);
                        String innerName = content[0];
                        if (content.length == 2 && !content[1].trim().equals("\\)")) {
                            String innerVars = content[1].replace(" {", "{");
                            innerVars = innerVars.substring(0, innerVars.length() - 1);
                            String[][] innerVarsList = splitParamsList(innerVars);
                            findParamTypes(innerVarsList, state);
                            innerClass.append(makeClassContent(innerName, state, innerVarsList));
                            jspContent.append("\n" + innerName + "." + "renderInto(buffer");
                            for (int i = 0; i < innerVarsList.length; i++) {
                                jspContent.append(", " + innerVarsList[i][1]);
                            }
                            jspContent.append(");\n");
                        } else {
                            innerClass.append(makeClassContent(innerName, state, null));
                            jspContent.append("\n" + innerName + "." + "renderInto(buffer);\n");
                        }
                    } else if (token.getContent().equals("end section")) {
                        break outerloop;
                    } else if (token.getContent().matches("params.*")) {
                        if (state.classDepth == 0) {
                            if (paramsList == null) {
                                String content = token.getContent();
                                params = content.substring(7, content.length() - 1).trim();
                                paramsList = splitParamsList(params);
                            } else {
                                throw new RuntimeException("Cannot have 2 params directives: on line" + token.getLine());
                            }
                        } else {
                            throw new RuntimeException("Cannot have a param directive inside a section.");
                        }
                    } else if (token.getContent().matches("include.*")) {
                        String content = token.getContent().substring(8);
                        String[] parts = content.split("\\(", 2);

                        if (parts.length == 1 || (parts.length == 2 && parts[1].trim().equals(")"))) {
                            jspContent.append("            " + parts[0] + ".renderInto(buffer);\n");
                        } else {
                            jspContent.append("            " + parts[0] + ".renderInto(buffer, ");
                            jspContent.append(parts[1] + ";\n");
                        }
                    } else {
                        throw new RuntimeException("Unsupported Directive on line" + token.getLine() + ":" + token.getContent());
                    }
                    break;
            }
        }
        if (state.classDepth == 0) {
            classHeader.append("\npublic class " + state.name.fileName + " " + superClass + " {\n");
        } else {
            classHeader.append("\npublic static class " + name + " " + superClass + " {\n");
        }

        classHeader.append("\nprivate static " + name + " INSTANCE = new " + name + "();\n\n");


        classHeader.append(innerClass);

        if (paramsList == null) {
            classHeader.append("\n" +
                    "    public static String render() {\n" +
                    "        StringBuilder sb = new StringBuilder();\n" +
                    "        renderInto(sb);\n" +
                    "        return sb.toString();\n" +
                    "    }\n\n");

            classHeader.append("    public static void renderInto(Appendable buffer) {\n" +
                    "        INSTANCE.renderImpl(buffer);\n" +
                    "    }\n");
            classHeader.append("    public void renderImpl(Appendable buffer) {\n");

        } else {
            if (params == null) {
                params = makeParamsString(paramsList);
            }
            classHeader.append("\n" +
                    "    public static String render(" + params + ") {\n" +
                    "        StringBuilder sb = new StringBuilder();\n" +
                    "        renderInto(sb");
            for (String[] p : paramsList) {
                classHeader.append(", " + p[1]);
            }
            classHeader.append(");\n" +
                    "        return sb.toString();\n" +
                    "    }\n\n");


            classHeader.append("    public static void renderInto(Appendable buffer, " + params + ") {\n" +
                    "        INSTANCE.renderImpl(buffer");
            for (String[] param: paramsList) {
                classHeader.append(", " + param[1]);
            }
            classHeader.append(");\n" +
                    "    }\n\n");
            classHeader.append("    public void renderImpl(Appendable buffer, " + params + ") {\n");

        }

        if (jspContent.length() > 0) {
            classHeader.append("        try {");

            jspContent.append("        } catch (IOException e) {\n" +
                    "            throw new RuntimeException(e);\n" +
                    "        }\n");
        }

        jspContent.append("    }\n");


        jspContent.append("\n" +
                "    public String toS(Object o) {\n" +
                "        return o == null ? \"\" : o.toString();\n" +
                "    }\n" +
                "}");


        state.classDepth--;

        return classHeader.append(jspContent);
    }

    private List<Directive> getDirectives(List<Token> tokens) {
        ArrayList<Directive> dirList = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() == DIRECTIVE) {
                dirList.add(new Directive(i, token));
            }
        }

        return dirList;
    }

    private void addImports(StringBuilder sb, List<Directive> dirList) {
        for (Directive dir: dirList) {
            if
        }
    }

    public static void main(String[] args) {
        String inputDir = args[0];
        String outputDir = args[1];

        Path root = Paths.get(inputDir);

        try {
            Object[] filesToConvert = Files.find(root, Integer.MAX_VALUE,  new fileTypeChecker()).toArray();
            for (Object p : filesToConvert){
                Name name = new Name(inputDir, outputDir, (Path) p);

                File writeTo = new File(name.javaWholePath);
                if (!writeTo.getParentFile().exists()) {
                    writeTo.getParentFile().mkdirs();
                }
                if (writeTo.createNewFile()){
                    System.out.println("File is created!");
                }else{
                    System.out.println("File already exists.");
                }

                //String content = new String(Files.readAllBytes(Paths.get(p.toString())));
                String content = getJavaContent(name, new String(Files.readAllBytes(Paths.get(p.toString()))));
                FileWriter fw = null;
                BufferedWriter bw = null;

                try {
                    fw = new FileWriter(writeTo);
                    bw = new BufferedWriter(fw);
                    bw.write(content);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (bw != null) {
                            bw.close();
                        }
                        if (fw != null) {
                            fw.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("The given parameter is not a valid directory.");
        }



        //TODO: scan input dir for all files with .bb.* ending and generate
        // a corresponding java file to the given output dir, preserving the package
        // relative to the input dir root, with a .render() static function that
        // renders the template
    }
}
