/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.oracle;

import static org.fest.assertions.Assertions.assertThat;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Test;

import io.debezium.connector.oracle.antlr.OracleDdlParser;
import io.debezium.connector.oracle.util.TestHelper;
import io.debezium.doc.FixFor;
import io.debezium.relational.Column;
import io.debezium.relational.Index;
import io.debezium.relational.Table;
import io.debezium.relational.TableId;
import io.debezium.relational.Tables;
import io.debezium.relational.ddl.DdlChanges;
import io.debezium.relational.ddl.DdlParserListener;
import io.debezium.util.IoUtil;

/**
 * This is the test suite for Oracle Antlr parser unit testing
 */
public class OracleDdlParserTest {

    private static final String TABLE_NAME = "TEST";
    private static final String PDB_NAME = "ORCLPDB1";

    private OracleDdlParser parser;
    private Tables tables;

    @Before
    public void setUp() {
        final OracleConnectorConfig config = new OracleConnectorConfig(TestHelper.defaultConfig().build());
        parser = new OracleDdlParser(true, null, config.getTableFilters().dataCollectionFilter());
        tables = new Tables();
    }

    @Test
    public void shouldParseCreateAndAlterTable() throws Exception {
        final String createStatement = IoUtil.read(IoUtil.getResourceAsStream("ddl/create_table.sql", null, getClass(), null, null));
        Objects.requireNonNull(createStatement);
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("DEBEZIUM");
        parser.parse(createStatement, tables);
        Table table = tables.forTable(new TableId(PDB_NAME, "DEBEZIUM", TABLE_NAME));

        assertThat(tables.size()).isEqualTo(1);
        assertThat(table.retrieveColumnNames()).containsExactly("ID", "COL1", "COL2", "COL3", "COL4", "COL5", "COL6", "COL7", "COL8", "COL9", "COL10", "COL11", "COL12",
                "COL13");
        // ID, primary key
        assertThat(table.columnWithName("ID").position()).isEqualTo(1);
        assertThat(table.isPrimaryKeyColumn("ID"));
        testColumn(table, "ID", false, Types.NUMERIC, "NUMBER", 19, 0, false, null);
        // number(4,2)
        testColumn(table, "COL1", true, Types.NUMERIC, "NUMBER", 4, 2, true, null);
        // varchar2(255) default 'debezium'
        testColumn(table, "COL2", false, Types.VARCHAR, "VARCHAR2", 255, null, true, "debezium");
        // nvarchar2(255)
        testColumn(table, "COL3", false, Types.NVARCHAR, "NVARCHAR2", 255, null, false, null);
        // char(4)
        testColumn(table, "COL4", true, Types.CHAR, "CHAR", 4, null, true, null);
        // nchar(4)
        testColumn(table, "COL5", true, Types.NCHAR, "NCHAR", 4, 0, true, null);
        // float(126)
        testColumn(table, "COL6", true, Types.FLOAT, "FLOAT", 126, 0, true, null);
        // date
        testColumn(table, "COL7", true, Types.TIMESTAMP, "DATE", -1, null, true, null);
        // timestamp
        testColumn(table, "COL8", true, Types.TIMESTAMP, "TIMESTAMP", 6, null, true, null);
        // blob
        testColumn(table, "COL9", true, Types.BLOB, "BLOB", -1, null, true, null);
        // clob
        testColumn(table, "COL10", true, Types.CLOB, "CLOB", -1, null, true, null);
        // sdo_geometry
        testColumn(table, "col11", true, Types.STRUCT, "MDSYS.SDO_GEOMETRY", -1, null, true, null);
        // number(1,0)
        testColumn(table, "col12", true, Types.NUMERIC, "NUMBER", 1, 0, true, null);
        // date
        testColumn(table, "col13", false, Types.TIMESTAMP, "DATE", -1, null, false, null);

        String ddl = "alter table " + TABLE_NAME + " add (col21 varchar2(20), col22 number(19));";
        parser.parse(ddl, tables);
        Table alteredTable = tables.forTable(new TableId(PDB_NAME, "DEBEZIUM", TABLE_NAME));
        assertThat(alteredTable.retrieveColumnNames()).containsExactly("ID", "COL1", "COL2", "COL3", "COL4", "COL5", "COL6", "COL7", "COL8", "COL9", "COL10", "COL11",
                "COL12", "COL13",
                "COL21",
                "COL22");
        // varchar2(255)
        testColumn(alteredTable, "COL21", true, Types.VARCHAR, "VARCHAR2", 20, null, true, null);
        testColumn(alteredTable, "COL22", true, Types.NUMERIC, "NUMBER", 19, 0, true, null);

        ddl = "alter table " + TABLE_NAME + " add col23 varchar2(20);";
        try {
            parser.parse(ddl, tables);
        }
        catch (Exception e) {
            // Although parenthesis are optional for Oracle, but must be presented to be parsed, Antlr source error?
            assertThat(e.getMessage().contains("no viable alternative at input"));
        }
        ddl = "alter table " + TABLE_NAME + " add (col23 varchar2(20) not null);";
        parser.parse(ddl, tables);
        alteredTable = tables.forTable(new TableId(PDB_NAME, "DEBEZIUM", TABLE_NAME));
        assertThat(alteredTable.retrieveColumnNames()).containsExactly("ID", "COL1", "COL2", "COL3", "COL4", "COL5", "COL6", "COL7", "COL8", "COL9", "COL10", "COL11",
                "COL12", "COL13",
                "COL21",
                "COL22", "COL23");
        testColumn(alteredTable, "COL23", false, Types.VARCHAR, "VARCHAR2", 20, null, false, null);

        ddl = "alter table " + TABLE_NAME + " drop (col22, col23);";
        parser.parse(ddl, tables);
        alteredTable = tables.forTable(new TableId(PDB_NAME, "DEBEZIUM", TABLE_NAME));
        assertThat(alteredTable.retrieveColumnNames()).containsExactly("ID", "COL1", "COL2", "COL3", "COL4", "COL5", "COL6", "COL7", "COL8", "COL9", "COL10", "COL11",
                "COL12", "COL13",
                "COL21");

        ddl = "drop table " + TABLE_NAME + ";";
        parser.parse(ddl, tables);
        assertThat(tables.size()).isZero();

        // todo
        ddl = "drop table " + TABLE_NAME + " cascade constrains purge;";
        ddl = "ALTER TABLE " + TABLE_NAME + " ADD CONSTRAINT FKB97209E040C4205 FOREIGN KEY (col1) REFERENCES debezium_ref(ID);";
        ddl = "ALTER TABLE " + TABLE_NAME + " MODIFY COL1 varchar2(50) not null;";
    }

