package bb.egen.demo;
import java.util.*;
public class NestedImportTest extends bb.runtime.BaseBBTemplate {

private static NestedImportTest INSTANCE = new NestedImportTest();
static class mySection extends bb.runtime.BaseBBTemplate {

private static mySection INSTANCE = new mySection();
    public static String render() {
        StringBuilder sb = new StringBuilder();
        renderInto(sb);
        return sb.toString();
    }

    public String toS(Object o) {
        return o == null ? "" : o.toString();
    }

     public static void renderInto(Appendable buffer) {INSTANCE.renderImpl(buffer);}    public void renderImpl(Appendable buffer) {
        try {
            buffer.append("\n        ");
            buffer.append("\n        ");
            HashSet<Integer> myHashSet = new HashSet<>();
        myHashSet.add(1);
        myHashSet.add(2);
        myHashSet.add(3);
        for(Integer a: myHashSet) {
            buffer.append("\n        <h2 style=\"font-size: ");
            buffer.append(toS(a));
            buffer.append("\">Font size: ");
            buffer.append(toS(a));
            buffer.append("</h2>\n        ");
            }
            buffer.append("\n    ");
} catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
    public static String render() {
        StringBuilder sb = new StringBuilder();
        renderInto(sb);
        return sb.toString();
    }

    public String toS(Object o) {
        return o == null ? "" : o.toString();
    }

     public static void renderInto(Appendable buffer) {INSTANCE.renderImpl(buffer);}    public void renderImpl(Appendable buffer) {
        try {
            buffer.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <title>Nested Import Tests</title>\n</head>\n<body>\n    <h1>This will make sure that nested imports are handled correctly.</h1>\n    ");
            mySection.renderInto(buffer);
            buffer.append("\n        <p> The above section should work </p>\n</body>\n</html>");
} catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}