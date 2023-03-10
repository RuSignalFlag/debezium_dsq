/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.relational;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.debezium.annotation.NotThreadSafe;

/**
 * An editor for {@link Table} instances, normally obtained from a {@link Tables} instance.
 *
 * @author Randall Hauch
 *
 * Modified by an in 2020.7.2 for constraint feature
 */
@NotThreadSafe
public interface TableEditor {

    /**
     * Create a new editor that does nothing.
     *
     * @param id the table's identifier; may not be null
     * @return the editor; never null
     */
    static TableEditor noOp(TableId id) {
        return new NoOpTableEditorImpl().tableId(id);
    }

    /**
     * Get the identifier for this table.
     *
     * @return the table identifier; may be null if not set
     */
    TableId tableId();

    /**
     * Set the table identifier.
     *
     * @param tableId the table identifier
     * @return this editor so callers can chain methods together
     */
    TableEditor tableId(TableId tableId);

    /**
     * Get the definitions for the columns in this table. The resulting list should not be modified directly;
     * instead, the column definitions should be defined with {@link #addColumns(Column...)},
     * {@link #addColumns(Iterable)}, {@link #setColumns(Column...)}, {@link #setColumns(Iterable)}, or
     * {@link #removeColumn(String)}.
     *
     * @return the ordered list of definitions; never null
     */
    List<Column> columns();

    /**
     * Get the names of the columns in this table. The resulting list should not be modified directly;
     * instead, the column definitions should be defined with {@link #addColumns(Column...)},
     * {@link #addColumns(Iterable)}, {@link #setColumns(Column...)}, {@link #setColumns(Iterable)}, or
     * {@link #removeColumn(String)}.
     *
     * @return the ordered list of column names; never null
     */
    default List<String> columnNames() {
        return columns().stream().map(Column::name).collect(Collectors.toList());
    }

    /**
     * Get the definition for the column in this table with the supplied name. The case of the supplied name does not matter.
     *
     * @param name the case-insensitive name of the column
     * @return the column definition, or null if there is no column with the given name
     */
    Column columnWithName(String name);

    /**
     * The list of column names that make up the primary key for this table. The resulting list should not be modified directly;
     * instead, the set of primary key names should be defined with {@link #setPrimaryKeyNames(String...)}.
     *
     * @return the list of column names that make up the primary key; never null but possibly empty
     */
    List<String> primaryKeyColumnNames();

    default List<String> primaryConstraintName() {
        return Collections.emptyList();
    }

    /**
     * The list of column changes that make up the primary key for this table. The resulting list should not be modified directly;
     * instead, the set of primary key changes should be defined with {@link #setPrimaryKeyNames(String...)}.
     *
     * @return the list of column changes that make up the primary key; never null but possibly empty
     */
    List<Map<String, String>> primaryKeyColumnChanges();

    /**
     * The list of column changes that make up the constraint for this table. The resulting list should not be modified directly;
     * instead, the set of primary key changes should be defined with {@link #setConstraintChanges(String...)}.
     *
     * @return the list of column changes that make up the constraint; never null but possibly empty
     */
    public List<Map<String, String>> constraintChanges();

    /**
     * The list of column that make up the foreign key for this table. The resulting list should not be modified directly;
     * instead, the set of foreign key should be defined with {@link #setPrimaryKeyNames(String...)}.
     *
     * @return the list of column that make up the foreign key; never null but possibly empty
     */
    List<Map<String, String>> foreignKeyColumns();

    /**
      * The list of column that make up the unique index for this table. The resulting list should not be modified directly;
     * instead, the set of unique index should be defined with {@link #setPrimaryKeyNames(String...)}.
     *
     * @return the list of column that make up the unique index; never null but possibly empty
       */
    List<Map<String, String>> uniqueColumns();

    /**
     * The list of column that make up the check for this table. The resulting list should not be modified directly;
     * instead, the set of check should be defined with {@link #setPrimaryKeyNames(String...)}.
     *
     * @return the list of column that make up the check; never null but possibly empty
       */
    List<Map<String, String>> checkColumns();

