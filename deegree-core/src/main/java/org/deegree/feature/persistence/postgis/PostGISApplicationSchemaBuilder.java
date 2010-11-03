//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.feature.persistence.postgis;

import static javax.xml.XMLConstants.DEFAULT_NS_PREFIX;
import static javax.xml.XMLConstants.NULL_NS_URI;
import static org.deegree.feature.persistence.BlobCodec.Compression.NONE;
import static org.deegree.feature.types.property.GeometryPropertyType.CoordinateDimension.DIM_2;
import static org.deegree.feature.types.property.GeometryPropertyType.CoordinateDimension.DIM_3;
import static org.deegree.feature.types.property.GeometryPropertyType.GeometryType.GEOMETRY;
import static org.deegree.feature.types.property.GeometryPropertyType.GeometryType.LINE_STRING;
import static org.deegree.feature.types.property.GeometryPropertyType.GeometryType.MULTI_GEOMETRY;
import static org.deegree.feature.types.property.GeometryPropertyType.GeometryType.MULTI_LINE_STRING;
import static org.deegree.feature.types.property.GeometryPropertyType.GeometryType.MULTI_POINT;
import static org.deegree.feature.types.property.GeometryPropertyType.GeometryType.MULTI_POLYGON;
import static org.deegree.feature.types.property.GeometryPropertyType.GeometryType.POINT;
import static org.deegree.feature.types.property.GeometryPropertyType.GeometryType.POLYGON;
import static org.deegree.feature.types.property.ValueRepresentation.BOTH;
import static org.deegree.feature.types.property.ValueRepresentation.INLINE;
import static org.deegree.gml.GMLVersion.GML_32;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.deegree.commons.jdbc.ConnectionManager;
import org.deegree.commons.tom.primitive.PrimitiveType;
import org.deegree.commons.utils.JDBCUtils;
import org.deegree.commons.utils.Pair;
import org.deegree.commons.xml.NamespaceContext;
import org.deegree.cs.CRS;
import org.deegree.feature.persistence.BlobCodec;
import org.deegree.feature.persistence.FeatureStoreException;
import org.deegree.feature.persistence.mapping.BBoxTableMapping;
import org.deegree.feature.persistence.mapping.BlobMapping;
import org.deegree.feature.persistence.mapping.DBField;
import org.deegree.feature.persistence.mapping.FeatureTypeMapping;
import org.deegree.feature.persistence.mapping.JoinChain;
import org.deegree.feature.persistence.mapping.MappedApplicationSchema;
import org.deegree.feature.persistence.mapping.MappingExpression;
import org.deegree.feature.persistence.mapping.antlr.FMLLexer;
import org.deegree.feature.persistence.mapping.antlr.FMLParser;
import org.deegree.feature.persistence.mapping.property.CompoundMapping;
import org.deegree.feature.persistence.mapping.property.FeatureMapping;
import org.deegree.feature.persistence.mapping.property.GeometryMapping;
import org.deegree.feature.persistence.mapping.property.Mapping;
import org.deegree.feature.persistence.mapping.property.PrimitiveMapping;
import org.deegree.feature.persistence.postgis.jaxb.AbstractPropertyDecl;
import org.deegree.feature.persistence.postgis.jaxb.CodePropertyDecl;
import org.deegree.feature.persistence.postgis.jaxb.CustomMapping;
import org.deegree.feature.persistence.postgis.jaxb.CustomPropertyDecl;
import org.deegree.feature.persistence.postgis.jaxb.FeaturePropertyDecl;
import org.deegree.feature.persistence.postgis.jaxb.FeatureTypeDecl;
import org.deegree.feature.persistence.postgis.jaxb.GeometryPropertyDecl;
import org.deegree.feature.persistence.postgis.jaxb.MeasurePropertyDecl;
import org.deegree.feature.persistence.postgis.jaxb.SimplePropertyDecl;
import org.deegree.feature.types.ApplicationSchema;
import org.deegree.feature.types.FeatureType;
import org.deegree.feature.types.GenericFeatureType;
import org.deegree.feature.types.property.CustomPropertyType;
import org.deegree.feature.types.property.FeaturePropertyType;
import org.deegree.feature.types.property.GeometryPropertyType;
import org.deegree.feature.types.property.PropertyType;
import org.deegree.feature.types.property.SimplePropertyType;
import org.deegree.feature.types.property.GeometryPropertyType.CoordinateDimension;
import org.deegree.feature.types.property.GeometryPropertyType.GeometryType;
import org.deegree.filter.expression.PropertyName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link MappedApplicationSchema} instances from feature type declarations / relational mapping information.
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
class PostGISApplicationSchemaBuilder {

