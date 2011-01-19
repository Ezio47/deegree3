//$HeadURL: svn+ssh://lbuesching@svn.wald.intevation.de/deegree/base/trunk/resources/eclipse/files_template.xml $
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2010 by:
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
package org.deegree.cs.persistence.deegree.d3;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.bind.JAXBException;

import org.deegree.commons.xml.XMLAdapter;
import org.deegree.commons.xml.jaxb.JAXBUtils;
import org.deegree.cs.exceptions.CRSStoreException;
import org.deegree.cs.persistence.CRSStore;
import org.deegree.cs.persistence.CRSStoreProvider;
import org.deegree.cs.persistence.deegree.DeegreeCRSStore;
import org.deegree.cs.persistence.deegree.d3.jaxb.DeegreeCRSStoreConfig;
import org.deegree.cs.transformations.TransformationFactory.DSTransform;
import org.slf4j.Logger;

/**
 * {@link DeegreeCRSStoreProvider} for the {@link DeegreeCRSStore} (deegree3!)
 * 
 * @author <a href="mailto:buesching@lat-lon.de">Lyn Buesching</a>
 * @author last edited by: $Author: lyn $
 * 
 * @version $Revision: $, $Date: $
 */
public class DeegreeCRSStoreProvider implements CRSStoreProvider {

    private static final Logger LOG = getLogger( DeegreeCRSStoreProvider.class );

    private static final String CONFIG_NS = "http://www.deegree.org/crs/stores/deegree";

    private static final String CONFIG_JAXB_PACKAGE = "org.deegree.cs.persistence.deegree.d3.jaxb";

    private static final String CONFIG_SCHEMA = "/META-INF/schemas/crs/stores/deegree/3.1.0/deegree.xsd";

    private static final String CONFIG_TEMPLATE = "/META-INF/schemas/crs/stores/deegree/3.1.0/example.xml";

    public String getConfigNamespace() {
        return CONFIG_NS;
    }

    public URL getConfigSchema() {
        return DeegreeCRSStoreProvider.class.getResource( CONFIG_SCHEMA );
    }

    public URL getConfigTemplate() {
        return DeegreeCRSStoreProvider.class.getResource( CONFIG_TEMPLATE );
    }

    public CRSStore getCRSStore( URL configURL )
                            throws CRSStoreException {
        DeegreeCRSStore<StAXResource> crsStore = null;
        try {
            DeegreeCRSStoreConfig config = (DeegreeCRSStoreConfig) JAXBUtils.unmarshall( CONFIG_JAXB_PACKAGE,
                                                                                         CONFIG_SCHEMA, configURL );
            XMLAdapter resolver = new XMLAdapter();
            resolver.setSystemId( configURL.toString() );

            String parserFile = config.getFile();
            if ( parserFile == null || parserFile.trim().length() == 0 ) {
                String msg = "Error in crs store configuration file '" + configURL + "': parserFile must not be null!";
                LOG.error( msg );
                throw new CRSStoreException( msg );
            }
            crsStore = new DeegreeCRSStore<StAXResource>( DSTransform.fromSchema( config ) );
            Parser parser = new Parser( crsStore, resolver.resolve( parserFile ) );
            crsStore.setResolver( parser );
        } catch ( JAXBException e ) {
            String msg = "Error in crs store configuration file '" + configURL + "': " + e.getMessage();
            LOG.error( msg );
            throw new CRSStoreException( msg, e );
        } catch ( MalformedURLException e ) {
            String msg = "Error in file declaration in the crs store configuration file '" + configURL + "': "
                         + e.getMessage();
            LOG.error( msg );
            throw new CRSStoreException( msg, e );
        }
        return crsStore;
    }
}
