/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework
 * <p>
 * Copyright (C) 2013 Yoav Artzi
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 ******************************************************************************/
package edu.uw.cs.lil.tiny.genlex.ccg.unification.split;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.categories.ComplexCategory;
import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.ComplexSyntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Slash;
import edu.uw.cs.lil.tiny.genlex.ccg.unification.split.SplittingServices.SplittingPair;
import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.AllSubExpressions;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.GetAllFreeVariables;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ReplaceExpression;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.ReplaceNthExpression;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.Simplify;
import edu.uw.cs.lil.tiny.mr.language.type.RecursiveComplexType;
import edu.uw.cs.lil.tiny.utils.PowerSet;
import edu.uw.cs.utils.collections.CollectionUtils;
import edu.uw.cs.utils.counter.Counter;
import edu.uw.cs.utils.log.ILogger;
import edu.uw.cs.utils.log.LoggerFactory;

/**
 * Do higher-order unification splits. X : h ==> X/Y : f Y : g s.g. f(g)=h (and
 * the backwards case also)
 * 
 * @author Luke Zettlemoyer
 */
public class MakeApplicationSplits {
	
	private static final ILogger	LOG	= LoggerFactory
												.create(MakeApplicationSplits.class);
	
	private MakeApplicationSplits() {
	}
	
	public static Set<SplittingPair> createAllSplittingPairs(
			Category<LogicalExpression> rootCategory,
			LogicalExpression functor, LogicalExpression functee,
			ICategoryServices<LogicalExpression> categoryServices) {
		
		// Simplify the functee and functor
		final LogicalExpression simplifiedFunctor = Simplify.of(functor);
		final LogicalExpression simplifiedFunctee = Simplify.of(functee);
		
		// make the category that will be pulled out
		final Category<LogicalExpression> functeeCategory = makeCategoryFor(simplifiedFunctee);
		
		// now, make the categories. the split can be either a forward
		// application or a backward application
		final SplittingPair forwardSplit;
		final ComplexCategory<LogicalExpression> forwardCategory = new ComplexCategory<LogicalExpression>(
				new ComplexSyntax(rootCategory.getSyntax(),
						functeeCategory.getSyntax(), Slash.FORWARD),
				simplifiedFunctor);
		forwardSplit = new SplittingPair(forwardCategory, functeeCategory);
		
		// error check. do the two pieces recombine to make the original
		// category?
		
		final Category<LogicalExpression> applyCat = categoryServices.apply(
				forwardCategory, functeeCategory);
		if (!rootCategory.equals(applyCat)) {
			LOG.error("ERROR: bad forward split");
			LOG.error("%s --> %s == %s != %s", forwardCategory,
					functeeCategory, applyCat, rootCategory);
		}
		
		final SplittingPair backSplit;
		final ComplexCategory<LogicalExpression> backCategory = new ComplexCategory<LogicalExpression>(
				new ComplexSyntax(rootCategory.getSyntax(),
						functeeCategory.getSyntax(), Slash.BACKWARD),
				simplifiedFunctor);
		backSplit = new SplittingPair(functeeCategory, backCategory);
		
		// error check. do the two pieces recombine to make the original
		// category?
		final Category<LogicalExpression> applyCat2 = categoryServices.apply(
				backCategory, functeeCategory);
		if (!rootCategory.equals(applyCat2)) {
			LOG.error("ERROR: bad backward split");
			LOG.error("%s --> %s == %s != %s", functeeCategory, backCategory,
					applyCat2, rootCategory);
		}
		
		final Set<SplittingPair> newSplits = new HashSet<SplittingServices.SplittingPair>();
		newSplits.add(forwardSplit);
		newSplits.add(backSplit);
		return newSplits;
	}
	
	/**
	 * Replace subExpression in originalExpression with replacementExpression
	 * and wrap the result with a Lambda operator with the variable newVariable.
	 * 
	 * @param originalExpression
	 * @param subExpression
	 *            Assumed to be a sub-expression of originalExpression.
	 * @param replacementExpression
	 * @param newVariable
	 *            replacementExpression already contains this variable.
	 * @param index
	 *            if 'null', will replace all occurrences of subExpression in
	 *            originalExpression. Otherwise will replace the n-th occurrence
	 *            only.
	 * @return
	 */
	private static LogicalExpression createFunctor(
			LogicalExpression originalExpression,
			LogicalExpression subExpression,
			LogicalExpression replacementExpression, Variable newVariable,
			Integer index) {
		final LogicalExpression newBody;
		if (index == null) {
			// Case replace all
			newBody = ReplaceExpression.of(originalExpression, subExpression,
					replacementExpression);
		} else {
			// Case replace only a single occurrence
			newBody = ReplaceNthExpression.of(originalExpression,
					subExpression, replacementExpression, index);
		}
		return new Lambda(newVariable, newBody);
	}
	