    private static final Logger LOG = LoggerFactory.getLogger( PostGISApplicationSchemaBuilder.class );

    private final String connId;

    private final String dbSchema;

    private Map<QName, FeatureType> ftNameToFt = new HashMap<QName, FeatureType>();

    private Map<QName, FeatureTypeMapping> ftNameToMapping = new HashMap<QName, FeatureTypeMapping>();

    private NamespaceContext nsContext = null;

    private Connection conn;

    private DatabaseMetaData md;

    /**
     * Creates a new {@link MappedApplicationSchema} instance.
     * 
     * @param appSchema
     *            application schema, can be <code>null</code> (for relational-only mappings)
     * @param ftDecls
     *            feature type declarations, can be <code>null</code> (for BLOB-only mappings)
     * @param jdbcConnId
     *            identifier of the JDBC connection, must not be <code>null</code>
     * @param dbSchema
     *            PostgreSQL database schema, can be <code>null</code>
     * @param storageCRS
     *            CRS used for storing geometries, must not be <code>null</code>
     * @return mapped application schema, never <code>null</code>
     * @throws SQLException
     * @throws FeatureStoreException
     */
    static MappedApplicationSchema build( ApplicationSchema appSchema, List<FeatureTypeDecl> ftDecls,
                                          String jdbcConnId, String dbSchema, CRS storageCRS, NamespaceContext nsContext )
                            throws SQLException, FeatureStoreException {

        MappedApplicationSchema mappedSchema = null;

        if ( appSchema != null ) {
            BBoxTableMapping bboxMapping = new BBoxTableMapping( storageCRS );
            BlobMapping blobMapping = new BlobMapping( "GML_OBJECTS", storageCRS, new BlobCodec( GML_32, NONE ) );

            PostGISApplicationSchemaBuilder builder = new PostGISApplicationSchemaBuilder( ftDecls, jdbcConnId,
                                                                                           dbSchema, nsContext );
            FeatureTypeMapping[] ftMappings = builder.ftNameToMapping.values().toArray(
                                                                                        new FeatureTypeMapping[builder.ftNameToMapping.size()] );

            mappedSchema = new MappedApplicationSchema( appSchema.getFeatureTypes(), appSchema.getFtToSuperFt(),
                                                        appSchema.getNamespaceBindings(), appSchema.getXSModel(),
                                                        ftMappings, storageCRS, bboxMapping, blobMapping );
        } else {
            // relational mode
            PostGISApplicationSchemaBuilder builder = new PostGISApplicationSchemaBuilder( ftDecls, jdbcConnId,
                                                                                           dbSchema, nsContext );
            FeatureType[] fts = builder.ftNameToFt.values().toArray( new FeatureType[builder.ftNameToFt.size()] );
            FeatureTypeMapping[] ftMappings = builder.ftNameToMapping.values().toArray(
                                                                                        new FeatureTypeMapping[builder.ftNameToMapping.size()] );
            mappedSchema = new MappedApplicationSchema( fts, null, null, null, ftMappings, storageCRS, null, null );
        }

        return mappedSchema;
    }

    private PostGISApplicationSchemaBuilder( List<FeatureTypeDecl> ftDecls, String connId, String dbSchema,
                                             NamespaceContext nsContext ) throws SQLException, FeatureStoreException {

        this.connId = connId;
        this.dbSchema = dbSchema;
        this.nsContext = nsContext;

        try {
            for ( FeatureTypeDecl ftDecl : ftDecls ) {
                process( ftDecl );
            }
        } finally {
            JDBCUtils.close( conn );
        }
    }

