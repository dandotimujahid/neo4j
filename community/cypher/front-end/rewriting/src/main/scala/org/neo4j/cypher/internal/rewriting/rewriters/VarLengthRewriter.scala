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
package org.neo4j.cypher.internal.rewriting.rewriters

import org.neo4j.cypher.internal.expressions.VarLengthBound
import org.neo4j.cypher.internal.util.Rewriter
import org.neo4j.cypher.internal.util.Rewriter.TopDownMergeableRewriter
import org.neo4j.cypher.internal.util.topDown

/**
 * Rewrites [[VarLengthLowerBound]] and [[VarLengthUpperBound]] predicates into expressions that the runtime can evaluate.
 */
case object VarLengthRewriter extends Rewriter with TopDownMergeableRewriter {

  override val innerRewriter: Rewriter = Rewriter.lift {
    case predicate: VarLengthBound => predicate.getRewrittenPredicate
  }

  private val instance = topDown(innerRewriter)

  override def apply(value: AnyRef): AnyRef = instance(value)
}