	/**
	 * Handle literals with order-insensitive recursive predicates.
	 * 
	 * @param originalCategory
	 * @param subExpression
	 *            The literal we currently considering, assumed to be a
	 *            sub-expression of originalCategory.getSem()
	 * @param count
	 *            The number of times this literal appears in
	 *            originalCategory.getSem()
	 * @param categoryServices
	 * @return
	 */
	private static Set<SplittingPair> doOrderInsensitiveSplits(
			Category<LogicalExpression> originalCategory,
			Literal subExpression, int count,
			ICategoryServices<LogicalExpression> categoryServices,
			int minSubsetSize) {
		
		final Set<SplittingPair> newSplits = new HashSet<SplittingServices.SplittingPair>();
		// Copy the arguments due to the arguments list generically extending
		// LogicalExpression
		final List<LogicalExpression> args = new LinkedList<LogicalExpression>(
				subExpression.getArguments());
		
		// Iterate over all subsets of arguments
		for (final List<LogicalExpression> argumentsSubset : new PowerSet<LogicalExpression>(
				args)) {
			final int subsetSize = argumentsSubset.size();
			if (subsetSize >= minSubsetSize
					&& subsetSize < SplittingServices.MAX_NUM_SUBS
					&& subsetSize < args.size()) {
				// Case the subset is not the complete list of arguments, not
				// only a single argument (will be handled in a separate place)
				// and is under the maximum subset size
				
				// Create the extracted literal, by using the subset of
				// arguments and the predicate of the original subExpression
				// literal
				final Literal extractedLiteral = new Literal(
						subExpression.getPredicate(),
						new ArrayList<LogicalExpression>(argumentsSubset));
				
				// Get the set of all free variables inside the extracted
				// literal, so we can tie them with Lambda operators
				final Set<Variable> freeVars = GetAllFreeVariables
						.of(extractedLiteral);
				
				if (freeVars.size() <= SplittingServices.MAX_NUM_VARS) {
					// Case the number of free variables is below the maximum
					// threshold
					
					// Iterate over all possible orders of the variables
					for (final List<Variable> variablesOrder : SplittingServices
							.allOrders(freeVars)) {
						
						// Wrap the extracted literal with Lambda operators to
						// tie all the free variables
						final LogicalExpression newFunctee = SplittingServices
								.makeExpression(variablesOrder,
										extractedLiteral);
						
						// Create the new variables that will replace the
						// extracted subset of arguments inside the original
						// expression
						final Variable newVariable = new Variable(
								LogicLanguageServices.getTypeRepository()
										.generalizeType(newFunctee.getType()));
						
						// Create the literal/variable that will take the place
						// of the extracted subset within the literal
						final LogicalExpression newLiteralArgument = SplittingServices
								.makeAssignment(variablesOrder, newVariable);
						
						// Construct the new literal by collecting all arguments
						// that are left behind and appending the list with
						// newLiteralArgument
						final List<LogicalExpression> leftBehindArguments = new LinkedList<LogicalExpression>(
								args);
						for (final LogicalExpression gone : argumentsSubset) {
							// Remove the arguments that are in the subset.
							// Takes into account the possibility of repeats, so
							// only removes a single argument for each argument
							// in the subset.
							leftBehindArguments.remove(gone);
						}
						leftBehindArguments.add(newLiteralArgument);
						final Literal replacingLiteral = new Literal(
								subExpression.getPredicate(),
								leftBehindArguments);
						
						// Create the functor and create the syntax and finally
						// all possible splitting pairs. First: replace all
						// occurrences
						newSplits.addAll(createAllSplittingPairs(
								originalCategory,
								createFunctor(originalCategory.getSem(),
										subExpression, replacingLiteral,
										newVariable, null), newFunctee,
								categoryServices));
						if (count > 1) {
							// Case the subExpression appears more than once, so
							// in addition to
							// replacing all occurrences, also replace each
							// single occurrence.
							// Not trying to replace all possible subsets of
							// occurrences to keep
							// the process tractable.
							for (int i = 0; i < count; i++) {
								newSplits.addAll(createAllSplittingPairs(
										originalCategory,
										createFunctor(
												originalCategory.getSem(),
												subExpression,
												replacingLiteral, newVariable,
												i), newFunctee,
										categoryServices));
							}
						}
					}
				}
			}
		}
		return newSplits;
	}
	
