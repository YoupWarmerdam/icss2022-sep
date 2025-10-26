package nl.han.ica.icss.checker;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.HashMap;
import java.util.LinkedList;

public class Checker {

    private LinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
         variableTypes = new LinkedList<>();
         checkStylesheet(ast.root);
    }


    private void checkStylesheet(Stylesheet sheet) {
        variableTypes.addFirst(new HashMap<>());
        for (ASTNode child : sheet.getChildren()) {
            if (child instanceof Stylerule) {
                checkStylerule((Stylerule) child);
            } else if (child instanceof VariableAssignment) {
                checkVariableAssignment((VariableAssignment) child);
            } else {
                child.setError("Unknown type: not a stylerule or variable assignment");
            }
        }
        variableTypes.removeFirst();
    }

    private void checkStylerule(Stylerule rule) {
        variableTypes.addFirst(new HashMap<>());
        for (ASTNode child : rule.body) {
            if (child instanceof VariableAssignment) {
                checkVariableAssignment((VariableAssignment) child);
            } else if (child instanceof Declaration) {
                checkDeclaration((Declaration) child);
            } else if (child instanceof IfClause) {
                checkIfClause((IfClause) child);
            } else {
                child.setError("Unknown type: not a variable assignment, declaration or if-clause");
            }
        }
        variableTypes.removeFirst();
    }

    private void checkDeclaration(Declaration declaration) {
        ExpressionType expressionType = checkExpression(declaration.expression);
        switch (declaration.property.name) {
            case "background-color":
                if (expressionType != ExpressionType.COLOR) {
                    declaration.setError("Only color expressions are allowed for background-color");
                }
                break;
            case "color":
                if (expressionType != ExpressionType.COLOR) {
                    declaration.setError("Only color expressions are allowed for color");
                }
                break;
            case "width":
                if (expressionType != ExpressionType.PIXEL && expressionType != ExpressionType.PERCENTAGE) {
                    declaration.setError("Only pixel and percentage expressions are allowed for width");
                }
                break;
            case "height":
                if (expressionType != ExpressionType.PIXEL) {
                    declaration.setError("Only pixel expressions are allowed for height");
                }
                break;
            default:
                declaration.setError("Unknown property: " + declaration.property.name);
        }
    }

    private void checkIfClause(IfClause ifClause) {
        variableTypes.addFirst(new HashMap<>());
        ExpressionType conditionType = checkExpression(ifClause.conditionalExpression);
        if(conditionType != ExpressionType.BOOL) {
            ifClause.conditionalExpression.setError("If condition must be a boolean expression");
        }

        for (ASTNode child : ifClause.body) {
            if (child instanceof VariableAssignment) {
                checkVariableAssignment((VariableAssignment) child);
            } else if (child instanceof Declaration) {
                checkDeclaration((Declaration) child);
            } else if (child instanceof IfClause) {
                checkIfClause((IfClause) child);
            } else if (child instanceof ElseClause) {
                checkElseClause((ElseClause) child);
            } else {
                child.setError("An if-clause must include only declarations, variable assignments, if-clauses, or else-clauses");
            }
        }
        variableTypes.removeFirst();
    }

    private void checkElseClause(ElseClause elseClause) {
        variableTypes.addFirst(new HashMap<>());
        for (ASTNode child : elseClause.body) {
            if (child instanceof VariableAssignment) {
                checkVariableAssignment((VariableAssignment) child);
            } else if (child instanceof Declaration) {
                checkDeclaration((Declaration) child);
            } else if (child instanceof IfClause) {
                checkIfClause((IfClause) child);
            } else {
                child.setError("An else-clause must include only declarations, variable assignments or if-clauses");
            }
        }
        variableTypes.removeFirst();
    }

    private void checkVariableAssignment(VariableAssignment assignment) {
        ExpressionType type = checkExpression(assignment.expression);
        variableTypes.getFirst().put(assignment.name.name, type);
    }

    private ExpressionType checkVariableReference(VariableReference reference) {
        for (HashMap<String, ExpressionType> scope : variableTypes) {
            if (scope.containsKey(reference.name)) {
                return scope.get(reference.name);
            }
        }
        reference.setError("Variable " + reference.name + " is not defined");
        return ExpressionType.UNDEFINED;
    }

    private ExpressionType checkExpression(Expression expression) {
        if (expression instanceof VariableReference) {
            return checkVariableReference((VariableReference) expression);
        } else if (expression instanceof Literal) {
            return getLiteralType((Literal) expression);
        } else if (expression instanceof Operation) {
            return checkOperation((Operation) expression);
        }
        return ExpressionType.UNDEFINED;
    }

    private ExpressionType checkOperation(Operation operation) {
        for (ASTNode child : operation.getChildren()) {
            if (child instanceof ColorLiteral) {
                child.setError("Color literals are not allowed in operations");
                return ExpressionType.UNDEFINED;
            } else if (child instanceof BoolLiteral) {
                child.setError("Boolean literals are not allowed in operations");
                return ExpressionType.UNDEFINED;
            }
        }

        if (operation instanceof AddOperation || operation instanceof SubtractOperation) {
            return checkAddSubtractOperation(operation);
        } else if (operation instanceof MultiplyOperation) {
            return checkMultiplyOperation((MultiplyOperation) operation);
        } else {
            operation.setError("Unknown operation");
            return ExpressionType.UNDEFINED;
        }
    }

    private ExpressionType checkAddSubtractOperation(Operation operation) {
        ExpressionType leftside = checkExpression(operation.lhs);
        ExpressionType rightside = checkExpression(operation.rhs);

        if (leftside == rightside) {
            return leftside;
        } else {
            operation.setError("Operands must be of the same type");
            return ExpressionType.UNDEFINED;
        }
    }

    private ExpressionType checkMultiplyOperation(MultiplyOperation operation) {
        ExpressionType leftside = checkExpression(operation.lhs);
        ExpressionType rightside = checkExpression(operation.rhs);

        if (leftside != ExpressionType.SCALAR && rightside != ExpressionType.SCALAR) {
            operation.setError(
                    "Multiply operation can only be used with an expression of type scalar and an expression of another type");
            return ExpressionType.UNDEFINED;
        }
        return (leftside == ExpressionType.SCALAR) ? rightside : leftside;
    }

    private ExpressionType getLiteralType(Literal literal) {
        if (literal instanceof BoolLiteral) return ExpressionType.BOOL;
        if (literal instanceof ColorLiteral) return ExpressionType.COLOR;
        if (literal instanceof ScalarLiteral) return ExpressionType.SCALAR;
        if (literal instanceof PixelLiteral) return ExpressionType.PIXEL;
        if (literal instanceof PercentageLiteral) return ExpressionType.PERCENTAGE;
        return ExpressionType.UNDEFINED;
    }
}
