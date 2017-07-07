package views;Index;

import java.io.IOException;


public class views;Index extends bb.sparkjava.BBSparkTemplate {

private static views;Index INSTANCE = new views;Index();


    public static String render() {
        StringBuilder sb = new StringBuilder();
        renderInto(sb);
        return sb.toString();
    }

    public static void renderInto(Appendable buffer) {
        INSTANCE.renderImpl(buffer);
    }

    public void renderImpl(Appendable buffer) {
    try {
            Layout.asLayout().header(buffer);
            buffer.append("\n");
            buffer.append("\n\n<h1>Hello Spark!</h1>");
            Layout.asLayout().footer(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
