package net.joekit;

import junit.framework.TestCase;

/**
 * Tests parsing in the GeoPoint class.
 *
 * @author Pablo Mayrgundter
 */
public class GeoPointTest extends TestCase {

  GeoPoint gp = null;

  @Override
  public void setUp() {
  }

  @Override
  public void tearDown() {
  }

  /* TODO the coordinate parsing now happens in a separate class called 
   * LatLongCoordParser. */
  public void testParseCoord() {
    //assertEquals(100.3, GeoPoint.parseCoord("100.3"));
    //assertEquals(100.3, GeoPoint.parseCoord("100.3E"));
    //assertEquals(-100.3, GeoPoint.parseCoord("100.3W"));
  }

  public static void main(final String [] args) {
    junit.textui.TestRunner.run(GeoPointTest.class);
  }
}