	/**
	 * Handle literals with order-sensitive recursive predicates.
	 * 
	 * @param originalCategory
	 * @param literal
	 *            The literal we currently considering, assumed to be a
	 *            sub-expression of originalCategory.getSem()
	 * @param count
	 *            The number of times this literal appears in
	 *            originalCategory.getSem()
	 * @param categoryServices
	 * @return
	 */
	private static Set<SplittingPair> doOrderSensitiveSplits(
			Category<LogicalExpression> originalCategory, Literal literal,
			int count, ICategoryServices<LogicalExpression> categoryServices,
			int minSpanLength) {
		final Set<SplittingPair> newSplits = new HashSet<SplittingServices.SplittingPair>();
		// Copy the arguments due to the arguments list generically extending
		// LogicalExpression
		final List<LogicalExpression> args = new LinkedList<LogicalExpression>(
				literal.getArguments());
		
		// Iterate over all span lengths
		for (int length = minSpanLength; length <= SplittingServices.MAX_NUM_SUBS; length++) {
			// Iterate over all spans of the given length
			for (int begin = 0; begin < args.size() - length; begin++) {
				// The current span of arguments
				final List<LogicalExpression> argumentsSublist = CollectionUtils
						.subList(args, begin, begin + length);
				
				// Create the extracted literal, by using the subset of
				// arguments and the predicate of the original subExpression
				// literal
				final Literal extractedLiteral = new Literal(
						literal.getPredicate(),
						new ArrayList<LogicalExpression>(argumentsSublist));
				
				// Get the set of all free variables inside the extracted
				// literal, so we can tie them with Lambda operators
				final Set<Variable> freeVars = GetAllFreeVariables
						.of(extractedLiteral);
				
				if (freeVars.size() <= SplittingServices.MAX_NUM_VARS) {
					// Case the number of free variables is below the maximum
					// threshold
					
					// Iterate over all possible orders of the variables
					for (final List<Variable> variablesOrder : SplittingServices
							.allOrders(freeVars)) {
						
						// Wrap the extracted literal with Lambda operators to
						// tie all the free variables
						final LogicalExpression newFunctee = SplittingServices
								.makeExpression(variablesOrder,
										extractedLiteral);
						
						// Create the new variables that will replace the
						// extracted subset of arguments inside the original
						// expression
						final Variable newVariable = new Variable(
								LogicLanguageServices.getTypeRepository()
										.generalizeType(newFunctee.getType()));
						
						// Create the literal/variable that will take the place
						// of the extracted subset within the literal
						final LogicalExpression newLiteralArgument = SplittingServices
								.makeAssignment(variablesOrder, newVariable);
						
						// Construct the new literal by collecting all arguments
						// that are left behind and appending the list with
						// newLiteralArgument
						final List<LogicalExpression> leftBehindArguments = new LinkedList<LogicalExpression>(
								args);
						for (int i = 0; i < length; i++) {
							leftBehindArguments.remove(begin);
						}
						leftBehindArguments.add(begin, newLiteralArgument);
						final Literal replacingLiteral = new Literal(
								literal.getPredicate(), leftBehindArguments);
						
						// Create the functor and create the syntax and finally
						// all possible splitting pairs. First: replace all
						// occurrences
						newSplits.addAll(createAllSplittingPairs(
								originalCategory,
								createFunctor(originalCategory.getSem(),
										literal, replacingLiteral, newVariable,
										null), newFunctee, categoryServices));
						if (count > 1) {
							// Case the subExpression appears more than once, so
							// in addition to
							// replacing all occurrences, also replace each
							// single occurrence.
							// Not trying to replace all possible subsets of
							// occurrences to keep
							// the process tractable.
							for (int i = 0; i < count; i++) {
								newSplits.addAll(createAllSplittingPairs(
										originalCategory,
										createFunctor(
												originalCategory.getSem(),
												literal, replacingLiteral,
												newVariable, i), newFunctee,
										categoryServices));
							}
						}
					}
				}
			}
		}
		return newSplits;
	}
	
