package at.ac.ait.ubicity.ubicity.flickrplugin;

import java.util.Date;

import org.junit.Test;

import at.ac.ait.ubicity.ubicity.flickrplugin.dto.FlickrDTO;

public class FlickrTest {

	@Test
	public void JsonTest() {

		FlickrDTO dto = new FlickrDTO("123", "http://ddd.com", "title", 125f,
				15.22f, new Date());

		System.out.println(dto.toJson());
	}
}
