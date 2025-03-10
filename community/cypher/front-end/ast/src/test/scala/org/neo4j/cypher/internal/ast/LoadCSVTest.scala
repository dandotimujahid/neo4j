/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.cypher.internal.ast

import SemanticCheckInTest.SemanticCheckWithDefaultContext
import org.neo4j.cypher.internal.ast.semantics.SemanticError
import org.neo4j.cypher.internal.ast.semantics.SemanticState
import org.neo4j.cypher.internal.expressions.StringLiteral
import org.neo4j.cypher.internal.expressions.Variable
import org.neo4j.cypher.internal.util.DummyPosition
import org.neo4j.cypher.internal.util.symbols.CTList
import org.neo4j.cypher.internal.util.symbols.CTMap
import org.neo4j.cypher.internal.util.symbols.CTString
import org.neo4j.cypher.internal.util.test_helpers.CypherFunSuite
import org.neo4j.gqlstatus.ErrorGqlStatusObjectImplementation
import org.neo4j.gqlstatus.GqlStatusInfoCodes

class LoadCSVTest extends CypherFunSuite {

  val literalURL = StringLiteral("file:///tmp/foo.csv")(DummyPosition(4).withInputLength(1))
  val variable = Variable("a")(DummyPosition(4), Variable.isIsolatedDefault)

  test("cannot overwrite existing variable") {
    val loadCSV = LoadCSV(withHeaders = true, literalURL, variable, None)(DummyPosition(6))
    val result = loadCSV.semanticCheck.run(SemanticState.clean)
    assert(result.errors === Seq())
  }

  test("when expecting headers, the variable has a map type") {
    val loadCSV = LoadCSV(withHeaders = true, literalURL, variable, None)(DummyPosition(6))
    val result = loadCSV.semanticCheck.run(SemanticState.clean)
    val expressionType = result.state.expressionType(variable).actual

    assert(expressionType === CTMap.invariant)
  }

  test("when not expecting headers, the variable has a list type") {
    val loadCSV = LoadCSV(withHeaders = false, literalURL, variable, None)(DummyPosition(6))
    val result = loadCSV.semanticCheck.run(SemanticState.clean)
    val expressionType = result.state.expressionType(variable).actual

    assert(expressionType === CTList(CTString).invariant)
  }

  test("should accept one-character wide field terminators") {
    val literal = StringLiteral("http://example.com/foo.csv")(DummyPosition(4).withInputLength(2))
    val loadCSV =
      LoadCSV(withHeaders = false, literal, variable, Some(StringLiteral("\t")(DummyPosition(0).withInputLength(2))))(
        DummyPosition(6)
      )
    val result = loadCSV.semanticCheck.run(SemanticState.clean)
    assert(result.errors === Vector.empty)
  }

  test("should reject more-than-one-character wide field terminators") {
    val literal = StringLiteral("http://example.com/foo.csv")(DummyPosition(4).withInputLength(2))
    val loadCSV =
      LoadCSV(withHeaders = false, literal, variable, Some(StringLiteral("  ")(DummyPosition(0).withInputLength(2))))(
        DummyPosition(6)
      )
    val result = loadCSV.semanticCheck.run(SemanticState.clean)
    val gql = ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_42001)
      .atPosition(0, 0, 0)
      .withCause(ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_42I05)
        .atPosition(0, 0, 0)
        .build())
      .build()

    assert(result.errors === Vector(SemanticError(
      gql,
      "CSV field terminator can only be one character wide",
      DummyPosition(0).withInputLength(2)
    )))
  }
}