    private void process( FeatureTypeDecl ftDecl )
                            throws FeatureStoreException, SQLException {

        String table = ftDecl.getTable();
        if ( table == null || table.isEmpty() ) {
            String msg = "Feature type element without or with empty table attribute.";
            throw new FeatureStoreException( msg );
        }

        LOG.debug( "Processing feature type mapping for table '" + table + "'." );
        QName ftName = ftDecl.getName();
        if ( ftName == null ) {
            LOG.debug( "Using table name for feature type." );
            ftName = new QName( table );
        }
        ftName = makeFullyQualified( ftName, "app", "http://www.deegree.org/app" );
        LOG.debug( "Feature type name: '" + ftName + "'." );

        boolean isAbstract = ftDecl.isAbstract() == null ? false : ftDecl.isAbstract();

        QName parentFtName = ftDecl.getParent();
        if ( parentFtName != null ) {
            parentFtName = makeFullyQualified( parentFtName, "app", "http://www.deegree.org/app" );
        }

        String fidMapping = ftDecl.getFidMapping();

        List<JAXBElement<? extends AbstractPropertyDecl>> propDecls = ftDecl.getAbstractProperty();
        if ( propDecls != null && !propDecls.isEmpty() ) {
            process( table, ftName, isAbstract, propDecls, fidMapping );
        } else {
            process( table, ftName, isAbstract, fidMapping );
        }
    }

    private void process( String table, QName ftName, boolean isAbstract, String fidMapping )
                            throws SQLException {

        LOG.debug( "Determining properties for feature type '" + ftName + "' from table '" + table + "'" );

        List<PropertyType> pts = new ArrayList<PropertyType>();
        String backendSrs = null;
        Map<QName, Mapping> propToColumn = new HashMap<QName, Mapping>();

        DatabaseMetaData md = getDBMetadata();
        ResultSet rs = null;
        try {
            // TODO schema
            rs = md.getColumns( null, "public", table.toLowerCase(), "%" );
            while ( rs.next() ) {
                String column = rs.getString( 4 );
                int sqlType = rs.getInt( 5 );
                String typeName = rs.getString( 6 );
                String isNullable = rs.getString( 18 );
                boolean isAutoincrement = "YES".equals( rs.getString( 23 ) );
                LOG.debug( "Deriving property type for column '" + column + "', typeName: '" + typeName
                           + "', typeCode: '" + sqlType + "', isNullable: '" + isNullable + "', isAutoincrement:' "
                           + isAutoincrement + "'" );

                if ( fidMapping == null && isAutoincrement ) {
                    fidMapping = column;
                } else {
                    DBField dbField = new DBField( column );
                    QName ptName = makeFullyQualified( new QName( column ), ftName.getPrefix(),
                                                       ftName.getNamespaceURI() );
                    if ( typeName.equals( "geometry" ) ) {
                        String srid = "-1";
                        CRS crs = new CRS( "EPSG:4326", true );
                        CoordinateDimension dim = DIM_2;
                        GeometryPropertyType.GeometryType geomType = GeometryType.GEOMETRY;
                        Connection conn = getConnection();
                        Statement stmt = null;
                        ResultSet rs2 = null;
                        try {
                            stmt = conn.createStatement();
                            String sql = "SELECT coord_dimension,srid,type FROM public.geometry_columns WHERE f_table_schema='"
                                         + "public"
                                         + "' AND f_table_name='"
                                         + table.toLowerCase()
                                         + "' AND f_geometry_column='" + column.toLowerCase() + "'";
                            rs2 = stmt.executeQuery( sql );
                            rs2.next();
                            if ( rs2.getInt( 2 ) != -1 ) {
                                crs = new CRS( "EPSG:" + rs2.getInt( 2 ), true );
                                crs.getWrappedCRS();
                            }
                            if ( rs2.getInt( 1 ) == 3 ) {
                                dim = DIM_3;
                            }
                            srid = "" + rs2.getInt( 2 );
                            geomType = getGeometryType( rs2.getString( 3 ) );
                            LOG.debug( "Derived geometry type: " + geomType + ", crs: " + crs + ", srid: " + srid
                                       + ", dim: " + dim + "" );
                        } catch ( Exception e ) {
                            LOG.warn( "Unable to determine actual geometry column details: " + e.getMessage()
                                      + ". Using defaults." );
                        } finally {
                            JDBCUtils.close( rs2, stmt, null, LOG );
                        }

                        PropertyType pt = new GeometryPropertyType( ptName, 0, 1, false, false, null, geomType, dim,
                                                                    INLINE );
                        pts.add( pt );
                        PropertyName path = new PropertyName( ptName );
                        GeometryMapping mapping = new GeometryMapping( path, dbField, geomType, dim, crs, srid, null );
                        propToColumn.put( ptName, mapping );
                    } else {
                        try {
                            PrimitiveType type = PrimitiveType.determinePrimitiveType( sqlType );
                            PropertyType pt = new SimplePropertyType( ptName, 0, 1, type, false, false, null );
                            pts.add( pt );
                            PropertyName path = new PropertyName( ptName );
                            PrimitiveMapping mapping = new PrimitiveMapping( path, dbField, type, null );
                            propToColumn.put( ptName, mapping );
                        } catch ( IllegalArgumentException e ) {
                            LOG.warn( "Skipping column with type code '" + sqlType + "' from list of properties:"
                                      + e.getMessage() );
                        }
                    }
                }
            }
        } finally {
            JDBCUtils.close( rs );
        }

        FeatureType ft = new GenericFeatureType( ftName, pts, isAbstract );
        ftNameToFt.put( ftName, ft );

        FeatureTypeMapping ftMapping = new FeatureTypeMapping( ftName, table, fidMapping, propToColumn, backendSrs );
        ftNameToMapping.put( ftName, ftMapping );
    }

