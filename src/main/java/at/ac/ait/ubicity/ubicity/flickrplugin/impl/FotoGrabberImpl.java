/**
    Copyright (C) 2014  AIT / Austrian Institute of Technology
    http://www.ait.ac.at

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see http://www.gnu.org/licenses/agpl-3.0.html
 */
package at.ac.ait.ubicity.ubicity.flickrplugin.impl;

import at.ac.ait.ubicity.commons.PluginContext;
import at.ac.ait.ubicity.commons.protocol.Answer;
import at.ac.ait.ubicity.commons.protocol.Command;
import at.ac.ait.ubicity.commons.protocol.Control;
import at.ac.ait.ubicity.commons.protocol.Medium;
import at.ac.ait.ubicity.commons.protocol.Term;
import at.ac.ait.ubicity.commons.protocol.Terms;
import at.ac.ait.ubicity.ubicity.flickrplugin.FotoGrabber;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jan van oort
 */
public class FotoGrabberImpl implements FotoGrabber {

    
    private Medium myMedium;
    
    private Map< String, TermHandler > handlers = new HashMap();
    
    
    final static Logger logger = Logger.getLogger( FotoGrabberImpl.class.getName() );
    
    static  {
        logger.setLevel( Level.ALL );
    }
    
    
    @Override
    public Answer execute(Command _command) {
        //do some sanity checking upon the command
        if( ! _command.getMedia().get().contains( myMedium ) ) return Answer.FAIL;
        if( null == _command.getTerms() || null == _command.getTerms().get() ) return Answer.ERROR;
        
        //deal with the case we have a control to execute, and get it out of the way:
        if( ! ( null == _command.getControl() ) ) return doControlOperation( _command ) ? Answer.ACK : Answer.FAIL;
        
        //we have the right Medium in the command, and we have Terms: we can go down to business
        Terms __terms = _command.getTerms();
        TermHandler tH = new TermHandler( __terms );
        handlers.put( __terms.getType(), tH );
        logger.info( "started TermHandler for type " + __terms.getType() );
        return Answer.ACK;
    }

    /**
     *
     */
    @Override
    public void mustStop() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @return
     */
    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setContext(PluginContext context) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PluginContext getContext() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    private boolean doControlOperation( Command _command ) {
        //find the TermHandler we need to act upon
        StringBuilder sb = new StringBuilder();
        
        _command.getTerms().get().stream().forEach((t) -> {
            sb.append( t.getValue().toLowerCase() );
        });
        
        TermHandler tH = handlers.get( sb.toString() );
        if( null == tH ) return false;
        if( _command.getControl().equals( Control.PAUSE ) ) return tH.doPause();
        if( _command.getControl().equals( Control.STOP ) ) return tH.doStop();
        
        // if we land here, something has gone pretty wrong
        Logger.getAnonymousLogger( this.getClass().getName() ).warning( "could not determine which control to perform, or for which terms the control was meant. Here follows a representation of the Command received : " + _command.toString() );
        return false;
    }
    
}



final class TermHandler extends Thread  {

    
    private final Terms terms;
    
    
    TermHandler( Terms _terms )     {
        terms = _terms;
    }

    
    
    public final void run() {
        FotoGrabberImpl.logger.info( "TermHander for " + terms.getType() + " :: RUN " );
    }
    
    
    public final boolean doStop() {
        try {   
            stop();
            return true;
        }
        catch( Exception  | Error e )   {
            Logger.getLogger( this.getClass().getName() ).warning( "problem encountered while trying to stop " );
            return false;
        }
    }
    
    
    
    
   public final boolean doPause()    {
        try {
            this.suspend();
            return true;
        }
        catch( Exception | Error e )    {
            Logger.getLogger( this.getClass().getName() ).warning( "problem encountered while trying to pause " );
            return false;
        }
    }
}
