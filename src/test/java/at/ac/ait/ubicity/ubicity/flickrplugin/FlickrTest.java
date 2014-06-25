package at.ac.ait.ubicity.ubicity.flickrplugin;

import java.util.Date;

import org.junit.Ignore;
import org.junit.Test;

import at.ac.ait.ubicity.contracts.flickr.FlickrDTO;

public class FlickrTest {

	@Ignore
	@Test
	public void JsonTest() {

		FlickrDTO dto = new FlickrDTO("123", "http://ddd.com", "title", 125f,
				15.22f, new Date());

		System.out.println(dto.toJson());
	}
}
