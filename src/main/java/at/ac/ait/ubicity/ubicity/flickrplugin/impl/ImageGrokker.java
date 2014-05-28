package at.ac.ait.ubicity.ubicity.flickrplugin.impl;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Tests if the set of URLs is valid and removes invalid ones from the set.
 * 
 * @author jan van oort
 */
public final class ImageGrokker {

	private static final Logger logger = Logger.getLogger(ImageGrokker.class);

	private final Set<URL> urls;

	public ImageGrokker(Set<URL> _urls) {
		urls = _urls;
	}

	public Set<URL> run() {
		Collection<URL> removable = new HashSet<URL>();

		urls.stream()
				.parallel()
				.forEach(
						(_url) -> {
							try {
								WeakReference<HttpURLConnection> conn = new WeakReference<HttpURLConnection>(
										(HttpURLConnection) _url
												.openConnection());
								conn.get().setInstanceFollowRedirects(false);
								String redirect = conn.get().getHeaderField(
										"Location");

								if (redirect != null) {
									removable.add(_url);
								}
								conn.clear();

							} catch (IOException ioex) {
								logger.error(
										"Exc. caught while checking valid URLs.",
										ioex);
							}
						});
		urls.removeAll(removable);

		logger.info("Removed " + removable.size() + " entries - returning "
				+ urls.size() + " entries");
		return urls;
	}
}
