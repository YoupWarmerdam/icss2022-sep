package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.literals.BoolLiteral;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Evaluator implements Transform {

    private LinkedList<HashMap<String, Literal>> variableValues;

    public Evaluator() {
        variableValues = new LinkedList<>();
    }

    @Override
    public void apply(AST ast) {
        applyStyleSheet(ast.root);
    }

    private void applyStyleSheet(Stylesheet sheet) {
        variableValues.addFirst(new HashMap<>());
        List<ASTNode> nodesToRemove = new ArrayList<>();

        for (ASTNode child : sheet.getChildren()) {
            if (child instanceof Stylerule) {
                applyStylerule((Stylerule) child);
            } else if (child instanceof VariableAssignment) {
                applyVariableAssignment((VariableAssignment) child);
                nodesToRemove.add(child);
            }
        }
        for (ASTNode child : nodesToRemove) {
            sheet.removeChild(child);
        }
        variableValues.removeFirst();
    }

    private void applyStylerule(Stylerule rule) {
        variableValues.addFirst(new HashMap<>());
        applyBodyNodes(rule.body);
        variableValues.removeFirst();
    }

    private List<ASTNode> applyIfClause(IfClause ifClause) {
        Literal conditionResult = evaluateExpression(ifClause.conditionalExpression);
        boolean ifClauseIsTrue = ((BoolLiteral) conditionResult).value;

        if (ifClauseIsTrue) {
            applyBodyNodes(ifClause.body);
            return ifClause.body;
        } else if (ifClause.elseClause != null) {
            applyBodyNodes(ifClause.elseClause.body);
            return ifClause.elseClause.body;
        } else {
            return new ArrayList<>();
        }
    }

    private void applyBodyNodes(ArrayList<ASTNode> body) {
        List<ASTNode> nodesToRemove = new ArrayList<>();
        List<ASTNode> nodesToAdd = new ArrayList<>();
        
        for (ASTNode node : body) {
            if (node instanceof Declaration) {
                applyDeclaration((Declaration) node);
            } else if (node instanceof VariableAssignment) {
                applyVariableAssignment((VariableAssignment) node);
                nodesToRemove.add(node);
            } else if (node instanceof IfClause) {
                nodesToAdd.addAll(applyIfClause((IfClause) node));
                nodesToRemove.add(node);
            }
        }
        
        body.removeAll(nodesToRemove);
        body.addAll(nodesToAdd);
    }

    private void applyDeclaration(Declaration declaration) {
        declaration.expression = evaluateExpression(declaration.expression);
    }

    private void applyVariableAssignment(VariableAssignment assignment) {
        Literal literal = evaluateExpression(assignment.expression);
        variableValues.peek().put(assignment.name.name, literal);
        assignment.expression = literal;
    }

    private Literal evaluateExpression(Expression expression) {
        if (expression instanceof Literal) {
            return (Literal) expression;
        } else if (expression instanceof MultiplyOperation) {
            return evaluateMultiplyOperation((MultiplyOperation) expression);
        } else if (expression instanceof AddOperation) {
            return evaluateAddSubtractOperation((AddOperation) expression, true);
        } else if(expression instanceof  SubtractOperation) {
            return evaluateAddSubtractOperation((SubtractOperation) expression, false);
        } else if (expression instanceof VariableReference) {
            return evaluateVariableReference((VariableReference) expression);
        }
        return null;
    }

    private Literal evaluateMultiplyOperation(MultiplyOperation operation) {
        Literal leftside = evaluateExpression(operation.lhs);
        Literal rightside = evaluateExpression(operation.rhs);

        if (leftside instanceof ScalarLiteral && rightside instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) leftside).value * ((ScalarLiteral) rightside).value);
        }
        if (leftside instanceof ScalarLiteral) {
            Literal temp = leftside;
            leftside = rightside;
            rightside = temp;
        }
        if (rightside instanceof ScalarLiteral) {
            if (leftside instanceof PixelLiteral) {
                return new PixelLiteral(((PixelLiteral) leftside).value * ((ScalarLiteral) rightside).value);
            } else if (leftside instanceof PercentageLiteral) {
                return new PercentageLiteral(((PercentageLiteral) leftside).value * ((ScalarLiteral) rightside).value);
            }
        }
        return null;
    }

    private Literal evaluateAddSubtractOperation(Operation operation, boolean isAdd) {
        Literal leftside = evaluateExpression(operation.lhs);
        Literal rightside = evaluateExpression(operation.rhs);

        if (leftside instanceof PixelLiteral && rightside instanceof PixelLiteral) {
            int result = isAdd
                    ? ((PixelLiteral) leftside).value + ((PixelLiteral) rightside).value
                    : ((PixelLiteral) leftside).value - ((PixelLiteral) rightside).value;
            return new PixelLiteral(result);
        } else if (leftside instanceof PercentageLiteral && rightside instanceof PercentageLiteral) {
            int result = isAdd
                    ? ((PercentageLiteral) leftside).value + ((PercentageLiteral) rightside).value
                    : ((PercentageLiteral) leftside).value - ((PercentageLiteral) rightside).value;
            return new PercentageLiteral(result);
        }
        return null;
    }

    private Literal evaluateVariableReference(VariableReference reference) {
        for (HashMap<String, Literal> variableValue : variableValues) {
            if (variableValue.containsKey(reference.name)) {
                return variableValue.get(reference.name);
            }
        }
        return null;
    }
}
