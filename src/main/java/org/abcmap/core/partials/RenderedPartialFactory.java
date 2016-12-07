package org.abcmap.core.partials;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 * Store and create partials
 * <p>
 * Partials are fixed size squares. Same degree value is used for partial height and width (e.g: 1 dg lat/lon) and this should cause issues but Geotools
 * renderer is supposed to compensate that (no deformation should appear)
 * <p>
 * Need more tests at several position
 * <p>
 */
public class RenderedPartialFactory {

    /**
     * Minimal size in world unit of rendered map on partial
     *
     * This value should prevent partial side to be negative
     */
    private static final double MIN_PARTIAL_SIDE_WU = 0.05d;

    private static long loadedPartialsReused = 0;

    /**
     * Associated map content
     */
    private MapContent mapContent;

    /**
     * Where are stored partials
     */
    private final RenderedPartialStore store;

    /**
     * Zoom level of current rendering
     */
    private double partialSideWu = 2d;

    /**
     * Default size in px of each partial
     */
    private int partialSidePx = 500;

    public RenderedPartialFactory(RenderedPartialStore store, MapContent content) {
        this.store = store;
        this.mapContent = content;
    }

    /**
     * Get partials from Upper Left Corner (world) position with specified dimension
     *
     * @param ulc
     * @param pixelDimension
     * @return
     */
    public RenderedPartialQueryResult intersect(Point2D ulc, Dimension pixelDimension, CoordinateReferenceSystem crs, Runnable toNotifyWhenPartialsCome) {

        // get width and height in decimal dg
        double wdg = partialSideWu * pixelDimension.width / partialSidePx;
        double hdg = partialSideWu * pixelDimension.height / partialSidePx;

        // create a new envelope
        double x1 = ulc.getX();
        double y1 = ulc.getY() - hdg; // to BLC
        double x2 = ulc.getX() + wdg;
        double y2 = ulc.getY();

        // create a new envelope
        return intersect(new ReferencedEnvelope(x1, x2, y1, y2, crs), toNotifyWhenPartialsCome);

    }

    /**
     * Get partials around a world envelope
     *
     * @param worldBounds
     * @return
     */
    public RenderedPartialQueryResult intersect(ReferencedEnvelope worldBounds, Runnable toNotifyWhenPartialsCome) {

        if (mapContent == null) {
            throw new NullPointerException("Noting to render, map content is null");
        }

        // keep the same value until end of rendering process, even if value is changed by setter
        double partialSideDg = this.partialSideWu;

        // Side value in decimal degree of each partial
        if (partialSideDg < MIN_PARTIAL_SIDE_WU) {
            partialSideDg = MIN_PARTIAL_SIDE_WU;
        }

        ArrayList<RenderedPartial> rsparts = new ArrayList<>();

        // count partials
        int tileNumberW = 0;
        int tileNumberH = 0;

        // first position to go from
        // position is rounded in order to have partials that can be reused in future display
        double x = getStartPointFrom(worldBounds.getMinX());
        double y = getStartPointFrom(worldBounds.getMinY());

        PartialRenderingQueue pr = null;

        // iterate area to render from bottom left corner to upper right corner
        while (y < worldBounds.getMaxY()) {

            // count horizontal partials only on the first line
            if (tileNumberH == 0) {
                tileNumberW++;
            }

            // compute needed area for next partial
            ReferencedEnvelope area = new ReferencedEnvelope(x, round(x + partialSideDg), y, round(y + partialSideDg), DefaultGeographicCRS.WGS84);

            // check if partial already exist and is already loaded
            RenderedPartial part = store.searchInLoadedList(area);
            if (part != null && part.getImage() != null) {
                rsparts.add(part);
                loadedPartialsReused++;
            }

            // partial does not exist or image is not loaded, create it
            else {

                // partial processing has already been scheduled
                if (PartialRenderingQueue.isRenderInProgress(area) && part != null) {
                    rsparts.add(part);
                }

                // partial processing have to be scheduled
                else {
                    // create a new partial
                    RenderedPartial newPart = new RenderedPartial(null, area, partialSidePx, partialSidePx);
                    store.addInLoadedList(newPart);
                    rsparts.add(newPart);

                    // Create a queue if needed. In most case, it is not needed.
                    if (pr == null) {
                        pr = new PartialRenderingQueue(mapContent, store, partialSidePx, partialSidePx, toNotifyWhenPartialsCome);
                    }

                    // create a task to retrieve or render image from map
                    pr.addTask(newPart);
                }

            }

            // go to next
            x += partialSideDg;

            // change line when finished
            if (x > worldBounds.getMaxX()) {
                y += partialSideDg;
                tileNumberH++;

                // reset x except the last loop
                if (y < worldBounds.getMaxY()) {
                    x = getStartPointFrom(worldBounds.getMinX());
                }
            }

        }

        // launch tasks to retrieve or produce partial in a separated thread, if needed
        if (pr != null) {
            pr.start();
        }

        // if not enough tiles, return null to avoid errors on transformations
        if (rsparts.size() < 1) {
            return null;
        }

        double w = worldBounds.getWidth();
        double h = worldBounds.getHeight();

        // compute real screen bounds of asked world area
        // given that we used fixed size partials, area can be larger than asked one
        Rectangle screenBounds = new Rectangle(0, 0,
                (int) Math.round(w * partialSidePx / partialSideDg),
                (int) Math.round(h * partialSidePx / partialSideDg));

        return new RenderedPartialQueryResult(rsparts, worldBounds, screenBounds, tileNumberW, tileNumberH);
    }

    /**
     * Get the closest start point of specified coordinate.
     * <p>
     * Coordinates are normalized in order to have reusable partials
     *
     * @param coord
     * @return
     */
    public double getStartPointFrom(double coord) {

        double mod = coord % partialSideWu;
        if (mod < 0) {
            mod += partialSideWu;
        }

        double rslt = coord - mod;

        return round(rslt);
    }

    /**
     * Round values to 6 decimal, in order to normalize coordinates and have reusable partials
     *
     * @param coord
     * @return
     */
    public double round(double coord) {
        return Math.round(coord * 1000000.0) / 1000000.0;
    }

    /**
     * Set rendered partial size in world unit
     * <p>
     * Partial size can be used as a "zoom" value
     *
     * @param
     */
    public void setPartialSideWu(double sideDg) {

        this.partialSideWu = sideDg;

        if (partialSideWu < MIN_PARTIAL_SIDE_WU) {
            partialSideWu = MIN_PARTIAL_SIDE_WU;
        }

    }

    public int getPartialSidePx() {
        return partialSidePx;
    }

    /**
     * Get rendered partial size in world unit
     * <p>
     * Partial size can be used as a "zoom" value
     *
     * @param
     */
    public double getPartialSideWu() {
        return partialSideWu;
    }

    public static long getLoadedPartialsReused() {
        return loadedPartialsReused;
    }

    public RenderedPartialStore getStore() {
        return store;
    }
}
