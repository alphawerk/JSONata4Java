/**
 * (c) Copyright 2018, 2019 IBM Corporation
 * 1 New Orchard Road, 
 * Armonk, New York, 10504-1722
 * United States
 * +1 914 499 1900
 * support: Nathaniel Mills wnm3@us.ibm.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.api.jsonata4java.expressions.functions;

import com.api.jsonata4java.expressions.EvaluateRuntimeException;
import com.api.jsonata4java.expressions.ExpressionsVisitor;
import com.api.jsonata4java.expressions.generated.MappingExpressionParser.Function_callContext;
import com.api.jsonata4java.expressions.utils.Constants;
import com.api.jsonata4java.expressions.utils.FunctionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * From http://docs.jsonata.org/string-functions.html:
 * 
 * $replace(str, pattern, replacement [, limit])
 * 
 * Finds occurrences of pattern within str and replaces them with replacement.
 * 
 * If str is not specified, then the context value is used as the value of str.
 * It is an error if str is not a string.
 * 
 * The pattern parameter can either be a string or a regular expression (regex).
 * If it is a string, it specifies the substring(s) within str which should be
 * replaced. If it is a regex, its is used to find.
 * 
 * The replacement parameter can either be a string or a function. If it is a
 * string, it specifies the sequence of characters that replace the substring(s)
 * that are matched by pattern. If pattern is a regex, then the replacement
 * string can refer to the characters that were matched by the regex as well as
 * any of the captured groups using a S followed by a number N:
 * 
 * * If N = 0, then it is replaced by substring matched by the regex as a whole.
 * * If N &GT; 0, then it is replaced by the substring captured by the Nth
 * parenthesised group in the regex. * If N is greater than the number of
 * captured groups, then it is replaced by the empty string. * A literal $
 * character must be written as $$ in the replacement string
 * 
 * If the replacement parameter is a function, then it is invoked for each match
 * occurrence of the pattern regex. The replacement function must take a single
 * parameter which will be the object structure of a regex match as described in
 * the $match function; and must return a string.
 * 
 * The optional limit parameter, is a number that specifies the maximum number
 * of replacements to make before stopping. The remainder of the input beyond
 * this limit will be copied to the output unchanged.
 * 
 * Examples
 * 
 * $replace("John Smith and John Jones", "John", "Mr")=="Mr Smith and Mr
 * Jones" $replace("John Smith and John Jones", "John", "Mr", 1)=="Mr Smith
 * and John Jones" $replace("abracadabra", "a.*?a", "*")=="*c*bra"
 * $replace("John Smith", "(\w+)\s(\w+)", "$2, $1")=="Smith, John"
 * $replace("265USD", "([0-9]+)USD", "$$$1")=="$265"
 * 
 */
public class ReplaceFunction extends FunctionBase implements Function {

	public static String ERR_BAD_CONTEXT = String.format(Constants.ERR_MSG_BAD_CONTEXT, Constants.FUNCTION_REPLACE);
	public static String ERR_ARG1BADTYPE = String.format(Constants.ERR_MSG_ARG1_BAD_TYPE, Constants.FUNCTION_REPLACE);
	public static String ERR_ARG2BADTYPE = String.format(Constants.ERR_MSG_ARG2_BAD_TYPE, Constants.FUNCTION_REPLACE);
	public static String ERR_ARG3BADTYPE = String.format(Constants.ERR_MSG_ARG3_BAD_TYPE, Constants.FUNCTION_REPLACE);
	public static String ERR_ARG4BADTYPE = String.format(Constants.ERR_MSG_ARG4_BAD_TYPE, Constants.FUNCTION_REPLACE);
	public static String ERR_ARG5BADTYPE = String.format(Constants.ERR_MSG_ARG5_BAD_TYPE, Constants.FUNCTION_REPLACE);
	public static final String ERR_MSG_ARG2_EMPTY_STR = String.format(Constants.ERR_MSG_ARG2_EMPTY_STR,
			Constants.FUNCTION_REPLACE);

	public JsonNode invoke(ExpressionsVisitor expressionVisitor, Function_callContext ctx) {
		// Create the variable to return
		JsonNode result = null;

		// Retrieve the number of arguments
		JsonNode argString = JsonNodeFactory.instance.nullNode();
		boolean useContext = FunctionUtils.useContextVariable(ctx, getSignature());
		int argCount = getArgumentCount(ctx);
		if (useContext) {
			argString = FunctionUtils.getContextVariable(expressionVisitor);
			argCount++;
		}

		// Make sure that we have the right number of arguments
		if (argCount >= 1 || argCount <= 4) {
			if (!useContext) {
				argString = FunctionUtils.getValuesListExpression(expressionVisitor, ctx, 0);
			}
			if (argString == null || !argString.isTextual()) {
				throw new EvaluateRuntimeException(ERR_ARG1BADTYPE);
			}
			if (argCount >= 2) {
				final JsonNode argPattern = FunctionUtils.getValuesListExpression(expressionVisitor, ctx,
						useContext ? 0 : 1);
				int limit = -1;
				// Make sure that the separator is not null
				if (argPattern != null && argPattern.isTextual()) {
					if (argPattern.asText().isEmpty()) {
						throw new EvaluateRuntimeException(ERR_MSG_ARG2_EMPTY_STR);
					}
					if (argCount >= 3) {
						final JsonNode argReplacement = FunctionUtils.getValuesListExpression(expressionVisitor, ctx,
								useContext ? 1 : 2);
						// Check to see if the pattern is just a string
						if (argReplacement != null && (argReplacement.isTextual())) {
							final String str = argString.textValue();
							final String pattern = argPattern.textValue();
							final String replacement = argReplacement.textValue();

							if (argCount == 4) {
								final JsonNode argLimit = FunctionUtils.getValuesListExpression(expressionVisitor, ctx,
										useContext ? 2 : 3);

								// Check to see if we have an optional limit argument we check
								// it
								if (argLimit != null) {
									if (argLimit.isNumber() && argLimit.asInt() >= 0) {
										limit = argLimit.asInt();
									} else {
										throw new EvaluateRuntimeException(ERR_ARG4BADTYPE);
									}
								}
							}

							// Check to see if a limit was specified
							if (limit == -1) {
								// No limits... replace all occurrences in the string
								result = new TextNode(str.replaceAll(pattern, replacement));
							} else {
								// Only perform the replace the specified number of times
								String retString = new String(str);
								for (int i = 0; i < limit; i++) {
									retString = retString.replaceFirst(pattern, replacement);
								} // FOR
								result = new TextNode(retString);
							}
						} else {
							throw new EvaluateRuntimeException(ERR_ARG3BADTYPE);
						}
					} else {
						throw new EvaluateRuntimeException(ERR_ARG2BADTYPE);
					}
				} else {
					throw new EvaluateRuntimeException(ERR_ARG2BADTYPE);
				}
			}
		} else {
			throw new EvaluateRuntimeException(argCount == 0 ? ERR_ARG1BADTYPE
					: argCount == 1 ? ERR_ARG1BADTYPE : argCount == 2 ? ERR_ARG2BADTYPE : ERR_ARG5BADTYPE);
		}

		return result;
	}

	@Override
	public String getSignature() {
		// accepts a string (or context variable), a string of function, an optional
		// number, returns a string
		return "<s-(sf)(sf)n?:s>";
	}
}
