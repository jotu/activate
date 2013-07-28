package net.fwbrasil.activate.storage.relational.idiom

import scala.collection.mutable.{ Map => MutableMap }
import net.fwbrasil.activate.storage.marshalling.BooleanStorageValue
import net.fwbrasil.activate.storage.marshalling.DoubleStorageValue
import net.fwbrasil.activate.storage.marshalling.IntStorageValue
import net.fwbrasil.activate.storage.marshalling.StorageRenameTable
import net.fwbrasil.activate.storage.marshalling.StorageRemoveTable
import net.fwbrasil.activate.storage.marshalling.BigDecimalStorageValue
import net.fwbrasil.activate.storage.marshalling.LongStorageValue
import net.fwbrasil.activate.storage.marshalling.StringStorageValue
import net.fwbrasil.activate.storage.marshalling.StorageValue
import net.fwbrasil.activate.storage.marshalling.DateStorageValue
import net.fwbrasil.activate.storage.marshalling.StorageAddColumn
import net.fwbrasil.activate.storage.marshalling.ModifyStorageAction
import net.fwbrasil.activate.storage.marshalling.FloatStorageValue
import net.fwbrasil.activate.storage.marshalling.StorageAddIndex
import net.fwbrasil.activate.storage.marshalling.StorageAddReference
import net.fwbrasil.activate.storage.marshalling.ReferenceStorageValue
import net.fwbrasil.activate.storage.marshalling.StorageRenameColumn
import net.fwbrasil.activate.storage.marshalling.StorageCreateTable
import net.fwbrasil.activate.storage.marshalling.StorageRemoveReference
import net.fwbrasil.activate.storage.marshalling.StorageRemoveColumn
import net.fwbrasil.activate.storage.marshalling.ByteArrayStorageValue
import net.fwbrasil.activate.storage.marshalling.StorageRemoveIndex
import net.fwbrasil.activate.storage.marshalling.ListStorageValue
import net.fwbrasil.activate.storage.marshalling.StorageCreateListTable
import net.fwbrasil.activate.storage.marshalling.StorageRemoveListTable
import net.fwbrasil.activate.storage.marshalling.IntStorageValue
import net.fwbrasil.activate.statement.query.OrderByCriteria
import net.fwbrasil.activate.statement.query.orderByAscendingDirection

object mssqlDialect extends SqlIdiom {
    def toSqlDmlRegexp(value: String, regex: String) =
        value + " ~ " + regex

    override def findTableStatement(tableName: String) =
        "SELECT COUNT(1) " +
            "  FROM information_schema.tables " +
            " WHERE table_name = '" + tableName + "'"

    override def findTableColumnStatement(tableName: String, columnName: String) =
        "SELECT COUNT(1) " +
            "  FROM information_schema.columns " +
            " WHERE table_name = '" + tableName + "'" +
            "   AND column_name = '" + columnName + "'"

    override def findIndexStatement(tableName: String, indexName: String) =
        "SELECT COUNT(1)  " +
            "	FROM sys.indexes" +
            "  WHERE object_name(object_id) = '" + tableName + "'" +
            "	AND name = '" + indexName + "'"

    override def findConstraintStatement(tableName: String, constraintName: String): String =
        "SELECT COUNT(1) " +
            "  FROM information_schema.key_column_usage " +
            " WHERE table_name = '" + tableName + "'" +
            "   AND constraint_name = '" + constraintName + "'"

    override def escape(string: String) =
        "\"" + string + "\""

    override def toSqlDdl(action: ModifyStorageAction): String = {
        action match {
            case StorageRemoveListTable(ownerTableName, listName, ifNotExists) =>
                "DROP TABLE " + escape(ownerTableName + listName.capitalize)
            case StorageCreateListTable(ownerTableName, listName, valueColumn, orderColumn, ifNotExists) =>
                "CREATE TABLE " + escape(ownerTableName + listName.capitalize) + "(\n" +
                    "	" + escape("owner") + " " + toSqlDdl(ReferenceStorageValue(None)) + " REFERENCES " + escape(ownerTableName) + "(ID),\n" +
                    toSqlDdl(valueColumn) + ", " + toSqlDdl(orderColumn) +
                    ")"
            case StorageCreateTable(tableName, columns, ifNotExists) =>
                "CREATE TABLE " + escape(tableName) + "(\n" +
                    "	ID " + toSqlDdl(ReferenceStorageValue(None)) + " PRIMARY KEY" + (if (columns.nonEmpty) ",\n" else "") +
                    columns.map(toSqlDdl).mkString(", \n") +
                    ")"
            case StorageRenameTable(oldName, newName, ifExists) =>
                "ALTER TABLE " + escape(oldName) + " RENAME TO " + escape(newName)
            case StorageRemoveTable(name, ifExists, isCascade) =>
                "DROP TABLE " + escape(name) + (if (isCascade) " CASCADE" else "")
            case StorageAddColumn(tableName, column, ifNotExists) =>
                "ALTER TABLE " + escape(tableName) + " ADD " + toSqlDdl(column)
            case StorageRenameColumn(tableName, oldName, column, ifExists) =>
                "ALTER TABLE " + escape(tableName) + " RENAME COLUMN " + escape(oldName) + " TO " + escape(column.name)
            case StorageRemoveColumn(tableName, name, ifExists) =>
                "ALTER TABLE " + escape(tableName) + " DROP COLUMN " + escape(name)
            case StorageAddIndex(tableName, columnName, indexName, ifNotExists, unique) =>
                "CREATE " + (if (unique) "UNIQUE " else "") + "INDEX " + escape(indexName) + " ON " + escape(tableName) + " (" + escape(columnName) + ")"
            case StorageRemoveIndex(tableName, columnName, name, ifExists) =>
                "DROP INDEX " + escape(name)
            case StorageAddReference(tableName, columnName, referencedTable, constraintName, ifNotExists) =>
                "ALTER TABLE " + escape(tableName) + " ADD CONSTRAINT " + escape(constraintName) + " FOREIGN KEY (" + escape(columnName) + ") REFERENCES " + escape(referencedTable) + "(id)"
            case StorageRemoveReference(tableName, columnName, referencedTable, constraintName, ifNotExists) =>
                "ALTER TABLE " + escape(tableName) + " DROP CONSTRAINT " + escape(constraintName)
        }
    }

    override def concat(strings: String*) =
        "CONCAT(" + strings.mkString(", ") + ")"

    override def toSqlDml(criteria: OrderByCriteria[_])(implicit binds: MutableMap[StorageValue, String]): String =
        super.toSqlDml(criteria) + (if(criteria.direction == orderByAscendingDirection)" NULLS FIRST" else " NULLS LAST")

    override def toSqlDdl(storageValue: StorageValue): String =
        storageValue match {
            case value: IntStorageValue =>
                "INTEGER"
            case value: LongStorageValue =>
                "BIGINT"
            case value: BooleanStorageValue =>
                "BIT"
            case value: StringStorageValue =>
                "VARCHAR(4000)"
            case value: FloatStorageValue =>
                "REAL"
            case value: DateStorageValue =>
                "TIMESTAMP"
            case value: DoubleStorageValue =>
                "DOUBLE"
            case value: BigDecimalStorageValue =>
                "DECIMAL"
            case value: ListStorageValue =>
                "VARCHAR(1)"
            case value: ByteArrayStorageValue =>
                "BLOB"
            case value: ReferenceStorageValue =>
                "VARCHAR(45)"
        }
}