    /**
     * this is original test by Gunnar Morling
     */
    @Test
    public void shouldParseCreateTable() {
        parser = new OracleDdlParser(true, false, true, null, Tables.TableFilter.includeAll());
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("DEBEZIUM");

        String CREATE_SIMPLE_TABLE = "create table debezium.customer (" +
                "  id int not null, " +
                "  name varchar2(1000), " +
                "  score decimal(6, 2), " +
                "  registered date, " +
                "  primary key (id)" +
                ");";
        parser.parse(CREATE_SIMPLE_TABLE, tables);
        // parse column comment
        String[] COMMENT_ON_COLUMNS = new String[]{ "comment on column debezium.customer.id is 'pk'",
                "comment on column customer.name is 'the name'",
                "comment on column customer.score is 'the score'",
                "comment on column customer.registered is 'registered date'" };
        for (String comment : COMMENT_ON_COLUMNS) {
            parser.parse(comment, tables);
        }
        // parse table comment
        String COMMENT_ON_TABLE = "comment on table debezium.customer is 'this is customer table'";
        parser.parse(COMMENT_ON_TABLE, tables);
        Table table = tables.forTable(new TableId(PDB_NAME, "DEBEZIUM", "CUSTOMER"));

        assertThat(table).isNotNull();

        Column id = table.columnWithName("ID");
        assertThat(id.isOptional()).isFalse();
        assertThat(id.jdbcType()).isEqualTo(Types.NUMERIC);
        assertThat(id.typeName()).isEqualTo("NUMBER");
        assertThat(id.comment()).isEqualTo("pk");

        final Column name = table.columnWithName("NAME");
        assertThat(name.isOptional()).isTrue();
        assertThat(name.jdbcType()).isEqualTo(Types.VARCHAR);
        assertThat(name.typeName()).isEqualTo("VARCHAR2");
        assertThat(name.length()).isEqualTo(1000);
        assertThat(name.comment()).isEqualTo("the name");

        final Column score = table.columnWithName("SCORE");
        assertThat(score.isOptional()).isTrue();
        assertThat(score.jdbcType()).isEqualTo(Types.NUMERIC);
        assertThat(score.typeName()).isEqualTo("NUMBER");
        assertThat(score.length()).isEqualTo(6);
        assertThat(score.scale().get()).isEqualTo(2);
        assertThat(score.comment()).isEqualTo("the score");

        assertThat(table.columns()).hasSize(4);
        assertThat(table.isPrimaryKeyColumn("ID"));

        assertThat(table.comment()).isEqualTo("this is customer table");
    }

