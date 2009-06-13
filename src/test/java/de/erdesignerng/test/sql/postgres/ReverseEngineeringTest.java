/**
 * Mogwai ERDesigner. Copyright (C) 2002 The Mogwai Project.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package de.erdesignerng.test.sql.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;

import de.erdesignerng.dialect.Dialect;
import de.erdesignerng.dialect.ReverseEngineeringOptions;
import de.erdesignerng.dialect.ReverseEngineeringStrategy;
import de.erdesignerng.dialect.TableNamingEnum;
import de.erdesignerng.dialect.h2.H2Dialect;
import de.erdesignerng.model.Attribute;
import de.erdesignerng.model.Index;
import de.erdesignerng.model.IndexExpression;
import de.erdesignerng.model.Model;
import de.erdesignerng.model.Relation;
import de.erdesignerng.model.Table;
import de.erdesignerng.model.View;
import de.erdesignerng.modificationtracker.HistoryModificationTracker;
import de.erdesignerng.test.sql.AbstractReverseEngineeringTest;

/**
 * Test for XML based model io.
 * 
 * @author $Author: mirkosertic $
 * @version $Date: 2008-11-16 17:48:26 $
 */
public class ReverseEngineeringTest extends AbstractReverseEngineeringTest {

    public void testReverseEngineerPostgreSQL() throws Exception {

        Class.forName("org.postgresql.Driver").newInstance();
        Connection theConnection = null;
        try {
            theConnection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/mogwai", "mogwai", "mogwai");

            // 
            Statement theStatement = theConnection.createStatement();
            try {
                theStatement.execute("DROP SCHEMA schemaa CASCADE");
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                theStatement.execute("DROP SCHEMA schemab CASCADE");
            } catch (Exception e) {
                e.printStackTrace();
            }

            loadSQL(theConnection, "db.sql");

            Dialect theDialect = new H2Dialect();
            ReverseEngineeringStrategy<H2Dialect> theST = theDialect.getReverseEngineeringStrategy();

            Model theModel = new Model();
            theModel.setDialect(theDialect);
            theModel.setModificationTracker(new HistoryModificationTracker(theModel));

            ReverseEngineeringOptions theOptions = new ReverseEngineeringOptions();
            theOptions.setTableNaming(TableNamingEnum.INCLUDE_SCHEMA);
            theOptions.getTableEntries().addAll(
                    theST.getTablesForSchemas(theConnection, theST.getSchemaEntries(theConnection)));

            theST.updateModelFromConnection(theModel, new EmptyWorldConnector(), theConnection, theOptions,
                    new EmptyReverseEngineeringNotifier());

            // Implement Unit Tests here
            Table theTable = theModel.getTables().findByNameAndSchema("TABLE1", "schemaa");
            assertTrue(theTable != null);
            assertTrue("Tablecomment".equals(theTable.getComment()));
            Attribute theAttribute = theTable.getAttributes().findByName("TB1_1");
            assertTrue(theAttribute != null);
            assertTrue(theAttribute.isNullable() == false);
            assertTrue(theAttribute.getDatatype().getName().equals("varchar"));
            assertTrue(theAttribute.getSize() == 20);
            assertTrue("Columncomment".equals(theAttribute.getComment()));
            theAttribute = theTable.getAttributes().findByName("TB1_2");
            assertTrue(theAttribute != null);
            assertTrue(theAttribute.isNullable());
            assertTrue(theAttribute.getDatatype().getName().equals("varchar"));
            assertTrue(theAttribute.getSize() == 100);
            theAttribute = theTable.getAttributes().findByName("TB1_3");
            assertTrue(theAttribute != null);
            assertTrue(theAttribute.isNullable() == false);
            assertTrue(theAttribute.getDatatype().getName().equals("numeric"));
            assertTrue(theAttribute.getSize() == 20);
            assertTrue(theAttribute.getFraction() == 5);

            Index thePK = theTable.getPrimarykey();
            assertTrue("PK1".equals(thePK.getName()));
            assertTrue(thePK != null);
            assertTrue(thePK.getExpressions().findByAttributeName("TB1_1") != null);

            theTable = theModel.getTables().findByNameAndSchema("TABLE1", "schemab");
            assertTrue(theTable != null);
            theAttribute = theTable.getAttributes().findByName("TB2_1");
            assertTrue(theAttribute != null);
            theAttribute = theTable.getAttributes().findByName("TB2_2");
            assertTrue(theAttribute != null);
            theAttribute = theTable.getAttributes().findByName("TB2_3");
            assertTrue(theAttribute != null);

            View theView = theModel.getViews().findByNameAndSchema("VIEW1", "schemab");
            assertTrue(theView != null);

            theView = theModel.getViews().findByNameAndSchema("VIEW1", "schemaa");
            assertTrue(theView == null);

            theTable = theModel.getTables().findByNameAndSchema("TABLE2", "schemab");
            Index theIndex = theTable.getIndexes().findByName("TABL22_IDX3");
            assertTrue(theIndex != null);
            assertTrue(theIndex.getExpressions().size() == 1);
            assertTrue(theIndex.getExpressions().findByAttributeName("TB3_2") == null);
            assertTrue("upper((tb3_2)::text)".equals(theIndex.getExpressions().get(0).getExpression()));

            assertTrue(theModel.getRelations().size() == 1);

            Relation theRelation = theModel.getRelations().findByName("FK1");
            assertTrue(theRelation != null);
            assertTrue("TABLE1".equals(theRelation.getImportingTable().getName()));
            assertTrue("schemab".equals(theRelation.getImportingTable().getSchema()));

            assertTrue("TABLE1".equals(theRelation.getExportingTable().getName()));
            assertTrue("schemaa".equals(theRelation.getExportingTable().getSchema()));

            assertTrue(theRelation.getMapping().size() == 1);
            Map.Entry<IndexExpression, Attribute> theEntry = theRelation.getMapping().entrySet().iterator().next();
            assertTrue("TB2_1".equals(theEntry.getValue().getName()));
            assertTrue("TB1_1".equals(theEntry.getKey().getAttributeRef().getName()));

        } finally {
            if (theConnection != null) {
                theConnection.close();
            }
        }
    }
}