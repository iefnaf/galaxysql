package org.apache.calcite.sql.fun;

import org.apache.calcite.sql.SqlAggFunction;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.calcite.sql.type.SqlTypeFamily;

public class SqlStddevPopAggFunction extends SqlAggFunction {
    public SqlStddevPopAggFunction() {
        super(
            "STDDEV_POP",
            null,
            SqlKind.STDDEV_POP,
            ReturnTypes.DOUBLE_NULLABLE,
            null,
            OperandTypes.family(SqlTypeFamily.ANY),
            SqlFunctionCategory.NUMERIC,
            false,
            false);
    }
}
