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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import net.xeoh.plugins.base.annotations.events.Shutdown;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import at.ac.ait.ubicity.commons.broker.events.ESMetadata;
import at.ac.ait.ubicity.commons.broker.events.ESMetadata.Action;
import at.ac.ait.ubicity.commons.broker.events.ESMetadata.Properties;
import at.ac.ait.ubicity.commons.broker.events.EventEntry;
import at.ac.ait.ubicity.commons.broker.events.Metadata;
import at.ac.ait.ubicity.commons.protocol.Answer;
import at.ac.ait.ubicity.commons.protocol.Command;
import at.ac.ait.ubicity.commons.protocol.Control;
import at.ac.ait.ubicity.commons.protocol.Medium;
import at.ac.ait.ubicity.commons.protocol.Terms;
import at.ac.ait.ubicity.commons.util.PropertyLoader;
import at.ac.ait.ubicity.core.Core;
import at.ac.ait.ubicity.core.UbicityBrokerException;
import at.ac.ait.ubicity.ubicity.flickrplugin.FlickrStreamer;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.SearchParameters;

@PluginImplementation
public class FlickrStreamerImpl implements FlickrStreamer {

	private final Medium myMedium = Medium.FLICKR;

	private final Map<String, TermHandler> handlers = new ConcurrentHashMap<String, TermHandler>();

	private Core core;

	private String name;

	private String esIndex;

	private int uniqueId;

	volatile boolean shutdown = false;

	final static Logger logger = Logger.getLogger(FlickrStreamerImpl.class);

	@Override
	@Init
	public void init() {
		uniqueId = new Random().nextInt();

		PropertyLoader config = new PropertyLoader(
				FlickrStreamerImpl.class.getResource("/flicker.cfg"));
		setPluginConfig(config);

		core = Core.getInstance();
		core.register(this);

		Thread t = new Thread(this);
		t.setName("execution context for " + getName());
		t.start();
	}

	/**
	 * Sets the Plugin configuration.
	 * 
	 * @param config
	 */
	private void setPluginConfig(PropertyLoader config) {
		this.name = config.getString("plugin.flickr.name");
		esIndex = config.getString("plugin.flickr.elasticsearch.index");
	}

	@Override
	public final int hashCode() {
		return uniqueId;
	}

	@Override
	public final boolean equals(Object o) {

		if (FlickrStreamerImpl.class.isInstance(o)) {
			FlickrStreamerImpl other = (FlickrStreamerImpl) o;
			return other.uniqueId == this.uniqueId;
		}
		return false;
	}

	@Override
	public Answer execute(Command _command) {
		logger.info("[Received a Command :: " + _command.toRESTString());
		// do some sanity checking upon the command
		if (!_command.getMedia().get().contains(myMedium))
			return Answer.FAIL;
		if (null == _command.getTerms() || null == _command.getTerms().get())
			return Answer.ERROR;

		// deal with the case we have a control to execute, and get it out of
		// the way:
		if (!(null == _command.getControl()))
			return doControlOperation(_command) ? Answer.ACK : Answer.FAIL;

		// we have the right Medium in the command, and we have Terms: we can go
		// down to business
		Terms __terms = _command.getTerms();

		TermHandler tH = new TermHandler(this, __terms);
		handlers.put(__terms.getType(), tH);
		Thread tTH = new Thread(tH);
		tTH.setPriority(Thread.MAX_PRIORITY);
		tTH.start();
		logger.info("started TermHandler for type " + __terms.getType());
		return Answer.ACK;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void run() {
		while (!shutdown) {
			try {
				Thread.sleep(10);
				for (TermHandler t : handlers.values()) {
					if (t.done) {
						t.stop();
						handlers.remove(t.terms.getType());
						t = null;
					}
				}
			} catch (InterruptedException _interrupt) {
				Thread.interrupted();
			} catch (Exception | Error e) {
				logger.fatal("Caught an exception while running : "
						+ e.toString());
			}
		}
	}

	private boolean doControlOperation(Command _command) {
		// find the TermHandler we need to act upon
		StringBuilder sb = new StringBuilder();

		_command.getTerms().get().stream().forEach((t) -> {
			sb.append(t.getValue().toLowerCase());
		});

		TermHandler tH = handlers.get(sb.toString());
		if (null == tH)
			return false;
		if (_command.getControl().equals(Control.PAUSE))
			return tH.doPause();
		if (_command.getControl().equals(Control.STOP))
			return tH.doStop();

		// if we land here, something has gone pretty wrong
		logger.warn("could not determine which control to perform, or for which terms the control was meant. Here follows a representation of the Command received : "
				+ _command.toString());
		return false;
	}

	EventEntry createEvent(String esType, String data) {

		HashMap<Properties, String> props = new HashMap<ESMetadata.Properties, String>();
		props.put(Properties.ES_INDEX, this.esIndex);
		props.put(Properties.ES_TYPE, esType);
		Metadata meta = new ESMetadata(Action.INDEX, 1, props);

		String id = this.name + "-" + UUID.randomUUID().toString();
		return new EventEntry(id, meta, data);
	}

	@Override
	@Shutdown
	public void shutdown() {
		shutdown = true;
	}
}

final class TermHandler extends Thread {

