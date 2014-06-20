package at.ac.ait.ubicity.ubicity.flickrplugin.dto;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class FlickrDTO {

	private static Gson gson = new Gson();

	private static SimpleDateFormat df = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ssZZ");
	{
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private final String id;
	private final String url;
	private final String title;

	@SerializedName("geo_point")
	private float[] geoPoint;

	@SerializedName("created_at")
	private String createdAt;

	public FlickrDTO(String id, String url, String title, float longitude,
			float latitude, Date createdAt) {
		this(id, url, title, createdAt);

		this.geoPoint = new float[] { longitude, latitude };
	}

	public FlickrDTO(String id, String url, String title, Date createdAt) {
		this.id = id;
		this.url = url;
		this.title = title;
		if (createdAt != null) {
			this.createdAt = df.format(createdAt);
		}
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

	public String toJson() {
		return gson.toJson(this);
	}
}
