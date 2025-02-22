/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.sql.fun;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlOperatorBinding;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.pretty.SqlPrettyWriter;
import org.apache.calcite.sql.type.InferTypes;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.calcite.sql.type.SqlTypeName;

public class SqlAlterTypeOperator extends SqlSpecialOperator {
    public SqlAlterTypeOperator() {
        super("ALTER_TYPE", SqlKind.DEFAULT, 100, true,
            ReturnTypes.explicit(SqlTypeName.ANY), InferTypes.RETURN_TYPE,
            OperandTypes.ZERO_OR_ONE);
    }

    public SqlAlterTypeOperator(SqlTypeName sqlTypeName) {
        super("ALTER_TYPE", SqlKind.DEFAULT, 100, true,
            ReturnTypes.explicit(sqlTypeName), InferTypes.RETURN_TYPE,
            OperandTypes.ZERO_OR_ONE);
    }

    @Override
    public void unparse(SqlWriter writer, SqlCall call, int leftPrec,
                        int rightPrec) {
        if (!call.getOperandList().isEmpty()) {
            writer.keyword(getName());
            final SqlWriter.Frame frame = writer.startList("(", ")");
            call.operand(0).unparse(writer, leftPrec, rightPrec);
            writer.endList(frame);
        }
    }

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
        if (opBinding.getOperandCount() == 1) {
            return opBinding.getOperandType(0);
        }
        return super.inferReturnType(opBinding);
    }
}

// End SqlDefaultOperator.java
