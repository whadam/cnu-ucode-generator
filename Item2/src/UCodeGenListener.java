import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class UCodeGenListener extends MiniGoBaseListener {

	ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
	
	ArrayList<Node> local_var_list = new ArrayList<>();
	ArrayList<Node> global_var_list = new ArrayList<>();
	
	int local_var_number = 0;
	int global_var_number = 0;
	int local_array_number = 0;
	int global_array_number = 0;
	int label_number = 6;
	int block_level = 1;
	int global_offset = 1;
	int local_offset = 1;
	boolean have_return_stmt;
	String ws = "           ";
	
	class Node {
		String ident, num;
		boolean isArray, isParam = false, isArgs = false, isAssign_stmt = false;
		
		Node(String ident, String num, boolean isArray) {
			this.ident = ident;
			this.num = num;
			this.isArray = isArray;
		}
	}
	
	Node findNode(String txt) {
		Node n;
		
		if (findLocalVar(txt) != null) {
			n = findLocalVar(txt);
		}
		else {//findVar(txt) != null
			n = findVar(txt);
		}
		
		return n;
	}
	
	Node findLocalVar(String str) {
		Node n = null;
		
		for (int i = 0; i < local_var_list.size(); i++) {
			if (local_var_list.get(i).ident.equals(str)) {
				n = local_var_list.get(i);
			}
		}
		
		return n;
	}
	
	Node findVar(String str) {
		Node n = null;
		
		for (int i = 0; i < global_var_list.size(); i++) {
			if (global_var_list.get(i).ident.equals(str)) {
				n = global_var_list.get(i);
			}
		}
		
		return n;
	}

	@Override
	public void exitProgram(MiniGoParser.ProgramContext ctx) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("[00][201402404][이준상][03].txt"));
			
