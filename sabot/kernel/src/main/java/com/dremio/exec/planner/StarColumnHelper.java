/*
 * Copyright (C) 2017-2019 Dremio Corporation
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

package com.dremio.exec.planner;

import java.util.List;
import java.util.Map;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;

public class StarColumnHelper {

  public static final String PREFIX_DELIMITER = "\u00a6\u00a6";

  public static final String STAR_COLUMN = "*";

  public static final String PREFIXED_STAR_COLUMN = PREFIX_DELIMITER + STAR_COLUMN;

  public static boolean containsStarColumn(RelDataType type) {
    if (!type.isStruct()) {
      return false;
    }

    List<String> fieldNames = type.getFieldNames();

    for (String s : fieldNames) {
      if (s.startsWith(STAR_COLUMN)) {
        return true;
      }
    }

    return false;
  }

  public static boolean containsStarColumnInProject(
      RelDataType inputRowType, List<RexNode> projExprs) {
    if (!inputRowType.isStruct()) {
      return false;
    }

    for (RexNode expr : projExprs) {
      if (expr instanceof RexInputRef) {
        String name = inputRowType.getFieldNames().get(((RexInputRef) expr).getIndex());

        if (name.startsWith(STAR_COLUMN)) {
          return true;
        }
      }
    }

    return false;
  }

  public static boolean isPrefixedStarColumn(String fieldName) {
    return fieldName.indexOf(PREFIXED_STAR_COLUMN)
        > 0; // the delimiter * starts at none-zero position.
  }

  public static boolean isNonPrefixedStarColumn(String fieldName) {
    return fieldName.startsWith(STAR_COLUMN);
  }

  public static boolean isStarColumn(String fieldName) {
    return isPrefixedStarColumn(fieldName) || isNonPrefixedStarColumn(fieldName);
  }

  // Expression in some sense is similar to regular columns. Expression (i.e. C1 + C2 + 10) is not
  // associated with an alias, the project will have (C1 + C2 + 10) --> f1, column "f1" could be
  // viewed as a regular column, and does not require prefix. If user put an alias, then,
  // the project will have (C1 + C2 + 10) -> alias.
  public static boolean isRegularColumnOrExp(String fieldName) {
    return !isStarColumn(fieldName);
  }

  public static String extractStarColumnPrefix(String fieldName) {

    assert (isPrefixedStarColumn(fieldName));

    return fieldName.substring(0, fieldName.indexOf(PREFIXED_STAR_COLUMN));
  }

  public static String extractColumnPrefix(String fieldName) {
    if (fieldName.indexOf(PREFIX_DELIMITER) >= 0) {
      return fieldName.substring(0, fieldName.indexOf(PREFIX_DELIMITER));
    } else {
      return "";
    }
  }

  // Given a set of prefixes, check if a regular column is subsumed by any of the prefixed star
  // column in the set.
  public static boolean subsumeColumn(Map<String, String> prefixMap, String fieldName) {
    String prefix = extractColumnPrefix(fieldName);

    if (isRegularColumnOrExp(fieldName)) {
      return false; // regular column or expression is not subsumed by any star column.
    } else {
      return prefixMap.containsKey(prefix)
          && !fieldName.equals(prefixMap.get(prefix)); // t1*0 is subsumed by t1*.
    }
  }
}
