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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import net.xeoh.plugins.base.annotations.events.Shutdown;

import org.apache.log4j.Logger;

import at.ac.ait.ubicity.commons.broker.BrokerProducer;
import at.ac.ait.ubicity.commons.broker.events.EventEntry;
import at.ac.ait.ubicity.commons.broker.events.EventEntry.Property;
import at.ac.ait.ubicity.commons.exceptions.UbicityBrokerException;
import at.ac.ait.ubicity.commons.jit.Action;
import at.ac.ait.ubicity.commons.jit.Answer;
import at.ac.ait.ubicity.commons.jit.Answer.Status;
import at.ac.ait.ubicity.commons.util.ESIndexCreator;
import at.ac.ait.ubicity.commons.util.PropertyLoader;
import at.ac.ait.ubicity.ubicity.flickrplugin.FlickrStreamer;
import at.ac.ait.ubicity.ubicity.flickrplugin.dto.FlickrDTO;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.SearchParameters;

@PluginImplementation
public class FlickrStreamerImpl extends BrokerProducer implements FlickrStreamer {

	private static final Logger logger = Logger.getLogger(FlickrStreamerImpl.class);

	private String name;
	private ESIndexCreator ic;
	private String pluginDest;

	private PhotosInterface flickrPicClient;
	private final ImageGrokker grokker = new ImageGrokker();

	@Override
	@Init
	public void init() {
		PropertyLoader config = new PropertyLoader(FlickrStreamerImpl.class.getResource("/flickr.cfg"));
		setProducerSettings(config);
		setPluginConfig(config);

		Flickr flickrClient = new Flickr(config.getString("plugin.flickr.api.key"), config.getString("plugin.flickr.api.secret"), new REST());
		flickrPicClient = flickrClient.getPhotosInterface();

		logger.info(name + " loaded");
	}

	/**
	 * Sets the Apollo broker settings
	 * 
	 * @param config
	 */
	private void setProducerSettings(PropertyLoader config) {
		try {
			super.init();
			pluginDest = config.getString("plugin.flickr.broker.dest");

		} catch (UbicityBrokerException e) {
			logger.error("During init caught exc.", e);
		}
	}

	/**
	 * Sets the Plugin configuration.
	 * 
	 * @param config
	 */
	private void setPluginConfig(PropertyLoader config) {
		this.name = config.getString("plugin.flickr.name");
		ic = new ESIndexCreator(config.getString("plugin.flickr.elasticsearch.index"), "", config.getString("plugin.flickr.elasticsearch.pattern"));

	}

	@Override
	public Answer process(Action action) {
		String data = action.getData().toLowerCase();

		logger.info("Search tags: " + data);
		return new Answer(action, Status.PROCESSED, searchPhotos(data.split(" ")));
	}

	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * Creates a new EventEntry object.
	 * 
	 * @param esType
	 * @param data
	 * @return
	 */
	private EventEntry createEvent(String esType, FlickrDTO data) {
		HashMap<Property, String> header = new HashMap<Property, String>();
		header.put(Property.ES_INDEX, ic.getIndex());
		header.put(Property.ES_TYPE, esType);
		header.put(Property.ID, this.name + "-" + data.getId());
		header.put(Property.PLUGIN_CHAIN, EventEntry.formatPluginChain(Arrays.asList(pluginDest)));

		return new EventEntry(header, data.toJson());
	}

	@Override
	@Shutdown
	public void shutdown() {
		shutdown(pluginDest);
	}

	/**
	 * Process the search and index functionality.
	 * 
	 * @param tags
	 */
	private String searchPhotos(String[] tags) {

		SearchParameters searchParams = new SearchParameters();
		searchParams.setSort(SearchParameters.INTERESTINGNESS_DESC);

		searchParams.setTags(tags);
		final CopyOnWriteArrayList<FlickrDTO> flickrList = new CopyOnWriteArrayList<FlickrDTO>();

		String status = "";

		try {
			PhotoList<Photo> photoList = flickrPicClient.search(searchParams, 0, 0);

			photoList.parallelStream().forEach(
					(p) -> {
						if (p.hasGeoData() && p.getGeoData() != null) {
							flickrList.add(new FlickrDTO(p.getId(), p.getLargeUrl(), p.getTitle(), p.getGeoData().getLongitude(), p.getGeoData().getLatitude(),
									p.getDateAdded()));
						} else {
							flickrList.add(new FlickrDTO(p.getId(), p.getLargeUrl(), p.getTitle(), p.getDateAdded()));
						}
					});

			List<FlickrDTO> grokkedList = grokker.removeDeadLinks(flickrList);

			grokkedList.forEach((dto) -> {
				try {
					publish(createEvent(buildEsType(tags), dto));
				} catch (UbicityBrokerException e) {
					logger.error("UbicityBroker threw exc: " + e.getBrokerMessage());
				}

			});

			status = "Indexed " + grokkedList.size() + " flickr images.";

		} catch (FlickrException e1) {
			logger.error("Fetching photos threw exc: " + e1);
		}

		return status;
	}

	/**
	 * Builds the ES Type based on the tags.
	 * 
	 * @param tags
	 * @return
	 */
	private String buildEsType(String[] tags) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < tags.length; i++) {
			sb.append(tags[i]);
		}

		return sb.toString().toLowerCase();
	}
}