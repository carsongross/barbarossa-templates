package demo.views;
public class Index extends bb.sparkjava.BBSparkTemplate {

private static Index INSTANCE = new Index();
    public static String render() {
        StringBuilder sb = new StringBuilder();
        renderInto(sb);
        return sb.toString();
    }

     public static void renderInto(Appendable buffer) {INSTANCE.renderImpl(buffer);}    public void renderImpl(Appendable buffer) {
        try {
Layout.asLayout().header(buffer);            buffer.append("\n");
            buffer.append("\n\n<div id=\"outer-frame\">\n    <div id=\"who\">\n        <ul>\n            <li>Harika</li>\n            <li>Ed</li>\n            <li>Carson</li>\n        </ul>\n    </div>\n\n    <div id=\"top-bar\">\n        Welcome to internchan\n    </div>\n\n    <div id=\"messages\">\n        <div>\n            Demo Message\n        </div>\n    </div>\n\n    <div id=\"chat-box\">\n        <form>\n        <textarea name=\"message\">\n        </textarea>\n            <button>Submit</button>\n        </form>\n    </div>\n</div>");
Layout.asLayout().footer(buffer);} catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}