    @Test
    public void testParsingMultiStatementTableMetadataResult() throws Exception {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("IDENTITYDB");

        String MULTI_STATEMENT_DDL = "  CREATE TABLE \"IDENTITYDB\".\"CHANGE_NUMBERS\"\n" +
                "   (    \"CHANGE_NO\" NUMBER(*,0) NOT NULL ENABLE,\n" +
                "    \"SOURCE_INFO\" VARCHAR2(128) NOT NULL ENABLE,\n" +
                "    \"CHANGED_TIME\" TIMESTAMP (6) DEFAULT SYSDATE,\n" +
                "    \"ENTITY_TYPE\" VARCHAR2(36) NOT NULL ENABLE,\n" +
                "    \"ORGANIZATIONID\" VARCHAR2(36),\n" +
                "    \"PROCESSED\" NUMBER(1,0) DEFAULT 1 NOT NULL ENABLE,\n" +
                "    \"TRACKING_ID\" VARCHAR2(256) NOT NULL ENABLE,\n" +
                "    \"EXPIRY_TIME\" TIMESTAMP (6),\n" +
                "    \"ACTOR\" VARCHAR2(36),\n" +
                "    \"ENTITY_ID\" VARCHAR2(36)\n" +
                "   )\n" +
                "  PARTITION BY RANGE (\"EXPIRY_TIME\") INTERVAL (NUMTODSINTERVAL(7, 'DAY'))\n" +
                "  SUBPARTITION BY HASH (\"CHANGE_NO\")\n" +
                "  SUBPARTITIONS 16\n" +
                " (PARTITION \"SYS_P44414\"  VALUES LESS THAN (TIMESTAMP' 2021-07-06 00:00:00')\n" +
                " ( SUBPARTITION \"SYS_SUBP44398\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44399\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44400\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44401\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44402\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44403\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44404\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44405\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44406\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44407\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44408\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44409\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44410\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44411\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44412\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44413\" ) );\n" +
                "  CREATE UNIQUE INDEX \"IDENTITYDB\".\"IDX_CHANGENUMBERS_PK\" ON \"IDENTITYDB\".\"CHANGE_NUMBERS\" (\"CHANGE_NO\", \"EXPIRY_TIME\")\n" +
                "   LOCAL\n" +
                " (PARTITION \"SYS_P44414\" NOCOMPRESS\n" +
                " ( SUBPARTITION \"SYS_SUBP44398\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44399\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44400\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44401\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44402\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44403\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44404\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44405\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44406\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44407\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44408\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44409\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44410\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44411\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44412\" ,\n" +
                "  SUBPARTITION \"SYS_SUBP44413\" ) );\n" +
                "ALTER TABLE \"IDENTITYDB\".\"CHANGE_NUMBERS\" ADD CONSTRAINT \"IDX_CHANGENUMBERS_PK\" PRIMARY KEY (\"CHANGE_NO\", \"EXPIRY_TIME\")\n" +
                "  USING INDEX \"IDENTITYDB\".\"IDX_CHANGENUMBERS_PK\"  ENABLE NOVALIDATE;";

        parser.parse(MULTI_STATEMENT_DDL, tables);
        final DdlChanges changes = parser.getDdlChanges();

        final List<DdlParserListener.EventType> eventTypes = new ArrayList<>();
        changes.getEventsByDatabase((String dbName, List<DdlParserListener.Event> events) -> {
            events.forEach(event -> eventTypes.add(event.type()));
        });
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.CREATE_TABLE,
                DdlParserListener.EventType.CREATE_INDEX,
                DdlParserListener.EventType.ALTER_TABLE);