    /**
     * map which column has changed in alter table sql
     * @return
     */
    Map<String, List<String>> getChangeColumn();

    default Set<String> getIndexes() {
        return null;
    }

    default Index getChangeIndex() {
        return null;
    }

    /**
     * Determine whether this table has a primary key.
     * @return {@code true} if this table has at least one {@link #primaryKeyColumnNames() primary key column}, or {@code false}
     * if there are no primary key columns
     */
    default boolean hasPrimaryKey() {
        return !primaryKeyColumnNames().isEmpty();
    }

    /**
     * Add one columns to this table, regardless of the {@link Column#position() position} of the supplied
     * columns. However, if an existing column definition matches a supplied column, the new column definition will replace
     * the existing column definition.
     *
     * @param column the definition for the column to be added
     * @return this editor so callers can chain methods together
     */
    default TableEditor addColumn(Column column) {
        return addColumns(column);
    }

    /**
     * Add one or more columns to this table, regardless of the {@link Column#position() position} of the supplied
     * columns. However, if an existing column definition matches a supplied column, the new column definition will replace
     * the existing column definition.
     *
     * @param columns the definitions for the columns to be added
     * @return this editor so callers can chain methods together
     */
    TableEditor addColumns(Column... columns);

    /**
     * Add one or more columns to the end of this table's list of columns, regardless of the {@link Column#position()
     * position} of the supplied columns. However, if an existing column definition matches a supplied column, the new column
     * definition will replace the existing column definition.
     *
     * @param columns the definitions for the columns to be added
     * @return this editor so callers can chain methods together
     */
    TableEditor addColumns(Iterable<Column> columns);

    /**
     * Set this table's column definitions. The table's primary key columns may be removed as a result of this method if they
     * refer to columns that are not in the supplied list of column definitions.
     *
     * @param columns the definitions for the columns to be added
     * @return this editor so callers can chain methods together
     */
    TableEditor setColumns(Column... columns);

    /**
     * Set this table's column definitions. The table's primary key columns may be removed as a result of this method if they
     * refer to columns that are not in the supplied list of column definitions.
     *
     * @param columns the definitions for the columns to be added
     * @return this editor so callers can chain methods together
     */
    TableEditor setColumns(Iterable<Column> columns);

    /**
     * Remove the column with the given name. This method does nothing if no such column exists.
     *
     * @param columnName the name of the column to be removed
     * @return this editor so callers can chain methods together
     */
    TableEditor removeColumn(String columnName);

    /**
     * Update the column with the given name. The existing column definition with the name as the column provided
     * is replaced with the new one.
     *
     * @param column the new column definition
     * @return this editor so callers can chain methods together
     */
    TableEditor updateColumn(Column column);

    /**
     * Reorder the column with the given name to be positioned after the designated column. If {@code afterColumnName} is null,
     * the column will be moved to the first column.
     *
     * @param columnName the name of the column to be removed
     * @param afterColumnName the name of the column after which the moved column is to be positioned; may be null if the column
     *            is to be moved to the first column
     * @return this editor so callers can chain methods together
     */
    TableEditor reorderColumn(String columnName, String afterColumnName);

    /**
     * Rename the column with the given name to the new specified name.
     *
     * @param existingName the existing name of the column to be renamed; may not be null
     * @param newName the new name of the column; may not be null
     * @return this editor so callers can chain methods together
     */
    TableEditor renameColumn(String existingName, String newName);

    /**
     * Set the columns that make up this table's primary key.
     *
     * @param pkColumnNames the names of this tables columns that make up the primary key
     * @return this editor so callers can chain methods together
     * @throws IllegalArgumentException if a name does not correspond to an existing column
     */
    TableEditor setPrimaryKeyNames(String... pkColumnNames);

    /**
     * Set the columns that make up this table's primary key.
     *
     * @param pkColumnNames the names of this tables columns that make up the primary key
     * @return this editor so callers can chain methods together
     * @throws IllegalArgumentException if a name does not correspond to an existing column
     */
    TableEditor setPrimaryKeyNames(List<String> pkColumnNames);

    default TableEditor setPrimaryConstraintName(List<String> primaryConstraintName) {
        return this;
    }

