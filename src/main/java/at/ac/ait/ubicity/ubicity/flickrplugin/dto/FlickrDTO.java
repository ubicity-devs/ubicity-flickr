package at.ac.ait.ubicity.ubicity.flickrplugin.dto;

import java.util.Date;

import at.ac.ait.ubicity.commons.templates.AbstractDTO;

import com.google.gson.annotations.SerializedName;

public class FlickrDTO extends AbstractDTO {

	private final String id;
	private final String url;
	private final String title;

	@SerializedName("geo_point")
	private float[] geoPoint;

	@SerializedName("created_at")
	private final String createdAt;

	public FlickrDTO(String id, String url, String title, float longitude, float latitude, Date createdAt) {
		this(id, url, title, createdAt);

		this.geoPoint = new float[] { longitude, latitude };
	}

	public FlickrDTO(String id, String url, String title, Date createdAt) {
		this.id = id;
		this.url = url;
		this.title = title;
		this.createdAt = dateAsString(createdAt);
	}

	public String getId() {
		return this.id;
	}

	public String getUrl() {
		return this.url;
	}

	public String getTitle() {
		return this.title;
	}

	public float[] getGeoPoint() {
		return this.geoPoint;
	}

	public String getCreatedAt() {
		return this.createdAt;
	}
}
