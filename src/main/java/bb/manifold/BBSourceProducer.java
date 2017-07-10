package bb.manifold;

import bb.codegen.ITemplateCodeGenerator;
import manifold.api.fs.IFile;
import manifold.api.host.ITypeLoader;
import manifold.api.sourceprod.JavaSourceProducer;
import manifold.util.JavacDiagnostic;
import manifold.util.StreamUtil;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStreamReader;

public class BBSourceProducer extends JavaSourceProducer<BBModel> {

    public void init( ITypeLoader typeLoader )
    {
        init(typeLoader, BBModel::new);
    }

    @Override
    protected boolean isInnerType(String topLevelFqn, String relativeInner) {
        return false;
    }

    @Override
    public boolean handlesFileExtension(String fileExtension) {
        return false;
    }

    @Override
    public boolean handlesFile(IFile file) {
        return file.getBaseName().endsWith(".bb");
    }

    @Override
    protected String produce(String topLevelFqn, String existing, BBModel model, DiagnosticListener<JavaFileObject> errrorHandler) {
        IFile file = model.getFile();
        try {
            String templateSource = StreamUtil.getContent(new InputStreamReader(file.openInputStream()));
            ITemplateCodeGenerator generator = getGenerator();
            return generator.generateCode(topLevelFqn, templateSource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ITemplateCodeGenerator getGenerator() {
        return new ITemplateCodeGenerator() {
            @Override
            public String generateCode(String fullyQualifiedName, String source) {
                return "";
            }
        };
    }
}