    /**
     * Set the columns change that make up this table's constraint.
     *
     * @param pkColumnNames the names of this tables columns change that make up the constraint
     * @return this editor so callers can chain methods together
     * @throws IllegalArgumentException if a name does not correspond to an existing column
     */
    TableEditor setConstraintChanges(List<Map<String, String>> constraintChanges);

    /**
     * Set the columns change that make up this table's primary key.
     *
     * @param pkColumnNames the names of this tables columns change that make up the primary key
     * @return this editor so callers can chain methods together
     * @throws IllegalArgumentException if a name does not correspond to an existing column
     */
    TableEditor setPrimaryKeyChanges(List<Map<String, String>> pkColumnChanges);

    /**
     * Set the columns that make up this table's unique index.
     *
     * @param uniqueColumns the names of this tables columns that make up the unique index
     * @return this editor so callers can chain methods together
     * @throws IllegalArgumentException if a name does not correspond to an existing column
     */
    TableEditor setUniqueColumns(List<Map<String, String>> uniqueColumns);

    /**
     * Set the columns that make up this table's check.
     *
     * @param checkColumns the names of this tables columns that make up the check
     * @return this editor so callers can chain methods together
     * @throws IllegalArgumentException if a name does not correspond to an existing column
     */
    TableEditor setCheckColumns(List<Map<String, String>> checkColumns);

    /**
     * Set the columns that make up this table's primary key.
     *
     * @param fkColumns the names of this tables columns that make up the foreign key
     * @return this editor so callers can chain methods together
     * @throws IllegalArgumentException if a name does not correspond to an existing column
     */
    TableEditor setForeignKeys(List<Map<String, String>> fkColumns);

    /**
     * Sets this table's primary key to contain all columns, ensuring that all values are unique within the table.
     * This is analogous to calling {@code setPrimaryKeyNames(columnNames())} except that the primary key is updated
     * when columns are added or removed.
     *
     * @return this editor so callers can chain methods together
     * @throws IllegalArgumentException if a name does not correspond to an existing column
     */
    TableEditor setUniqueValues();

    /**
     * Set the name of the character set that should be used by default in the columns that require a character set but have
     * not defined one.
     * @param charsetName the name of the character set that should be used by default
     * @return this editor so callers can chain methods together
     */
    TableEditor setDefaultCharsetName(String charsetName);

    /**
     * Set the comment of the table
     * @param comment table comment
     * @return this editor so callers can chain methods together
     */
    TableEditor setComment(String comment);

    TableEditor setChangeColumn(Map<String, List<String>> changeColumn);

    default TableEditor setChangeIndex(Index index) {
        return this;
    }

    default TableEditor setIndexes(Set<String> index) {
        return this;
    }

    default TableEditor addIndex(String indexName) {
        return this;
    }

    default TableEditor removeIndex(String indexName) {
        return this;
    }

    /**
     * Determine if a {@link #setDefaultCharsetName(String) default character set} has been set on this table.
     * @return {@code true} if this has a default character set, or {@code false} if one has not yet been set
     */
    boolean hasDefaultCharsetName();

    /**
     * Determine if a {@link #setComment(String) comment} has been set on this table.
     * @return {@code true} if this has a comment, or {@code false} if one has not yet been set
     */
    boolean hasComment();

    /**
     * Determine whether this table's primary key contains all columns (via {@link #setUniqueValues()}) such that all rows
     * within the table are unique.
     * @return {@code true} if {@link #setUniqueValues()} was last called on this table, or {@code false} otherwise
     */
    boolean hasUniqueValues();

    /**
     * Determine whether this table's constraint contains all columns (via {@link #clearConstraint()}) such that all rows
     * clear all constraint.
     * @return {@code true} if {@link #clearConstraint()} was last called on this table, or {@code false} otherwise
     */
    boolean clearConstraint();

    /**
     * Obtain an immutable table definition representing the current state of this editor. This editor can be reused
     * after this method, since the resulting table definition no longer refers to any of the data used in this editor.
     *
     * @return the immutable table definition; never null
     */
    Table create();

    default void clearColumnChange() {

    }

}