    private void process( String table, QName ftName, boolean isAbstract,
                          List<JAXBElement<? extends AbstractPropertyDecl>> propDecls, String fidMapping ) {

        List<PropertyType> pts = new ArrayList<PropertyType>();
        String backendSrs = null;
        Map<QName, Mapping> propToColumn = new HashMap<QName, Mapping>();
        for ( JAXBElement<? extends AbstractPropertyDecl> propDeclEl : propDecls ) {
            AbstractPropertyDecl propDecl = propDeclEl.getValue();
            Pair<PropertyType, Mapping> pt = process( propDecl );

            if ( pt.first == null ) {
                // TODO
                continue;
            }

            pts.add( pt.first );
            propToColumn.put( pt.first.getName(), pt.second );

            // TODO what about different srids for multiple geometry properties?
            if ( propDecl instanceof GeometryPropertyDecl ) {
                GeometryPropertyDecl geoPropDecl = (GeometryPropertyDecl) propDecl;
                if ( geoPropDecl.getSrid() != null ) {
                    backendSrs = geoPropDecl.getSrid().toString();
                }
            }
        }

        FeatureType ft = new GenericFeatureType( ftName, pts, isAbstract );
        ftNameToFt.put( ftName, ft );

        FeatureTypeMapping ftMapping = new FeatureTypeMapping( ftName, table, fidMapping, propToColumn, backendSrs );
        ftNameToMapping.put( ftName, ftMapping );
    }

