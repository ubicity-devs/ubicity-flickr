package at.ac.ait.ubicity.ubicity.flickrplugin.impl;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import at.ac.ait.ubicity.ubicity.flickrplugin.dto.FlickrDTO;

/**
 * Tests if the set of URLs is valid and removes invalid ones from the set.
 * 
 * @author jan van oort
 */
public class ImageGrokker {

	private static final Logger logger = Logger.getLogger(ImageGrokker.class);

	public synchronized List<FlickrDTO> removeDeadLinks(List<FlickrDTO> flickrList) {
		Collection<FlickrDTO> removable = new HashSet<FlickrDTO>();

		flickrList.stream().parallel().forEach((dto) -> {
			try {
				WeakReference<HttpURLConnection> conn = new WeakReference<HttpURLConnection>((HttpURLConnection) new URL(dto.getUrl()).openConnection());
				conn.get().setInstanceFollowRedirects(false);
				String redirect = conn.get().getHeaderField("Location");

				if (redirect != null) {
					removable.add(dto);
				}
				conn.clear();

			} catch (Exception ioex) {
				logger.error("Exc. caught while checking valid URLs.", ioex);
				removable.add(dto);
			}
		});
		flickrList.removeAll(removable);

		logger.info("Removed " + removable.size() + " entries - returning " + flickrList.size() + " entries");

		return flickrList;
	}
}
