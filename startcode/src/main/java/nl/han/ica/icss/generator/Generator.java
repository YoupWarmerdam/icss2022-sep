package nl.han.ica.icss.generator;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.selectors.*;

import java.util.List;

public class Generator {

	public String generate(AST ast) {
        return generateStylesheet(ast.root);
	}

	private String generateStylesheet(Stylesheet sheet) {
		StringBuilder stylesheet = new StringBuilder();
		for (ASTNode child : sheet.getChildren()) {
			if (child instanceof Stylerule) {
				stylesheet.append(generateStylerule((Stylerule) child)).append("\n");
			}
		}
		return stylesheet.toString();
	}

	private String generateStylerule(Stylerule rule) {
		StringBuilder stylerule = new StringBuilder();
		stylerule.append(generateSelector(rule.selectors));

		for (ASTNode child : rule.body) {
			if (child instanceof Declaration) {
				// Two spaces per scope level (GE02 requirement)
				stylerule.append("  ").append(generateDeclaration((Declaration) child)).append("\n");
			}
		}
		return stylerule.append("}").toString();
	}

	private String generateSelector(List<Selector> selector) {
		StringBuilder selectors = new StringBuilder();
		for (Selector s : selector) {
			if (s instanceof TagSelector) {
				selectors.append(((TagSelector) s).tag);
			} else if (s instanceof ClassSelector) {
				selectors.append(((ClassSelector) s).cls);
			} else if (s instanceof IdSelector) {
				selectors.append(((IdSelector) s).id);
			}
		}
		return selectors.append("{\n").toString();
	}

	private String generateDeclaration(Declaration declaration) {
		return declaration.property.name + ": " + generateLiteral((Literal) declaration.expression) + ";";
	}

	private String generateLiteral(Literal expression) {
		if (expression instanceof ColorLiteral) {
			return ((ColorLiteral) expression).value;
		} else if (expression instanceof PercentageLiteral) {
			return ((PercentageLiteral) expression).value + "%";
		} else if (expression instanceof PixelLiteral) {
			return ((PixelLiteral) expression).value + "px";
		} else if (expression instanceof ScalarLiteral) {
			return String.valueOf(((ScalarLiteral) expression).value);
		} else {
			return null;
		}
	}
}