    private Pair<PropertyType, Mapping> process( AbstractPropertyDecl propDecl ) {

        QName ptName = propDecl.getName();

        int minOccurs = propDecl.getMinOccurs() == null ? 1 : propDecl.getMinOccurs().intValue();
        int maxOccurs = 1;

        if ( propDecl.getMaxOccurs() != null ) {
            if ( propDecl.getMaxOccurs().equals( "unbounded" ) ) {
                maxOccurs = -1;
            } else {
                maxOccurs = Integer.parseInt( propDecl.getMaxOccurs() );
            }
        }

        MappingExpression mapping = parseMappingExpression( propDecl.getMapping() );
        JoinChain joinedTable = null;
        if ( propDecl.getJoinedTable() != null ) {
            joinedTable = (JoinChain) parseMappingExpression( propDecl.getJoinedTable().getValue() );
        }

        PropertyType pt = null;
        Mapping m = null;
        PropertyName path = new PropertyName( ptName.toString(), nsContext );
        if ( propDecl instanceof SimplePropertyDecl ) {
            SimplePropertyDecl spt = (SimplePropertyDecl) propDecl;
            PrimitiveType primType = getPrimitiveType( spt.getType() );
            pt = new SimplePropertyType( ptName, minOccurs, maxOccurs, primType, false, false, null );
            m = new PrimitiveMapping( path, mapping, primType, joinedTable );
        } else if ( propDecl instanceof GeometryPropertyDecl ) {
            GeometryPropertyDecl gpt = (GeometryPropertyDecl) propDecl;
            pt = new GeometryPropertyType( ptName, minOccurs, maxOccurs, false, false, null, GEOMETRY, DIM_2, BOTH );
            m = new GeometryMapping( path, mapping, GEOMETRY, DIM_2, new CRS( "EPSG:4326", true ), "-1", joinedTable );
        } else if ( propDecl instanceof FeaturePropertyDecl ) {
            FeaturePropertyDecl fpt = (FeaturePropertyDecl) propDecl;
            pt = new FeaturePropertyType( ptName, minOccurs, maxOccurs, false, false, null, fpt.getType(), BOTH );
            m = new FeatureMapping( path, mapping, fpt.getType(), joinedTable );
        } else if ( propDecl instanceof CustomPropertyDecl ) {
            CustomPropertyDecl cpt = (CustomPropertyDecl) propDecl;
            pt = new CustomPropertyType( ptName, minOccurs, maxOccurs, null, false, false, null );
            m = new CompoundMapping( path, mapping, process( cpt.getAbstractCustomMapping() ), joinedTable );
        } else if ( propDecl instanceof CodePropertyDecl ) {
            LOG.warn( "TODO: CodePropertyDecl " );
        } else if ( propDecl instanceof MeasurePropertyDecl ) {
            LOG.warn( "TODO: MeasurePropertyDecl " );
        } else {
            throw new RuntimeException( "Internal error: Unhandled property JAXB property type: " + propDecl.getClass() );
        }

        return new Pair<PropertyType, Mapping>( pt, m );
    }

    private List<Mapping> process( List<JAXBElement<? extends CustomMapping>> customMappings ) {
        List<Mapping> mappings = new ArrayList<Mapping>( customMappings.size() );
        for ( JAXBElement<? extends CustomMapping> customMappingEl : customMappings ) {
            CustomMapping customMapping = customMappingEl.getValue();

            String path = customMapping.getPath();
            MappingExpression mapping = null;
            if ( customMapping.getMapping() != null ) {
                ANTLRStringStream in = new ANTLRStringStream( customMapping.getMapping() );
                FMLLexer lexer = new FMLLexer( in );
                CommonTokenStream tokens = new CommonTokenStream( lexer );
                FMLParser parser = new FMLParser( tokens );
                try {
                    mapping = parser.mappingExpr().value;
                } catch ( RecognitionException e ) {
                    LOG.warn( "Unable to parse mapping expression '" + customMapping.getMapping() + "'" );
                }
            }

            PropertyName propName = new PropertyName( path, nsContext );
            if ( customMapping instanceof org.deegree.feature.persistence.postgis.jaxb.PrimitiveMapping ) {
                org.deegree.feature.persistence.postgis.jaxb.PrimitiveMapping primitiveMapping = (org.deegree.feature.persistence.postgis.jaxb.PrimitiveMapping) customMapping;
                mappings.add( new PrimitiveMapping( propName, mapping, getPrimitiveType( primitiveMapping.getType() ),
                                                    null ) );
            } else if ( customMapping instanceof org.deegree.feature.persistence.postgis.jaxb.GeometryMapping ) {
                org.deegree.feature.persistence.postgis.jaxb.GeometryMapping geometryMapping = (org.deegree.feature.persistence.postgis.jaxb.GeometryMapping) customMapping;
                mappings.add( new GeometryMapping( propName, mapping, GEOMETRY, DIM_2, new CRS( "EPSG:4326", true ),
                                                   "-1", null ) );
            } else if ( customMapping instanceof org.deegree.feature.persistence.postgis.jaxb.FeatureMapping ) {
                org.deegree.feature.persistence.postgis.jaxb.FeatureMapping featureMapping = (org.deegree.feature.persistence.postgis.jaxb.FeatureMapping) customMapping;
                mappings.add( new FeatureMapping( propName, mapping, featureMapping.getType(), null ) );
            } else if ( customMapping instanceof org.deegree.feature.persistence.postgis.jaxb.CustomMapping ) {
                org.deegree.feature.persistence.postgis.jaxb.ComplexMapping compoundMapping = (org.deegree.feature.persistence.postgis.jaxb.ComplexMapping) customMapping;
                List<Mapping> particles = process( compoundMapping.getAbstractCustomMapping() );
                JoinChain joinedTable = null;
                if ( compoundMapping.getJoinedTable() != null ) {

                    joinedTable = (JoinChain) parseMappingExpression( compoundMapping.getJoinedTable().getValue() );
                }
                mappings.add( new CompoundMapping( propName, mapping, particles, joinedTable ) );
            } else {
                throw new RuntimeException( "Internal error. Unexpected JAXB type '" + customMapping.getClass() + "'." );
            }
        }
        return mappings;
    }

