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
package org.deegree.cs.persistence.deegree.db;

import java.util.List;

import org.deegree.cs.CRSCodeType;
import org.deegree.cs.CRSIdentifiable;
import org.deegree.cs.coordinatesystems.CoordinateSystem;
import org.deegree.cs.exceptions.CRSConfigurationException;
import org.deegree.cs.persistence.AbstractCRSStore;
import org.deegree.cs.transformations.Transformation;
import org.deegree.cs.transformations.TransformationFactory.DSTransform;

/**
 * TODO add class documentation here
 * 
 * This class have to be implemented - it's just an skeleton for the EPSGDBSynchronizer, yet!
 * 
 * @author <a href="mailto:buesching@lat-lon.de">Lyn Buesching</a>
 * @author last edited by: $Author: lyn $
 * 
 * @version $Revision: $, $Date: $
 */
public class DBCRSStore extends AbstractCRSStore<String> {

    public DBCRSStore( DSTransform prefTransformType ) {
        super( prefTransformType );
        // TODO Auto-generated constructor stub
    }

    @Override
    public void init() {
        // TODO Auto-generated method stub

    }

    @Override
    public Transformation getTransformation( CoordinateSystem sourceCRS, CoordinateSystem targetCRS )
                            throws CRSConfigurationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CRSIdentifiable getIdentifiable( CRSCodeType id )
                            throws CRSConfigurationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<CoordinateSystem> getAvailableCRSs()
                            throws CRSConfigurationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<CRSCodeType[]> getAvailableCRSCodes()
                            throws CRSConfigurationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected CoordinateSystem parseCoordinateSystem( String crsDefinition )
                            throws CRSConfigurationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Transformation parseTransformation( String transformationDefinition )
                            throws CRSConfigurationException {
        // TODO Auto-generated method stub
        return null;
    }

}
