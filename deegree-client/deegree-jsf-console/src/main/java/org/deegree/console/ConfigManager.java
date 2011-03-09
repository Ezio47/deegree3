//$HeadURL$
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
package org.deegree.console;

import static javax.faces.application.FacesMessage.SEVERITY_ERROR;
import static org.deegree.services.controller.OGCFrontController.getServiceWorkspace;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import lombok.Getter;
import lombok.Setter;

import org.deegree.commons.config.ResourceManager;
import org.deegree.commons.config.ResourceProvider;
import org.deegree.commons.config.ResourceState;
import org.slf4j.Logger;

/**
 * TODO add class documentation here
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
@ManagedBean
@SessionScoped
public class ConfigManager {

    private static final Logger LOG = getLogger( ConfigManager.class );

    @Getter
    private ResourceManagerMetadata2 currentResourceManager;

    @Getter
    private String newConfigType;

    @Getter
    private List<String> newConfigTypeTemplates;

    @Getter
    @Setter
    private String newConfigTypeTemplate;

    @Getter
    @Setter
    private String newConfigId;

    // @Getter
    // private Config proxyConfig;

    @Getter
    private Config metadataConfig;

    @Getter
    private Config mainConfig;

    private boolean modified;

    public List<ResourceManagerMetadata2> getResourceManagers() {
        List<ResourceManagerMetadata2> rmMetadata = new ArrayList<ResourceManagerMetadata2>();
        for ( ResourceManager mgr : getServiceWorkspace().getResourceManagers() ) {
            ResourceManagerMetadata2 md = ResourceManagerMetadata2.getMetadata( mgr );
            if ( md != null ) {
                rmMetadata.add( md );
            }
        }
        Collections.sort( rmMetadata );
        return rmMetadata;
    }

    public void resourceManagerChanged( ActionEvent evt ) {
        String linkText = ( (HtmlCommandLink) evt.getSource() ).getValue().toString();
        for ( ResourceManagerMetadata2 mgr : getResourceManagers() ) {
            if ( mgr.getName().equals( linkText ) ) {
                currentResourceManager = mgr;
            }
        }
    }

    public List<Config> getAvailableResources() {
        List<Config> configs = new ArrayList<Config>();
        for ( ResourceState state : currentResourceManager.getManager().getStates() ) {
            configs.add( new Config( state, this, currentResourceManager.getManager(),
                                     currentResourceManager.getStartView() ) );
        }
        return configs;
    }

    public void setNewConfigType( String newConfigType ) {
        this.newConfigType = newConfigType;
        for ( ResourceProvider p : currentResourceManager.getProviders() ) {
            if ( p.getConfigNamespace().endsWith( newConfigType ) ) {
                ResourceProviderMetadata md = ResourceProviderMetadata.getMetadata( p );
                newConfigTypeTemplates = new LinkedList<String>( md.getExamples().keySet() );
            }
        }
    }

    public String startWizard() {

        String nextView = "/console/jsf/wizard";

        ResourceProvider rp = null;
        for ( ResourceProvider p : currentResourceManager.getProviders() ) {
            if ( p.getConfigNamespace().endsWith( newConfigType ) ) {
                rp = p;
            }
        }
        if ( rp != null ) {
            ResourceProviderMetadata md = ResourceProviderMetadata.getMetadata( rp );
            nextView = md.getConfigWizardView();
        }
        return nextView;
    }

    public String createConfig() {

        ResourceManager manager = currentResourceManager.getManager();

        // lookup template
        URL templateURL = null;
        for ( ResourceProvider p : currentResourceManager.getProviders() ) {
            if ( p.getConfigNamespace().endsWith( newConfigType ) ) {
                if ( newConfigTypeTemplate != null ) {
                    ResourceProviderMetadata md = ResourceProviderMetadata.getMetadata( p );
                    templateURL = md.getExamples().get( newConfigTypeTemplate ).getContentLocation();
                    LOG.info( "Found template URL: " + templateURL );
                }
            }
        }

        // let the resource manager do the dirty work
        ResourceState rs = null;
        try {
            rs = manager.createResource( newConfigId, templateURL.openStream() );
            Config c = new Config( rs, this, currentResourceManager.getManager(), currentResourceManager.getStartView() );
            return c.edit();
        } catch ( Throwable t ) {
            FacesMessage fm = new FacesMessage( SEVERITY_ERROR, "Unable to create config: " + t.getMessage(), null );
            FacesContext.getCurrentInstance().addMessage( null, fm );
            return null;
        }
    }
}