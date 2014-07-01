package at.ac.ait.ubicity.ubicity.flickrplugin;

import org.junit.Ignore;
import org.junit.Test;

import at.ac.ait.ubicity.commons.util.PropertyLoader;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.SearchParameters;

public class FlickrTest1 {

	@Test
	@Ignore
	public void flickrSSLTest() throws FlickrException {
		PropertyLoader config = new PropertyLoader(
				FlickrStreamer.class.getResource("/flicker.cfg"));

		REST rest = new REST();

		Flickr flickr = new Flickr(config.getString("plugin.flickr.api.key"),
				config.getString("plugin.flickr.api.secret"), rest);

		PhotosInterface photosInterface = flickr.getPhotosInterface();

		SearchParameters searchParams = new SearchParameters();
		searchParams.setSort(SearchParameters.INTERESTINGNESS_DESC);
		String[] tags = { "beer" };
		searchParams.setTags(tags);

		PhotoList<Photo> photoList = photosInterface
				.search(searchParams, 20, 1);

		System.out.println(photoList.getTotal());
	}
}
