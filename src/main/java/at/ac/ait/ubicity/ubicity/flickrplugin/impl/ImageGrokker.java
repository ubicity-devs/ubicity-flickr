
package at.ac.ait.ubicity.ubicity.flickrplugin.impl;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author jan van oort
 */
public final class ImageGrokker  {

    
    private   Set< URL > urls;
    
    public ImageGrokker( Set< URL > _urls ) {
        urls = _urls;
    }
    
    
    
    
    public  Set< URL > run() {
        Collection< URL > removable = new HashSet();
        urls.stream().parallel().forEach((_url) -> {
            try {
                WeakReference<HttpURLConnection> conn =  new WeakReference( ( HttpURLConnection ) _url.openConnection() );
                conn.get().setInstanceFollowRedirects( false );
                String redirect = conn.get().getHeaderField( "Location" );
                
                if( ! ( redirect == null ) ) {
                    System.out.println( "[GROK] redirect String: " + redirect );
                    removable.add( _url );
                    System.out.println( "[GROK] removed " + _url.toString() );
                }
                conn.clear();
                
            }
            catch( IOException ioex )   {
                ioex.printStackTrace();
            }
        });
        urls.removeAll( removable );
        return urls;
    }
}
