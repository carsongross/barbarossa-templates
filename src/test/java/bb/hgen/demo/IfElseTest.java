package bb.hgen.demo;

import java.io.IOException;


public class IfElseTest {

    public static String render() {
        StringBuilder sb = new StringBuilder();
        renderInto(sb);
        return sb.toString();
    }

    public static void renderInto(Appendable buffer) {
        try {
            buffer.append("<!DOCTYPE html>\n");
            int day = 3;
            buffer.append("\n<html>\n    <head><title>IF...ELSE Example</title></head>\n\n    <body>\n        ");
            if (day == 1 | day == 7) {
            buffer.append("\n            <p> Today is weekend</p>\n        ");
            } else {
            buffer.append("\n            <p> Today is not weekend</p>\n        ");
            }
            buffer.append("\n    </body>\n</html>");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String toS(Object o) {
        return o == null ? "" : o.toString();
    }
}