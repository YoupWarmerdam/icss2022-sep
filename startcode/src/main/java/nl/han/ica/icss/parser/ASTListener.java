package nl.han.ica.icss.parser;

import java.util.Stack;

import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {
	
	//Accumulator attributes:
	private AST ast;

	//Use this to keep track of the parent nodes when recursively traversing the ast
	private IHANStack<ASTNode> currentContainer;

	public ASTListener() {
		ast = new AST();
		currentContainer = new HANStack<>();
	}
    public AST getAST() {
        return ast;
    }

	@Override
	public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
		Stylesheet stylesheet = new Stylesheet();
		currentContainer.push(stylesheet);
	}

	@Override
	public void exitStylesheet(ICSSParser.StylesheetContext ctx) {
		Stylesheet stylesheet = (Stylesheet) currentContainer.pop();
		ast.setRoot(stylesheet);
	}

	@Override
	public void enterStylerule(ICSSParser.StyleruleContext ctx) {
		Stylerule stylerule = new Stylerule();
		currentContainer.push(stylerule);
	}

	@Override
	public void exitStylerule(ICSSParser.StyleruleContext ctx) {
		Stylerule stylerule = (Stylerule) currentContainer.pop();
		currentContainer.peek().addChild(stylerule);
	}

	@Override
	public void exitSelector(ICSSParser.SelectorContext ctx) {
		Selector selector;
		if (ctx.LOWER_IDENT() != null) {
			selector = new TagSelector(ctx.LOWER_IDENT().getText());
		} else if (ctx.ID_IDENT() != null) {
			selector = new IdSelector(ctx.ID_IDENT().getText());
		} else {
			selector = new ClassSelector(ctx.CLASS_IDENT().getText());
		}
		currentContainer.peek().addChild(selector);
	}

	@Override
	public void enterVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
		VariableAssignment variableAssignment = new VariableAssignment();
		variableAssignment.name = new VariableReference(ctx.CAPITAL_IDENT().getText());
		currentContainer.push(variableAssignment);
	}

	@Override
	public void exitVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
		VariableAssignment variableAssignment = (VariableAssignment) currentContainer.pop();
		currentContainer.peek().addChild(variableAssignment);
	}

	@Override
	public void enterDeclaration(ICSSParser.DeclarationContext ctx) {
		Declaration declaration = new Declaration();
		currentContainer.push(declaration);
	}

	@Override
	public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
		Declaration declaration = (Declaration) currentContainer.pop();
		declaration.property = new PropertyName(ctx.LOWER_IDENT().getText());
		currentContainer.peek().addChild(declaration);
	}

	@Override
	public void enterPlusMinExpressie(ICSSParser.PlusMinExpressieContext ctx) {
		if (ctx.getChild(1).getText().equals("+")) {
			currentContainer.push(new AddOperation());
		} else if (ctx.getChild(1).getText().equals("-")) {
			currentContainer.push(new SubtractOperation());
		}
	}

	@Override
	public void exitPlusMinExpressie(ICSSParser.PlusMinExpressieContext ctx) {
		Operation operation = (Operation) currentContainer.pop();
		currentContainer.peek().addChild(operation);
	}

	@Override
	public void enterMulExpressie(ICSSParser.MulExpressieContext ctx) {
		currentContainer.push(new MultiplyOperation());
	}

	@Override
	public void exitMulExpressie(ICSSParser.MulExpressieContext ctx) {
		Operation operation = (Operation) currentContainer.pop();
		currentContainer.peek().addChild(operation);
	}

	@Override
	public void exitLiteral(ICSSParser.LiteralContext ctx) {
		Expression expression;
		if (ctx.COLOR() != null) {
			expression = new ColorLiteral(ctx.COLOR().getText());
		} else if (ctx.PIXELSIZE() != null) {
			expression = new PixelLiteral(ctx.PIXELSIZE().getText());
		} else if (ctx.PERCENTAGE() != null) {
			expression = new PercentageLiteral(ctx.PERCENTAGE().getText());
		} else if (ctx.SCALAR() != null) {
			expression = new ScalarLiteral(ctx.SCALAR().getText());
		} else if (ctx.TRUE() != null) {
			expression = new BoolLiteral(true);
		} else if (ctx.FALSE() != null) {
			expression = new BoolLiteral(false);
		} else {
			expression = new VariableReference(ctx.CAPITAL_IDENT().getText());
		}
		currentContainer.peek().addChild(expression);
	}
}