    private PrimitiveType getPrimitiveType( org.deegree.feature.persistence.postgis.jaxb.PrimitiveType type ) {
        switch ( type ) {
        case BOOLEAN:
            return PrimitiveType.BOOLEAN;
        case DATE:
            return PrimitiveType.DATE;
        case DATE_TIME:
            return PrimitiveType.DATE_TIME;
        case DECIMAL:
            return PrimitiveType.DECIMAL;
        case DOUBLE:
            return PrimitiveType.DOUBLE;
        case INTEGER:
            return PrimitiveType.INTEGER;
        case STRING:
            return PrimitiveType.STRING;
        case TIME:
            return PrimitiveType.TIME;
        }
        throw new RuntimeException( "Internal error: Unhandled JAXB primitive type: " + type );
    }

    private GeometryType getGeometryType( String pgType ) {
        if ( "GEOMETRY".equals( pgType ) ) {
            return GEOMETRY;
        } else if ( "POINT".equals( pgType ) ) {
            return POINT;
        } else if ( "LINESTRING".equals( pgType ) ) {
            return LINE_STRING;
        } else if ( "POLYGON".equals( pgType ) ) {
            return POLYGON;
        } else if ( "MULTIPOINT".equals( pgType ) ) {
            return MULTI_POINT;
        } else if ( "MULTILINESTRING".equals( pgType ) ) {
            return MULTI_LINE_STRING;
        } else if ( "MULTIPOLYGON".equals( pgType ) ) {
            return MULTI_POLYGON;
        } else if ( "GEOMETRYCOLLECTION".equals( pgType ) ) {
            return MULTI_GEOMETRY;
        }
        LOG.warn( "Unknown PostGIS geometry type '" + pgType + "'. Interpreting as generic geometry." );
        return GEOMETRY;
    }

    private MappingExpression parseMappingExpression( String s ) {
        MappingExpression mapping = null;
        if ( s != null ) {
            ANTLRStringStream in = new ANTLRStringStream( s );
            FMLLexer lexer = new FMLLexer( in );
            CommonTokenStream tokens = new CommonTokenStream( lexer );
            FMLParser parser = new FMLParser( tokens );
            try {
                mapping = parser.mappingExpr().value;
            } catch ( RecognitionException e ) {
                LOG.warn( "Unable to parse mapping expression '" + s + "': " + e.getMessage() );
            }
        }
        return mapping;
    }

    private Connection getConnection()
                            throws SQLException {
        if ( conn == null ) {
            conn = ConnectionManager.getConnection( connId );
        }
        return conn;
    }

    private DatabaseMetaData getDBMetadata()
                            throws SQLException {
        if ( md == null ) {
            Connection conn = getConnection();
            md = conn.getMetaData();
        }
        return md;
    }

    private QName makeFullyQualified( QName qName, String defaultPrefix, String defaultNamespace ) {
        String prefix = qName.getPrefix();
        String namespace = qName.getNamespaceURI();
        String localPart = qName.getLocalPart();
        if ( DEFAULT_NS_PREFIX.equals( prefix ) ) {
            prefix = defaultPrefix;
        }
        if ( NULL_NS_URI.equals( namespace ) ) {
            namespace = defaultNamespace;
        }
        return new QName( namespace, localPart, prefix );
    }
}