	final Terms terms;
	boolean done = false;

	private Flickr flickrClient = null;
	private final Core core;

	private final FlickrStreamerImpl grapper;

	final static Logger logger = Logger.getLogger(TermHandler.class);

	TermHandler(FlickrStreamerImpl grapper, Terms _terms) {
		this.grapper = grapper;
		this.terms = _terms;
		this.core = Core.getInstance();

		PropertyLoader config = new PropertyLoader(
				TermHandler.class.getResource("/flicker.cfg"));
		flickrClient = new Flickr(config.getString("plugin.flickr.api.key"),
				config.getString("plugin.flickr.api.secret"), new REST());
		Flickr.debugStream = false;
	}

	@Override
	public final void run() {
		FlickrStreamerImpl.logger.info("TermHander for " + terms.getType()
				+ " :: RUN ");

		// initialize SearchParameter object, which stores the search keyword(s)
		SearchParameters searchParams = new SearchParameters();
		searchParams.setSort(SearchParameters.INTERESTINGNESS_DESC);

		// create tag keyword array
		int termCounter = 0;

		String[] tags = new String[terms.termList.size()];

		terms.termList.stream().forEach((_t) -> {
			tags[termCounter] = _t.getValue();
		});

		searchParams.setTags(tags);

		// initialize PhotosInterface object
		PhotosInterface photosInterface = flickrClient.getPhotosInterface();
		// execute search with given tags

		long _start = System.currentTimeMillis();
		final Set<URL> __urls = new HashSet();
		try {

			PhotoList<Photo> photoList = photosInterface.search(searchParams,
					20, 1);
			logger.info("Fetched " + photoList.size() + " Flickr results in "
					+ (System.currentTimeMillis() - _start) + " [ms]");

			photoList
					.stream()
					.parallel()
					.forEach(
							(p) -> {
								try {
									URL __url = new URL(p.getLargeUrl());
									__urls.add(__url);

								} catch (MalformedURLException _badURL) {
									logger.warn("[FLICKR] * * * bad URL: "
											+ _badURL.toString());
								}
							});
			int rawSize = __urls.size();
			grok(__urls);

			logger.info("Grokker removed " + rawSize + " entries. Indexing "
					+ __urls.size() + " entries.");
			index(__urls);

		} catch (FlickrException fe) {
			done = true;
			logger.error("Caught flickrException", fe);
			throw new Error(fe);
		}
		done = true;
		return;
	}

	public final boolean doStop() {
		try {
			stop();
			return true;
		} catch (Exception | Error e) {
			logger.warn("problem encountered while trying to stop ");
			return false;
		}
	}

	public final boolean doPause() {
		try {
			this.suspend();
			return true;
		} catch (Exception | Error e) {
			logger.warn("problem encountered while trying to pause ");
			return false;
		}
	}

	private void index(final Set<URL> urlList) {

		for (URL u : urlList) {
			Map<String, String> json = new HashMap();
			json.put("url", u.toString());

			// Publish event
			EventEntry entry = this.grapper.createEvent(terms.getType(),
					new JSONObject(json).toString());
			try {
				core.publish(entry);
			} catch (UbicityBrokerException e) {
				logger.error("UbicityBroker threw exc: " + e.getBrokerMessage());
			}
		}
	}

	private Set<URL> grok(Set<URL> __urls) {
		return (new ImageGrokker(__urls)).run();
	}
}