        Table table = tables.forTable(new TableId(PDB_NAME, "IDENTITYDB", "CHANGE_NUMBERS"));
        List<String> columnNames = table.retrieveColumnNames();
        assertThat(columnNames).contains("CHANGE_NO", "SOURCE_INFO", "CHANGED_TIME", "ENTITY_TYPE", "ORGANIZATIONID", "PROCESSED", "TRACKING_ID", "EXPIRY_TIME", "ACTOR",
                "ENTITY_ID");
        assertThat(table.primaryKeyColumnNames()).containsExactly("CHANGE_NO", "EXPIRY_TIME");
    }

    @Test
    @FixFor("DBZ-4135")
    public void shouldParseAlterTableAddColumnStatement() throws Exception {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");

        Table table = Table.editor()
                .tableId(new TableId(PDB_NAME, "SCOTT", "T_DBZ_TEST1"))
                .addColumn(Column.editor().name("ID").create())
                .create();
        tables.overwriteTable(table);

        String SQL = "ALTER TABLE \"SCOTT\".\"T_DBZ_TEST1\" ADD T_VARCHAR2 VARCHAR2(20);";
        parser.parse(SQL, tables);

        final DdlChanges changes = parser.getDdlChanges();
        final List<DdlParserListener.EventType> eventTypes = new ArrayList<>();
        changes.getEventsByDatabase((String dbName, List<DdlParserListener.Event> events) -> {
            events.forEach(event -> eventTypes.add(event.type()));
        });
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.ALTER_TABLE);

        table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "T_DBZ_TEST1"));
        List<String> columnNames = table.retrieveColumnNames();
        assertThat(columnNames).contains("ID", "T_VARCHAR2");
        assertThat(table.columnWithName("T_VARCHAR2").typeName()).isEqualTo("VARCHAR2");
        assertThat(table.columnWithName("T_VARCHAR2").length()).isEqualTo(20);
    }

    @Test
    @FixFor("DBZ-4135")
    public void shouldParseAlterTableModifyColumnStatement() throws Exception {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");

        Table table = Table.editor()
                .tableId(new TableId(PDB_NAME, "SCOTT", "T_DBZ_TEST1"))
                .addColumn(Column.editor().name("ID").create())
                .addColumn(Column.editor().name("T_VARCHAR2").type("VARCHAR2").length(10).create())
                .create();
        tables.overwriteTable(table);

        String SQL = "ALTER TABLE \"SCOTT\".\"T_DBZ_TEST1\" MODIFY T_VARCHAR2 VARCHAR2(20);";
        parser.parse(SQL, tables);

        final DdlChanges changes = parser.getDdlChanges();
        final List<DdlParserListener.EventType> eventTypes = new ArrayList<>();
        changes.getEventsByDatabase((String dbName, List<DdlParserListener.Event> events) -> {
            events.forEach(event -> eventTypes.add(event.type()));
        });
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.ALTER_TABLE);

        table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "T_DBZ_TEST1"));
        List<String> columnNames = table.retrieveColumnNames();
        assertThat(columnNames).contains("ID", "T_VARCHAR2");
        assertThat(table.columnWithName("T_VARCHAR2").typeName()).isEqualTo("VARCHAR2");
        assertThat(table.columnWithName("T_VARCHAR2").length()).isEqualTo(20);
    }

    @Test
    @FixFor("DBZ-4135")
    public void shouldParseAlterTableDropColumnStatement() throws Exception {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");

        Table table = Table.editor()
                .tableId(new TableId(PDB_NAME, "SCOTT", "T_DBZ_TEST1"))
                .addColumn(Column.editor().name("ID").create())
                .addColumn(Column.editor().name("T_VARCHAR2").type("VARCHAR2").length(10).create())
                .create();
        tables.overwriteTable(table);

        String SQL = "TRUNCATE TABLE \"SCOTT\".\"T_DBZ_TEST1\" CASCADE";
        parser.parse(SQL, tables);

        final DdlChanges changes = parser.getDdlChanges();
        final List<DdlParserListener.EventType> eventTypes = new ArrayList<>();
        changes.getEventsByDatabase((String dbName, List<DdlParserListener.Event> events) -> {
            events.forEach(event -> eventTypes.add(event.type()));
        });
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.ALTER_TABLE);

        table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "T_DBZ_TEST1"));
        List<String> columnNames = table.retrieveColumnNames();
        assertThat(columnNames).contains("ID");
    }

    @Test
    @FixFor("DBZ-4240")
    public void shouldParseNumberAsteriskWithDefaultPrecision() throws Exception {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");

        String SQL = "CREATE TABLE \"SCOTT\".\"ASTERISK_TEST\" (ID NUMBER(*,0) NOT NULL)";
        parser.parse(SQL, tables);

        DdlChanges changes = parser.getDdlChanges();
        List<DdlParserListener.EventType> eventTypes = getEventTypesFromChanges(changes);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.CREATE_TABLE);

        Table table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "ASTERISK_TEST"));
        assertThat(table.columnWithName("ID").length()).isEqualTo(38);
        assertThat(table.columnWithName("ID").scale().get()).isEqualTo(0);
        assertThat(table.columnWithName("ID").isOptional()).isFalse();

        // Reset changes
        changes.reset();

        SQL = "ALTER TABLE \"SCOTT\".\"ASTERISK_TEST\" MODIFY (ID NUMBER(*,0) NULL);";
        parser.parse(SQL, tables);

        changes = parser.getDdlChanges();
        eventTypes = getEventTypesFromChanges(changes);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.ALTER_TABLE);

        table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "ASTERISK_TEST"));
        assertThat(table.columnWithName("ID").length()).isEqualTo(38);
        assertThat(table.columnWithName("ID").scale().get()).isEqualTo(0);
        assertThat(table.columnWithName("ID").isOptional()).isTrue();
    }

    private List<DdlParserListener.EventType> getEventTypesFromChanges(DdlChanges changes) {
        List<DdlParserListener.EventType> eventTypes = new ArrayList<>();
        changes.getEventsByDatabase((String dbName, List<DdlParserListener.Event> events) -> {
            events.forEach(event -> eventTypes.add(event.type()));
        });
        return eventTypes;
    }

    @Test
    public void shouldParseIndex() {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");
        // create table before create index
        Table table = Table.editor()
                .tableId(new TableId(PDB_NAME, "SCOTT", "T_DBZ_TEST1"))
                .addColumn(Column.editor().name("ID").create())
                .addColumn(Column.editor().name("T_VARCHAR2").type("VARCHAR2").length(10).create())
                .create();
        tables.overwriteTable(table);
        String sql = "create index IDX_TAB_1 on SCOTT.T_DBZ_TEST1(((col_1*2)+1)/3);";
        DdlChanges changes = parser.getDdlChanges();
        changes.reset();
        parser.parse(sql, tables);
        List<DdlParserListener.EventType> eventTypes = getEventTypesFromChanges(changes);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.CREATE_INDEX);
        table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "T_DBZ_TEST1"));
        Index index = table.indexChanges();
        assertThat(index.getIndexColumnExpr().size()).isEqualTo(1);
    }

    private void testColumn(@NotNull Table table, @NotNull String name, boolean isOptional, Integer jdbcType, String typeName, Integer length,
                            Integer scale, Boolean hasDefault, Object defaultValue) {
        Column column = table.columnWithName(name);
        assertThat(column.isOptional()).isEqualTo(isOptional);
        assertThat(column.jdbcType()).isEqualTo(jdbcType);
        assertThat(column.typeName()).isEqualTo(typeName);
        assertThat(column.length()).isEqualTo(length);
        Optional<Integer> oScale = column.scale();
        if (oScale.isPresent()) {
            assertThat(oScale.get()).isEqualTo(scale);
        }
        assertThat(column.hasDefaultValue()).isEqualTo(hasDefault);
        if (column.hasDefaultValue() && column.defaultValueExpression().isPresent()) {
            assertThat(defaultValue.equals(column.defaultValueExpression().get()));
        }
    }

    @Test
    @FixFor("DBZ-5390")
    public void shouldParseCheckConstraint() throws Exception {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");

        String SQL = "CREATE TABLE SCOTT.ASTERISK_TEST\n" + "(\n" + "    COL_1 NUMBER,\n" + "    COL_2 NUMBER NOT NULL,\n" +
                "    CONSTRAINT TEST_1_UNIQUE\n" + "        UNIQUE (COL_1, COL_2),\n" + "    CONSTRAINT CK_NAME\n" +
                "        CHECK (COL_2 IN (7, 8, 9))\n" + ");";
        parser.parse(SQL, tables);

        DdlChanges changes = parser.getDdlChanges();
        List<DdlParserListener.EventType> eventTypes = getEventTypesFromChanges(changes);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.CREATE_TABLE);

        Table table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "ASTERISK_TEST"));
        assertThat(table.columns().size()).isEqualTo(2);
    }

    @Test
    public void shouldParseInLineConstraintAndOutLineConstraint() throws Exception {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");

        String SQL = "create table ASTERISK_TEST\n" + "(\n" + "    col_1 varchar2(10) unique,\n" + "    col_2 varchar2(10),\n" + "    col_3 varchar2(20),\n" +
                "    constraint uk_col_2 unique (col_2, col_3)\n" + ");";
        parser.parse(SQL, tables);

        DdlChanges changes = parser.getDdlChanges();
        List<DdlParserListener.EventType> eventTypes = getEventTypesFromChanges(changes);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.CREATE_TABLE);

        Table table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "ASTERISK_TEST"));
        assertThat(table.columns().size()).isEqualTo(3);
        assertThat(table.uniqueColumns().size()).isEqualTo(3);
    }

    @Test
    public void shouldParseCheckConstraintAndAddColumnInOneDDL() throws Exception {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");
        Table table = Table.editor()
                .tableId(new TableId(PDB_NAME, "SCOTT", "T_DBZ_TEST1"))
                .addColumns(Column.editor().name("COL_0").type("VARCHAR2").length(32).create())
                .addColumn(Column.editor().name("COL_1").create())
                .addColumn(Column.editor().name("COL_3").type("NUMBER").length(1).create())
                .addColumn(Column.editor().name("COL_2").type("VARCHAR2").length(10).create())
                .create();
        tables.overwriteTable(table);
        String sql = "alter table SCOTT.T_DBZ_TEST1\n" + "    modify (col_3 varchar2(32) default 'aaa' not null constraint uk_col_3 unique)\n" +
                "    modify (col_0 varchar2(32) default 'abc' null)\n" + "    add col_5 varchar(64)\n" +
                "    add col_6 number(5) default 1 not null\n" + "    add constraint tab_1_pk primary key (col_6)\n" +
                "    add constraint tab_1_uk_1 unique (col_2,col_1)\n" + "    add constraint TAB_1_CK\n" + "        check (col_6 in (10, 20, 30));";
        DdlChanges changes = parser.getDdlChanges();
        changes.reset();
        parser.parse(sql, tables);
        List<DdlParserListener.EventType> eventTypes = getEventTypesFromChanges(changes);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.ALTER_TABLE);
        table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "T_DBZ_TEST1"));
        assertThat(table.columnWithName("COL_6").length()).isEqualTo(5);
    }

    @Test
    public void shouldParseDropConstraintByName() throws Exception {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");
        Table table = Table.editor().tableId(new TableId(PDB_NAME, "SCOTT", "TAB_1"))
                .addColumn(Column.editor().name("COL_1").type("VARCHAR2").length(32).create())
                .create();
        tables.overwriteTable(table);
        String sql = "ALTER TABLE SCOTT.TAB_1 DROP CONSTRAINT UK_COL_2;";
        DdlChanges changes = parser.getDdlChanges();
        changes.reset();
        parser.parse(sql, tables);
        List<DdlParserListener.EventType> eventTypes = getEventTypesFromChanges(changes);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.ALTER_TABLE);
        table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "TAB_1"));
        assertThat(table.columnWithName("COL_1").length()).isEqualTo(32);
    }

    @Test
    public void shouldParseConstraintUsingIndex() throws Exception {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");

        String SQL = "CREATE TABLE \"SCOTT\".\"DBZ4976\"" + "(\n" +
                "    COL_3 varchar2(32), COL_1 VARCHAR2(10) UNIQUE USING INDEX (CREATE UNIQUE INDEX UID_TEST_UID ON DBZ4976 (COL_1))\n" + ");";
        parser.parse(SQL, tables);
        DdlChanges changes = parser.getDdlChanges();
        changes.reset();

        SQL = "alter table \"SCOTT\".\"DBZ4976\"\n" + "    add col_2 varchar(32)\n" + "   modify col_3 varchar2(64) not null primary key using " +
                "index  idx_t_ddl_0008_01 add constraint uk_2 unique (COL_2) using index (create index idx_tab_col2 on DBZ4976 (col_2))\n";
        parser.parse(SQL, tables);

        changes = parser.getDdlChanges();
        List<DdlParserListener.EventType> eventTypes = getEventTypesFromChanges(changes);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.ALTER_TABLE);

        Table table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "DBZ4976"));
        assertThat(table.columnWithName("COL_1").length()).isEqualTo(10);
    }

    @Test
    public void shouldParserPrimaryKeyConstraintDrop() {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");
        Table table = Table.editor()
                .tableId(new TableId(PDB_NAME, "SCOTT", "TAB_1"))
                .addColumn(Column.editor().name("col_1").type("INT").create())
                .addColumn(Column.editor().name("col_2").type("VARCHAR2").length(32).create())
                .addColumn(Column.editor().name("col_3").type("VARCHAR2").length(32).create())
                .create();
        tables.overwriteTable(table);
        DdlChanges ddlChanges = parser.getDdlChanges();
        String sql = "alter table SCOTT.TAB_1 add col_4 int constraint pk_1 primary key;";
        ddlChanges.reset();
        parser.parse(sql, tables);
        table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "TAB_1"));
        String sql2 = "alter table SCOTT.TAB_1 drop constraint pk_1;";
        ddlChanges.reset();
        parser.parse(sql2, tables);
        ddlChanges = parser.getDdlChanges();
        List<DdlParserListener.EventType> eventTypes = getEventTypesFromChanges(ddlChanges);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.ALTER_TABLE);
        table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "TAB_1"));
        assertThat(table.primaryKeyColumns().size()).isEqualTo(0);
    }

    @Test
    public void shouldParserCheckConstraintInLIne() {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");
        Table table = Table.editor()
                .tableId(new TableId(PDB_NAME, "SCOTT", "TAB_1"))
                .addColumn(Column.editor().name("col_1").type("INT").create())
                .addColumn(Column.editor().name("col_2").type("VARCHAR2").length(32).create())
                .addColumn(Column.editor().name("col_3").type("VARCHAR2").length(32).create())
                .create();
        tables.overwriteTable(table);
        DdlChanges ddlChanges = parser.getDdlChanges();
        String sql = "ALTER TABLE SCOTT.TAB_1 ADD COL_4 INT CONSTRAINT CK_1 CHECK ((COL_4 >5) AND (SQRT(COL_4) > 10));";
        ddlChanges.reset();
        parser.parse(sql, tables);
        ddlChanges = parser.getDdlChanges();
        List<DdlParserListener.EventType> eventTypes = getEventTypesFromChanges(ddlChanges);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.ALTER_TABLE);
        table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "TAB_1"));
        assertThat(table.checkColumns().size()).isEqualTo(1);
    }

    @Test
    public void shouldParserForeignKeyConstraint() {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");
        Table table = Table.editor()
                .tableId(new TableId(PDB_NAME, "SCOTT", "TAB_1"))
                .addColumn(Column.editor().name("col_1").type("INT").create())
                .addColumn(Column.editor().name("col_2").type("VARCHAR2").length(32).create())
                .addColumn(Column.editor().name("col_3").type("VARCHAR2").length(32).create())
                .addColumn(Column.editor().name("col_4").type("VARCHAR2").length(32).create())
                .create();
        tables.overwriteTable(table);
        DdlChanges ddlChanges = parser.getDdlChanges();
        String sql = "ALTER TABLE SCOTT.TAB_1 ADD CONSTRAINT fk_2 FOREIGN KEY(COL3,COL4) REFERENCES SCOTT.TAB_1 (COL1,COL2) ON DELETE SET NULL;";
        ddlChanges.reset();
        parser.parse(sql, tables);
        ddlChanges = parser.getDdlChanges();
        List<DdlParserListener.EventType> eventTypes = getEventTypesFromChanges(ddlChanges);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.ALTER_TABLE);
        table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "TAB_1"));
        assertThat(table.foreignKeyColumns().size()).isEqualTo(1);
    }

    @Test
    public void shouldParseDropMultiConstraint() {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");
        Table table = Table.editor()
                .tableId(new TableId(PDB_NAME, "SCOTT", "TAB_1"))
                .addColumn(Column.editor().name("col_1").type("INT").create())
                .create();
        tables.overwriteTable(table);
        DdlChanges ddlChanges = parser.getDdlChanges();
        String sql = "ALTER TABLE SCOTT.TAB_1\n" + " DROP CONSTRAINT CK_3\n" + " DROP CONSTRAINT UQ_3\n" + " DROP CONSTRAINT UQ_4\n" +
                " DROP CONSTRAINT CK_2\n" + " DROP CONSTRAINT PK_1 CASCADE\n" + " DROP CONSTRAINT CK_1\n" + " DROP CONSTRAINT UQ_2\n" +
                " DROP CONSTRAINT UQ_1;";
        ddlChanges.reset();
        parser.parse(sql, tables);
        ddlChanges = parser.getDdlChanges();
        List<DdlParserListener.EventType> eventTypes = getEventTypesFromChanges(ddlChanges);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.ALTER_TABLE);
        table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "TAB_1"));
        assertThat(table.primaryKeyColumnChanges().size()).isEqualTo(8);
    }

    @Test
    public void shouldParseDropColumnCascade() {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");
        Table table = Table.editor()
                .tableId(new TableId(PDB_NAME, "SCOTT", "TAB_1"))
                .addColumn(Column.editor().name("col_1").type("INT").create())
                .addColumn(Column.editor().name("col_2").type("VARCHAR2").length(32).create())
                .create();
        tables.overwriteTable(table);
        DdlChanges ddlChanges = parser.getDdlChanges();
        String sql = "ALTER TABLE SCOTT.TAB_1 DROP COLUMN COL_2 CASCADE CONSTRAINTS;";
        ddlChanges.reset();
        parser.parse(sql, tables);
        ddlChanges = parser.getDdlChanges();
        List<DdlParserListener.EventType> eventTypes = getEventTypesFromChanges(ddlChanges);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.ALTER_TABLE);
        table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "TAB_1"));
        assertThat(table.columns().size()).isEqualTo(1);
    }

    @Test
    public void parseCreateTableWithPkAndFK() {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");
        String sql = "create table SCOTT.TAB_1\n" + "(\n" + "    col1 int\n" + "        constraint pk_1 primary key,\n" +
                "    col2 varchar(50),\n" + "    col3 varchar(50),\n" + "    col4 int references PUSHENG.t_ddl_0026 (col1)\n" + ");";
        DdlChanges ddlChanges = parser.getDdlChanges();
        ddlChanges.reset();
        parser.parse(sql, tables);
        ddlChanges = parser.getDdlChanges();
        List<DdlParserListener.EventType> eventTypes = getEventTypesFromChanges(ddlChanges);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.CREATE_TABLE);
        Table table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "TAB_1"));
        assertThat(table.columns().size()).isEqualTo(4);
    }

    @Test
    public void testPrimaryKeyName() {
        parser.setCurrentDatabase(PDB_NAME);
        parser.setCurrentSchema("SCOTT");
        String sql = "create table SCOTT.TAB_1\n" + "(\n" + "    col1 int,\n" + "    col2 varchar(50),\n" + "    col3 int,\n" +
                "    col4 varchar(60)\n" + ");";
        DdlChanges ddlChanges = parser.getDdlChanges();
        ddlChanges.reset();
        parser.parse(sql, tables);
        ddlChanges = parser.getDdlChanges();
        String sql2 = "alter table SCOTT.TAB_1\n" + "    add constraint pk_1 primary key (col1);";
        ddlChanges.reset();
        parser.parse(sql2, tables);
        String sql3 = "alter table SCOTT.TAB_1\n" + "    drop constraint pk_1;";
        ddlChanges.reset();
        parser.parse(sql3, tables);
        String sql4 = "alter table SCOTT.TAB_1\n" + "    add constraint pk_2 primary key (col2, col3, col4);";
        ddlChanges.reset();
        parser.parse(sql4, tables);
        String sql5 = "alter table SCOTT.TAB_1\n" + "    drop constraint pk_2;";
        ddlChanges.reset();
        parser.parse(sql5, tables);
        String sql6 = "alter table SCOTT.TAB_1\n" + "    modify col1 primary key;";
        ddlChanges.reset();
        parser.parse(sql6, tables);
        String sql7 = "alter table SCOTT.TAB_1 drop primary key;";
        ddlChanges.reset();
        parser.parse(sql7, tables);
        List<DdlParserListener.EventType> eventTypes = getEventTypesFromChanges(ddlChanges);
        assertThat(eventTypes).containsExactly(DdlParserListener.EventType.ALTER_TABLE);
        Table table = tables.forTable(new TableId(PDB_NAME, "SCOTT", "TAB_1"));
        assertThat(table.primaryConstraintName().size()).isEqualTo(0);

    }
}
