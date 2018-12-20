import java.io.IOException;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class UcodeGenerator {

	public static void main(String[] args) throws IOException {
		MiniGoLexer lexer = new MiniGoLexer(CharStreams.fromFileName("[00][201402404][이준상][02].go"));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		MiniGoParser parser = new MiniGoParser(tokens);
		ParseTree tree = parser.program();
		
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(new UCodeGenListener(), tree);
	}

}