	/**
	 * @param subExpression
	 *            The sub expression we are pulling out
	 * @param argumentOrder
	 *            The order of variables inside the sub expression (this gives
	 *            the order of the lambdas that we put on the outside). This is
	 *            a list of variables that are inside subExpression.
	 * @param originalCategory
	 *            The current category we are splitting. Contains subExpression.
	 * @param splits
	 *            The list of splits. We add the result to it.
	 * @param index
	 * @param categoryServices
	 * @return
	 */
	static private Set<SplittingPair> doSplits(LogicalExpression subExpression,
			List<Variable> argumentOrder,
			Category<LogicalExpression> originalCategory, int count,
			ICategoryServices<LogicalExpression> categoryServices) {
		
		// The logical expression we will split
		final LogicalExpression originalExpression = originalCategory.getSem();
		
		// Make the sub logical expression that will be pulled out by
		// adding arguments for all of the free variables it
		// contains (if any) in the order specified by argumentOrder
		final LogicalExpression newFunctee = SplittingServices.makeExpression(
				argumentOrder, subExpression);
		
		// Make the function that, when applied to newFunctee, will
		// recreate the rootExpression. Use the general type so we will always
		// use only 'e' and 't' for typing the lambda operator we add around the
		// root category.
		final Variable newVariable = new Variable(LogicLanguageServices
				.getTypeRepository().generalizeType(newFunctee.getType()));
		
		// The expression that we will replace the sub-expression we are pulling
		// out. newVar is going to be the predicate (or the constant, if the
		// list of variables is empty).
		final LogicalExpression replacementExpression = SplittingServices
				.makeAssignment(argumentOrder, newVariable);
		
		// All splitting pairs
		final Set<SplittingPair> splittingPairs = new HashSet<SplittingServices.SplittingPair>();
		
		// Create the new functor and create all syntax options (forward
		// application, backward application)
		// and add the complete categories to the list of all splits.
		splittingPairs.addAll(createAllSplittingPairs(
				originalCategory,
				createFunctor(originalExpression, subExpression,
						replacementExpression, newVariable, null), newFunctee,
				categoryServices));
		
		createFunctor(originalExpression, subExpression, replacementExpression,
				newVariable, null);
		
		if (count > 1) {
			// Case the subExpression appears more than once, so in addition to
			// replacing all occurrences, also replace each single occurrence.
			// Not trying to replace all possible subsets of occurrences to keep
			// the process tractable.
			for (int i = 0; i < count; i++) {
				splittingPairs.addAll(createAllSplittingPairs(
						originalCategory,
						createFunctor(originalExpression, subExpression,
								replacementExpression, newVariable, i),
						newFunctee, categoryServices));
			}
		}
		
		return splittingPairs;
	}
	
	static protected Category<LogicalExpression> makeCategoryFor(
			LogicalExpression input) {
		return Category.create(SplittingServices.typeToSyntax(input.getType()),
				input);
	}
	
	static Set<SplittingPair> of(Category<LogicalExpression> originalCategory,
			ICategoryServices<LogicalExpression> categoryServices) {
		// Get all sub-expressions and predicates
		final Map<LogicalExpression, Counter> subExpressions = new HashMap<LogicalExpression, Counter>();
		
		for (final LogicalExpression subExpression : AllSubExpressions
				.of(originalCategory.getSem())) {
			if (subExpressions.containsKey(subExpression)) {
				subExpressions.get(subExpression).inc();
			} else {
				subExpressions.put(subExpression, new Counter(1));
			}
		}
		
		// Accumulates all possible splits
		final Set<SplittingPair> splits = new HashSet<SplittingServices.SplittingPair>();
		
		// Iterate over the sub-expressions, generating splits from each one in
		// turn. Also handle special cases (See upper part of the loop).
		for (final Map.Entry<LogicalExpression, Counter> entry : subExpressions
				.entrySet()) {
			final LogicalExpression subExpression = entry.getKey();
			final int count = entry.getValue().value();
			
			if (LogicLanguageServices.isCoordinationPredicate(subExpression)) {
				// Skip extracting coordination predicates
				continue;
			}
			
			if (subExpression instanceof Literal) {
				// Case Literal, so try the various special cases
				final Literal literal = (Literal) subExpression;
				
				if (literal.getPredicate() instanceof Variable) {
					// If the predicate is a variable, we completely skip
					// processing it (we might still extract its arguments)
					continue;
				} else if (literal.getPredicateType() instanceof RecursiveComplexType
						&& LogicLanguageServices.isCollpasiblePredicate(literal
								.getPredicate())) {
					// If this the predicate is a recursive predicate, handle it
					// with respect to its order sensitivity. Here we handle
					// with pulling out subsets of the arguments, but not the
					// entire literal
					final int minArgs = ((RecursiveComplexType) literal
							.getPredicateType()).getMinArgs();
					if (literal.getPredicateType().isOrderSensitive()) {
						// Case order sensitive
						splits.addAll(doOrderSensitiveSplits(originalCategory,
								literal, count, categoryServices, minArgs));
					} else {
						// Case order insensitive
						splits.addAll(doOrderInsensitiveSplits(
								originalCategory, literal, count,
								categoryServices, minArgs));
					}
				}
			}
			
			final Set<Variable> freeVars = GetAllFreeVariables
					.of(subExpression);
			if (freeVars.size() <= SplittingServices.MAX_NUM_VARS) {
				// Do all possible orderings of variables, if there are more
				// than the limit of variables, just ignore this split
				
				for (final List<Variable> order : SplittingServices
						.allOrders(freeVars)) {
					// Extract the sub-expression with the given order of
					// variables. Also handles repeating sub-expressions.
					splits.addAll(doSplits(subExpression, order,
							originalCategory, count, categoryServices));
				}
			}
			
		}
		
		return splits;
	}
	
}
