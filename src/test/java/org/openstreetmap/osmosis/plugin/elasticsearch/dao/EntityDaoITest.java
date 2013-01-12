package org.openstreetmap.osmosis.plugin.elasticsearch.dao;

import java.io.IOException;

import junit.framework.Assert;

import org.elasticsearch.action.get.GetResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.plugin.elasticsearch.index.osm.OsmIndexBuilder;
import org.openstreetmap.osmosis.plugin.elasticsearch.service.IndexAdminService;
import org.openstreetmap.osmosis.plugin.elasticsearch.utils.AbstractElasticsearchInMemoryTest;
import org.openstreetmap.osmosis.plugin.elasticsearch.utils.OsmDataBuilder;

public class EntityDaoITest extends AbstractElasticsearchInMemoryTest {

	private static final String INDEX_NAME = "osm-test";

	private EntityDao entityDao;

	@Before
	public void setUp() throws IOException {
		IndexAdminService indexService = new IndexAdminService(client());
		indexService.createIndex(INDEX_NAME, new OsmIndexBuilder().getIndexMapping());
		entityDao = new EntityDao(INDEX_NAME, client());
	}

	@After
	public void tearDown() {
		delete();
		refresh();
	}

	@Test
	public void saveNode() {
		// Setup
		Node node = OsmDataBuilder.buildSampleNode();

		// Action
		entityDao.save(node);
		refresh(INDEX_NAME);

		// Assert
		GetResponse response = client().prepareGet(INDEX_NAME, "node", "1").execute().actionGet();
		Assert.assertTrue(response.exists());
		String expected = "{\"location\":[2.0,1.0],\"tags\":{\"highway\":\"traffic_signals\"}}";
		String actual = response.getSourceAsString();
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void saveWay() {
		// Setup
		Way way = OsmDataBuilder.buildSampleWay();

		// Action
		entityDao.save(way);
		refresh(INDEX_NAME);

		// Assert
		GetResponse response = client().prepareGet(INDEX_NAME, "way", "1").execute().actionGet();
		Assert.assertTrue(response.exists());
		String expected = "{\"tags\":{\"highway\":\"residential\"},\"nodes\":[1]}";
		String actual = response.getSourceAsString();
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void findNode() {
		// Setup
		Node node = OsmDataBuilder.buildSampleNode();
		entityDao.save(node);
		refresh(INDEX_NAME);

		// Action
		Node actual = entityDao.find(1l, Node.class);

		// Assert
		Assert.assertEquals(1l, actual.getId());
		Assert.assertEquals(1.0d, actual.getLatitude());
		Assert.assertEquals(2.0d, actual.getLongitude());
		Tag tag = actual.getTags().iterator().next();
		Assert.assertEquals("highway", tag.getKey());
		Assert.assertEquals("traffic_signals", tag.getValue());
	}

	@Test
	public void findNode_doesNotExists() {
		// Setup
		Way way = OsmDataBuilder.buildSampleWay();
		entityDao.save(way);
		refresh(INDEX_NAME);

		// Action
		Node actual = entityDao.find(2l, Node.class);

		// Assert
		Assert.assertNull(actual);
	}

	@Test
	public void findWay() {
		// Setup
		Way way = OsmDataBuilder.buildSampleWay();
		entityDao.save(way);
		refresh(INDEX_NAME);

		// Action
		Way actual = entityDao.find(1l, Way.class);

		// Assert
		Assert.assertEquals(1l, actual.getId());
		Tag tag = actual.getTags().iterator().next();
		Assert.assertEquals("highway", tag.getKey());
		Assert.assertEquals("residential", tag.getValue());
		WayNode wayNode = actual.getWayNodes().get(0);
		Assert.assertEquals(1l, wayNode.getNodeId());
	}

	@Test
	public void findWay_doesNotExists() {
		// Setup
		Way way = OsmDataBuilder.buildSampleWay();
		entityDao.save(way);
		refresh(INDEX_NAME);

		// Action
		Way actual = entityDao.find(2l, Way.class);

		// Assert
		Assert.assertNull(actual);
	}

}