//			System.out.println(testString);
			for (MiniGoParser.DeclContext d : ctx.decl()) {
				bw.write(newTexts.get(d));
				System.out.println(newTexts.get(d));
			}
			bw.newLine();
			
			bw.write(ws + "bgn " + global_var_number);
			bw.newLine();
			System.out.println(ws + "bgn " + global_var_number);
			
			for (MiniGoParser.DeclContext d : ctx.decl()) {
				if (d.fun_decl() != null && d.fun_decl().getChild(1).getText().equals("main")) {
					bw.write(ws + "ldp\n" +
							ws + "call main\n" +
							ws + "end");
					System.out.println(ws + "ldp\n" +
										ws + "call main\n" +
										ws + "end");
				}
			}
			
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void exitDecl(MiniGoParser.DeclContext ctx) {
		String str;
		
		if (ctx.getChild(0) == ctx.var_decl()) {
			str = newTexts.get(ctx.var_decl());
		}
		else {
			str = newTexts.get(ctx.fun_decl());
		}
		
		newTexts.put(ctx, str);
	}

	@Override
	public void exitVar_decl(MiniGoParser.Var_declContext ctx) {//전역변수
		String str = "";
		
		if (ctx.getChildCount() == 3) {//VAR IDENT type_spec
			global_var_list.add(new Node(ctx.getChild(1).getText(), block_level + " " + (++global_var_number), false));
			str += ws + "sym " + block_level + " " + global_offset++ + " 1";
		}
		else if (ctx.getChildCount() == 5) {//VAR IDENT ',' IDENT type_spec
			global_var_list.add(new Node(ctx.getChild(1).getText(), block_level + " " + (++global_var_number), false));
			str += ws + "sym " + block_level + " " + global_offset++ + " 1";
			global_var_list.add(new Node(ctx.getChild(3).getText(), block_level + " " + (++global_var_number), false));
			str += ws + "sym " + block_level + " " + global_offset++ + " 1";
		}
		else {//ctx.getChildCount() == 6 //VAR IDENT '[' LITERAL ']' type_spec
			global_var_list.add(new Node(ctx.getChild(1).getText(), block_level + " " + (++global_var_number), true));
			global_array_number = Integer.parseInt(ctx.LITERAL().getText());
			str += ws + "sym " + block_level + " " + global_offset + " " + global_array_number;
			global_offset += global_array_number;
		}
		
		newTexts.put(ctx, str);
	}
	
	@Override
	public void enterFun_decl(MiniGoParser.Fun_declContext ctx) {
		block_level++;
		have_return_stmt = false;
		super.enterFun_decl(ctx);
	}

	@Override
	public void exitFun_decl(MiniGoParser.Fun_declContext ctx) {
		String str = "";
		
		str += ctx.getChild(1).getText() +
				ws.substring(ctx.getChild(1).getText().length(), ws.length()) +
				"proc " + local_var_number + " " + block_level + " 2\n";//lexical level == block number
		
		for (int i = 0; i < local_var_number; i++) {
			if (local_var_list.get(i).isArray == true) {
				str += ws + "sym " + block_level + " " + local_offset + " " + local_array_number + "\n";
				local_offset += local_array_number;
			}
			else {
				global_offset += 1;
				str += ws + "sym " + block_level + " " + local_offset++ + " 1\n"; 
			}
		}
				
		str += newTexts.get(ctx.compound_stmt());
		
		if (!have_return_stmt) {
			str += ws + "end";
		}
		else {
			str += ws + "ret\n" + ws + "end";
		}
		
		local_array_number = 0;
		local_var_number = 0;
		block_level--;
		
		newTexts.put(ctx, str);
	}

	@Override
	public void exitParams(MiniGoParser.ParamsContext ctx) {
		String str = "";
		
		if (ctx.getChild(0) != ctx.param(0)) {
			str += ctx.getChild(0).getText();
		}
		else {
			for (int i = 0; i < ctx.param().size(); i++) {
				str += ctx.param(i).getText();
			}
		}
		
		newTexts.put(ctx, str);
	}

	@Override
	public void exitParam(MiniGoParser.ParamContext ctx) {
		Node n = new Node(ctx.IDENT().getText(), block_level + " " + (++local_var_number), false);//IDENT type_spec
		n.isParam = true;
		
		if (ctx.getChildCount() == 4) {//IDENT '[' ']' type_spec
			n.isArgs = true;
		}
		
		local_var_list.add(n);
		
		newTexts.put(ctx, "");
	}

	@Override
	public void exitStmt(MiniGoParser.StmtContext ctx) {
		String str = "";
		
		if (ctx.getChild(0) == ctx.assign_stmt()) {
			str += newTexts.get(ctx.assign_stmt());
		}
		else if (ctx.getChild(0) == ctx.compound_stmt()) {
			str += newTexts.get(ctx.compound_stmt());
		}
		else if (ctx.getChild(0) == ctx.expr_stmt()) {
			str += newTexts.get(ctx.expr_stmt());
		}
		else if (ctx.getChild(0) == ctx.if_stmt()) {
			str += newTexts.get(ctx.if_stmt());
		}
		else if (ctx.getChild(0) == ctx.for_stmt()) {
			str += newTexts.get(ctx.for_stmt());
		}
		else {//ctx.getChild(0) == ctx.return_stmt()
			str += newTexts.get(ctx.return_stmt());
		}
		
		newTexts.put(ctx, str);
	}

	@Override
	public void exitExpr_stmt(MiniGoParser.Expr_stmtContext ctx) {
		newTexts.put(ctx, newTexts.get(ctx.expr()));
	}

	@Override
	public void exitAssign_stmt(MiniGoParser.Assign_stmtContext ctx) {//지역변수 선언 및 정의 or 정의
		String str = "";
		
		if (ctx.getChildCount() == 9) {//VAR IDENT ',' IDENT type_spec '=' LITERAL ',' LITERAL
			Node n = new Node(ctx.IDENT(0).getText(), block_level + " " + (++local_var_number), false);
			n.isAssign_stmt = true;
			local_var_list.add(n);
			str += ws + "ldc " + ctx.LITERAL(0).getText() + "\n" +
					ws + "str " + n.num + "\n";
			Node n2 = new Node(ctx.IDENT(1).getText(), block_level + " " + (++local_var_number), false);
			n2.isAssign_stmt = true;
			local_var_list.add(n2);
			str += ws + "ldc " + ctx.LITERAL(1).getText() + "\n" + 
					ws + "str " + n2.num + "\n";
		}
		else if (ctx.getChildCount() == 5) {//VAR IDENT type_spec '=' expr
			Node n = new Node(ctx.IDENT(0).getText(), block_level + " " + (++local_var_number), false);
			n.isAssign_stmt = true;
			local_var_list.add(n);
			str += newTexts.get(ctx.expr(0)) +
					ws + "str " + n.num + "\n";
		}
		else if (ctx.getChildCount() == 4) {//IDENT type_spec '=' expr
			Node var = findNode(ctx.IDENT(0).getText());
			
			str += newTexts.get(ctx.expr(0)) + 
					ws + "str " + var.num + "\n";
		}
		else {//ctx.getChildCount() == 6	//IDENT '[' expr ']' '=' expr
			Node var = findNode(ctx.IDENT(0).getText());
			
			str += newTexts.get(ctx.expr(0));
			
			if (var.isArgs == true) {
				str += ws + "lod " + var.num + "\n";
			}
			else {
				str += ws + "lda " + var.num + "\n";
			}
			
			str += ws + "add\n" + newTexts.get(ctx.expr(1)) + ws + "sti\n";
		}
		
		newTexts.put(ctx, str);
	}

	@Override
	public void exitCompound_stmt(MiniGoParser.Compound_stmtContext ctx) {
		String str = "";
		
		for (int i = 0; i < ctx.local_decl().size(); i++) {
			str += newTexts.get(ctx.local_decl(i));
		}
		for (int i = 0; i < ctx.stmt().size(); i++) {
			str += newTexts.get(ctx.stmt(i));
		}
		
		newTexts.put(ctx, str);
	}
	
	@Override
	public void exitIf_stmt(MiniGoParser.If_stmtContext ctx) {
		String str = newTexts.get(ctx.expr());
		
		label_number--;
		
		str += ws + "fjp $$" + label_number + "\n" + newTexts.get(ctx.compound_stmt(0)) +
				"$$" + label_number + ws.substring(3, ws.length()) + "nop\n";
		
		if (ctx.getChildCount() == 5) {//IF expr compound_stmt ELSE compound_stmt
			str += newTexts.get(ctx.compound_stmt(1));
		}
		
		newTexts.put(ctx, str);
	}

	@Override
	public void exitFor_stmt(MiniGoParser.For_stmtContext ctx) {
		String str = "";
		
		label_number -= 2;
		
		str += "$$" + label_number + ws.substring(3, ws.length()) + "nop\n" + newTexts.get(ctx.expr()) +
				ws + "fjp $$" + (label_number+1) + "\n" + newTexts.get(ctx.compound_stmt()) +
				ws + "ujp $$" + label_number + "\n" +
				"$$" + (label_number+1) + ws.substring(3, ws.length()) + "nop\n";
		
		newTexts.put(ctx, str);
	}

	@Override
	public void exitReturn_stmt(MiniGoParser.Return_stmtContext ctx) {
		String str = "";
		
		if (ctx.getChildCount() == 1) {//RETURN
			str += ws + "ret\n";
		}
		else if (ctx.getChildCount() == 2) {//RETURN expr
			str += newTexts.get(ctx.expr(0)) +
					ws + "retv\n";
		}
		else {//ctx.getChildCount() == 4 //RETURN expr ',' expr
			str += newTexts.get(ctx.expr(0)) +
					newTexts.get(ctx.expr(1)) +
					ws + "retv\n";
		}
		
		have_return_stmt = true;
		
		newTexts.put(ctx, str);
	}

	@Override
	public void exitLocal_decl(MiniGoParser.Local_declContext ctx) {
		
		if (ctx.getChildCount() == 3) {//VAR IDENT type_spec
			local_var_list.add(new Node(ctx.getChild(1).getText(), "2 " + (++local_var_number), false));
		}
		else {//ctx.getChildCount() == 6 //VAR IDENT '[' LITERAL ']' type_spec
			local_var_list.add(new Node(ctx.getChild(1).getText(), "2 " + (++local_var_number), true));
			local_array_number = Integer.parseInt(ctx.LITERAL().getText());
		}
		
		newTexts.put(ctx, "");
	}

	@Override
	public void exitExpr(MiniGoParser.ExprContext ctx) {
		String str = "";
		
		if (ctx.getChildCount() == 1) {
			
			if (ctx.getChild(0).equals(ctx.LITERAL(0))) {//LITERAL
				str += ws + "ldc " + ctx.LITERAL(0).getText() + "\n";
			}
			else {//IDENT
				Node n = findNode(ctx.getChild(0).getText());
				
				if (n.isArray == true) {
					str += ws + "lda " + n.num + "\n";
				}
				else {
					str += ws + "lod " + n.num + "\n";
				}
			}
		}
		
		else if (ctx.getChildCount() == 2) {//op expr
			str += newTexts.get(ctx.expr(0));
			String op = ctx.getChild(0).getText();
			
			Node n = findNode(ctx.getChild(1).getText());
			
			if (op.equals("-")) {
				str += ws + "neg\n";
			}
			else if (op.equals("--") ) {
				str += ws + "dec\n" +
						ws + "str " + n.num + "\n";
			}
			else if (op.equals("++")) {
				str += ws + "inc\n" +
						ws + "str " + n.num + "\n";
			}
			else if (op.equals("!")) {
				str += ws + "notop\n";
			}
		}
		
		else if (ctx.getChildCount() == 3) {
			if (ctx.getChild(1) != ctx.expr(0)) {
				String op = ctx.getChild(1).getText();
				
				if (ctx.getChild(1).getText().equals("=")) {//IDENT = expr
					Node n = findNode(ctx.getChild(0).getText());
					
					str += newTexts.get(ctx.expr(0)) +
							ws + "str " + n.num + "\n";
					
				}
				else if (!ctx.getChild(1).getText().equals("=")) {//expr op expr
//					str += newTexts.get(ctx.expr(0)) + newTexts.get(ctx.expr(1));
					
					if (ctx.expr(0).IDENT() != null) {
						Node n = findNode(ctx.getChild(0).getText());
						str += ws + "lod " + n.num + "\n";
					}
					else {//ctx.expr(0) == ctx.LITERAL()
						str += ws + "ldc " + ctx.getChild(0).getText() + "\n";
					}
					
					if (ctx.expr(1).IDENT() != null) {
						Node n = findNode(ctx.getChild(2).getText());
						str += ws + "lod " + n.num + "\n";
					}
					else {//ctx.expr(0) == ctx.LITERAL()
						str += ws + "ldc " + ctx.getChild(2).getText() + "\n";
					}
					
					if (op.equals("*")) {
						str += ws + "mult\n";
					}
					else if (op.equals("/")) {
						str += ws + "div\n";
					}
					else if (op.equals("%")) {
						str += ws + "mod\n";
					}
					else if (op.equals("+")) {
						str += ws + "add\n";
					}
					else if (op.equals("-")) {
						str += ws + "sub\n";
					}
					else if (op.equals("==")) {//EQ
						str += ws + "eq\n";
					}
					else if (op.equals("!=")) {//NE
						str += ws + "ne\n";
					}
					else if (op.equals("<=")) {//LE
						str += ws + "le\n";
					}
					else if (op.equals("<")) {
						str += ws + "lt\n";
					}
					else if (op.equals(">=")) {//GE
						str += ws + "ge\n";
					}
					else if (op.equals(">")) {
						str += ws + "gt\n";
					}
					else if (op.equals("and")) {
						str += ws + "and\n";
					}
					else if (op.equals("or")) {
						str += ws + "or\n";
					}
					else if (op.equals(",")) {//LITERAL , LITERAL
						str += ws + "ldc " + ctx.LITERAL(0).getText() + "\n" + 
								ws + "ldc " + ctx.LITERAL(1).getText() + "\n";
					}
				}
			}
			else {// '(' expr ')'
				str += newTexts.get(ctx.expr(0));
			}
		}
		
		else if (ctx.getChildCount() == 4) {
			if (ctx.getChild(1).getText().equals("(")) {//IDENT (args) //이 문법 못읽음
				str += ws + "ldp\n" + 
						newTexts.get(ctx.args()) +
						ws + "call " + ctx.getChild(0).getText() + "\n";
			}
			else {//IDENT [expr]
				Node n = findNode(ctx.IDENT().getText());
				
				str += newTexts.get(ctx.expr(0));
				
				if (n.isParam == false && n.isArgs == false) {
					str += ws + "lda ";
				}
				else {
					str += ws + "lod ";
				}
				
				str += n.num + "\n" +
						ws + "add\n" +
						ws + "ldi\n";
			}
		}
		
		else if (ctx.getChildCount() == 6) {
			if (ctx.getChild(0) == ctx.IDENT()) {//IDENT '[' expr ']' '=' expr
				Node n = findNode(ctx.IDENT().getText());
				
				str += newTexts.get(ctx.expr(0));
				
				if (n.isArgs == true) {
					str += ws + "lod " + n.num + "\n";
				}
				else {
					str += ws + "lda " + n.num + "\n";
				}
				
				str += ws + "add\n" + newTexts.get(ctx.expr(1)) + ws + "sti\n";
			}
			else {//FMT '.' IDENT '(' args ')'
				str += ws + "ldp\n" + 
						newTexts.get(ctx.args()) +
						ws + "call write\n";
			}
		}
		
		newTexts.put(ctx, str);
	}

	@Override
	public void exitArgs(MiniGoParser.ArgsContext ctx) {
		String str = "";
		
		for (int i = 0; i < ctx.expr().size(); i++) {
			str += newTexts.get(ctx.expr(i));
		}
		
		newTexts.put(ctx, str);
	}
}
