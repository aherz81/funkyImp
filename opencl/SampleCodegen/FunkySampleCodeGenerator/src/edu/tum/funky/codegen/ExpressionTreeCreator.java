/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.tum.funky.codegen;

import edu.tum.funky.codegen.expression.Expression;
import edu.tum.funky.codegen.expression.LiteralExpression;
import edu.tum.funky.codegen.expression.RandomArrayAccessExpression;
import edu.tum.funky.codegen.expression.IdentifierExpression;
import edu.tum.funky.codegen.expression.BinaryOperationExpression;
import edu.tum.funky.codegen.expression.LocalArrayAccessExpression;
import edu.tum.funky.codegen.expression.SimpleArrayAccessExpression;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 *
 * @author Alexander Pöppl
 */
public class ExpressionTreeCreator {

    private static final List<String> identifiers = Arrays.asList("x", "y");
    private static final List<String> operators = Arrays.asList("+", "*", "/", "-");
    private static final List<String> domains = Arrays.asList("matrix");
    
    
    private final CodeGeneratorConfiguration config;
    private final Random rand;
    private final LiteralExpression.LiteralType literalType;
    private int nodes;

    public ExpressionTreeCreator(LiteralExpression.LiteralType literalType, CodeGeneratorConfiguration config) {
        this.rand = new Random();
        this.literalType = literalType;
        this.nodes = 0;
        this.config = config;
    }

    public Optional<Expression> createTree() {
        this.nodes = 0;
        try {
            return Optional.of(createTreeInternal().simplify());
        } catch (IllegalStateException ex) {
            return Optional.empty();
        }
    }
    
    /**
     * Generates random Expression trees. This is done recursively. With a random number
     * @return a newly generated Expression tree with less than 300 nodes.
     * 
     */
    private Expression createTreeInternal() {
        if (nodes++ > 300) {
            throw new IllegalStateException("Too many nodes.");
        } else {
            double randomNumber = rand.nextDouble();
            if (randomNumber < 0.5) {
                return getBinary();
            } else if (randomNumber < 0.6) {
                return getLiteral();
            } else if (randomNumber < 0.7) {
                return getIdentifier();
            } else if (randomNumber < 0.8) {
                if (literalType == LiteralExpression.LiteralType.INT) {
                    return getIdentifier();
                } else {
                    return new RandomArrayAccessExpression(config);
                }
            } else if (randomNumber < 0.9) {
                if (literalType == LiteralExpression.LiteralType.INT) {
                    return getIdentifier();
                } else {
                    return new LocalArrayAccessExpression(config);
                }
            } else {
                if (literalType == LiteralExpression.LiteralType.INT) {
                    return getLiteral();
                } else {
                    return new SimpleArrayAccessExpression();
                }
            }
        }

    }
    
    private Expression getIdentifier() {
        String ident = identifiers.get(Math.round(rand.nextFloat()));
        return new IdentifierExpression(ident);
    }

    private Expression getBinary() {
        String op;
        do {
            int opNum = rand.nextInt(4);
            op = operators.get(opNum);
            
        } while ((this.literalType == LiteralExpression.LiteralType.INT && (op.equals("-") || op.equals("/"))) ||
                 (this.literalType == LiteralExpression.LiteralType.FLOAT && !config.isFloatDivisionAllowed() && op.equals("/")));
        
        Expression left = createTreeInternal();
        Expression right = createTreeInternal();
        return new BinaryOperationExpression(left, right, op, literalType);
    }

    private LiteralExpression getLiteral() {
        if (this.literalType == LiteralExpression.LiteralType.FLOAT) {
            return new LiteralExpression(LiteralExpression.LiteralType.FLOAT, (float) rand.nextDouble() * 1000.f);
        } else {
            return new LiteralExpression(LiteralExpression.LiteralType.INT, rand.nextInt(1024));
        }
    }
}
