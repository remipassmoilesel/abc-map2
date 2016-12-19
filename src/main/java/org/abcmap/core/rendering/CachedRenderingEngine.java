package org.abcmap.core.rendering;

import org.abcmap.core.events.manager.EventNotificationManager;
import org.abcmap.core.events.manager.HasEventNotificationManager;
import org.abcmap.core.log.CustomLogger;
import org.abcmap.core.managers.LogManager;
import org.abcmap.core.project.Project;
import org.abcmap.core.project.layers.AbstractLayer;
import org.abcmap.core.rendering.partials.RenderedPartial;
import org.abcmap.core.rendering.partials.RenderedPartialFactory;
import org.abcmap.core.rendering.partials.RenderedPartialQueryResult;
import org.abcmap.core.utils.GeoUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContent;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Render a map by cut it in several partials. When partials are rendered,
 * they are stored in database in order to avoid resource consumption.
 */
public class CachedRenderingEngine implements HasEventNotificationManager {

    private static final CustomLogger logger = LogManager.getLogger(CachedRenderingEngine.class);

    /**
     * Minimal size in world unit of rendered map on partial
     * <p>
     * This value should prevent partial side to be negative
     */
    public static final double MIN_PARTIAL_SIDE_WU = 0.1d;

    /**
     * Default size in pixel of each partial
     */
    public static final double DEFAULT_PARTIAL_SIDE_PX = 500d;

    /**
     * List of map content associated with layers
     */
    private final HashMap<String, MapContent> layerMapContents;

    /**
     * List of factories employed to renderer map. Each factory renderer one layer
     */
    private final HashMap<String, RenderedPartialFactory> partialFactories;

    /**
     * If set to true, scale will be limited
     */
    private boolean scaleLimited;

    /**
     * Current set of partials that have to be painted
     */
    private HashMap<String, RenderedPartialQueryResult> currentPartials;

    /**
     * Last world envelope (positions) of map rendered on panel
     */
    private ReferencedEnvelope worldEnvelope;

    /**
     * Minimum interval between rendering in ms
     */
    private long renderMinIntervalMs = 50;

    /**
     * Current side of a partial
     */
    private final double partialSidePx;

    /**
     * Last time of rendering in ms
     */
    private long lastRender = -1;

    /**
     * Current value of rendered map in partials. In world unit.
     */
    private double partialSideWu;

    /**
     * Minimum size of map rendered on a partial ("zoom" value)
     */
    private double minimumPartialSideWu;

    /**
     * Maximum size of map rendered on a partial ("zoom" value)
     */
    private double maximumPartialSideWu;

    /**
     * Lock to prevent too much thread rendering
     */
    private final ReentrantLock renderLock;


    /**
     * Project associated with this panel
     */
    private final Project project;

    /**
     * If set to true, additional information will be displayed on map
     */
    private boolean debugMode = false;

    /**
     * Rendered surface size
     */
    private Dimension renderedSizePx;

    private final EventNotificationManager notifm;

    public CachedRenderingEngine(Project p) {
        this.project = p;
        this.renderLock = new ReentrantLock();
        this.partialFactories = new HashMap<>();
        this.layerMapContents = new HashMap<>();
        this.currentPartials = new HashMap<>();

        // default partial size in pixel
        this.partialSidePx = DEFAULT_PARTIAL_SIDE_PX;

        // limit scale
        this.scaleLimited = true;

        // default world envelope
        this.worldEnvelope = project.getMaximumBounds();

        // // first display, use default values
        this.renderedSizePx = new Dimension(1000, 1000);
        computeMinAndMaxPartialSideWu();
        setPartialSideWu(maximumPartialSideWu);

        // listen partial store changes
        this.notifm = new EventNotificationManager(this);
        project.getRenderedPartialsStore().getNotificationManager().addObserver(this);

    }

    /**
     * Compute minimum and maximum limit of scale
     * <p>
     * Prevent display errors
     */
    private void computeMinAndMaxPartialSideWu() {
        ReferencedEnvelope world = project.getMaximumBounds();
        this.minimumPartialSideWu = (world.getMaxX() - world.getMinX()) / 10;
        this.maximumPartialSideWu = (world.getMaxX() - world.getMinX());
    }

    @Override
    protected void finalize() throws Throwable {
        // remove observer on finalizing
        project.getRenderedPartialsStore().getNotificationManager().removeObserver(this);
    }

    public void paint(Graphics2D g2d) {

        if (renderLock.isLocked()) {
            logger.debug("Render is in progress, abort painting");
            return;
        }

        System.out.println();

        for (AbstractLayer lay : project.getLayersList()) {

            RenderedPartialQueryResult partials = currentPartials.get(lay.getId());

            // list of layer changed before refreshMap called
            if (partials == null) {
                continue;
            }

            // get affine transform to set position of partials
            AffineTransform worldToScreen = partials.getWorldToScreenTransform();

            if (debugMode) {
                g2d.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
            }

            // iterate current partials
            for (RenderedPartial part : partials.getPartials()) {

                // compute position of tile on map
                ReferencedEnvelope ev = part.getEnvelope();
                Point2D.Double worldPos = new Point2D.Double(ev.getMinX(), ev.getMaxY());
                Point2D screenPos = worldToScreen.transform(worldPos, null);

                int x = (int) Math.round(screenPos.getX());
                int y = (int) Math.round(screenPos.getY());
                int w = part.getRenderedWidth();
                int h = part.getRenderedHeight();

                // draw partial
                g2d.drawImage(part.getImage(), x, y, w, h, null);

                if (debugMode) {

                    g2d.setColor(Color.darkGray);
                    g2d.drawRect(x, y, w, h);

                    // show index on partial
                    g2d.setColor(Color.BLACK);
                    String index = "#" + partials.getPartials().indexOf(part);
                    g2d.drawString(index, x + w / 2, y + h / 2);

                }

            }

            // draw maximums bounds asked if necessary
            if (debugMode) {
                Point2D.Double ulc = new Point2D.Double(worldEnvelope.getMinX(), worldEnvelope.getMaxY());
                Point2D wp = worldToScreen.transform(ulc, null);
                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(Color.red);
                g2d.drawRect((int) wp.getX(), (int) wp.getY(), 3, 3);
            }
        }
    }

    public void prepareMap(ReferencedEnvelope worldEnvelope, Dimension pixelDim) throws RenderingException {

        /*
        System.out.println();
        System.out.println("worldEnvelope");
        System.out.println(worldEnvelope);
        System.out.println("pixelDim");
        System.out.println(pixelDim);
        System.out.println("partialSideWu");
        System.out.println(partialSideWu);
        */

        if (worldEnvelope == null || pixelDim == null) {
            throw new NullPointerException("Invalid parameter: " + worldEnvelope + " / " + pixelDim);
        }

        if (worldEnvelope.getMaxX() - worldEnvelope.getMinX() < 0) {
            throw new RenderingException("Invalid envelope: " + worldEnvelope);
        }

        if (worldEnvelope.getMaxY() - worldEnvelope.getMinY() < 0) {
            throw new RenderingException("Invalid envelope: " + worldEnvelope);
        }

        if (pixelDim.width < 0 || pixelDim.height < 0) {
            throw new RenderingException("Invalid dimensions: " + pixelDim);
        }

        if (worldEnvelope.getCoordinateReferenceSystem().equals(project.getCrs()) == false) {
            throw new RenderingException("Coordinate Reference Systems are different: " + worldEnvelope.getCoordinateReferenceSystem() + " / " + project.getCrs());
        }

        // check if this method have not been called few milliseconds before
        if (checkMinimumRenderInterval() == false) {
            return;
        }

        // on thread at a time renderer map for now
        if (renderLock.tryLock() == false) {
            logger.error("Abort rendering operations, rendering is already in progress");
            return;
        }

        // set essential parameters after verifications
        computeMinAndMaxPartialSideWu();
        this.worldEnvelope = worldEnvelope;
        this.renderedSizePx = pixelDim;
        computePartialSideWu();

        try {

            logger.debug("Rendering component: " + this);

            // iterate layers, sorted by z-index
            for (AbstractLayer lay : project.getLayersList()) {

                String layId = lay.getId();

                // retrieve map content associated with layer

                // if map does no exist, create one
                MapContent map = layerMapContents.get(layId);
                if (map == null) {
                    map = lay.buildMapContent();
                    layerMapContents.put(layId, map);
                }

                // retrieve partial factory associated with layer
                RenderedPartialFactory factory = partialFactories.get(layId);
                if (factory == null) {
                    factory = new RenderedPartialFactory(project.getRenderedPartialsStore(), map, layId);
                    factory.setDebugMode(debugMode);
                    partialFactories.put(layId, factory);
                }

                // if map is not up to date, create a new one and invalidate cache
                if (GeoUtils.isMapContains(map, lay.getInternalLayer()) == false) {

                    System.out.println("Cache invalidated ! " + layId);

                    map = lay.buildMapContent();
                    layerMapContents.put(layId, map);
                    factory.setMapContent(map);

                    project.getRenderedPartialsStore().deletePartialsForLayer(layId);

                }

                // search which partials are necessary to display
                RenderedPartialQueryResult newPartials = factory.intersect(worldEnvelope, partialSideWu,
                        () -> {
                            // each time a partial come, map will be repaint
                            notifm.fireEvent(new RenderingEvent(RenderingEvent.NEW_PARTIAL_LOADED));

                            // notify all waiters that new partial come
                            synchronized (CachedRenderingEngine.this) {
                                CachedRenderingEngine.this.notifyAll();
                            }

                        });

                // store it to draw it later
                currentPartials.put(layId, newPartials);
            }


        } finally {
            renderLock.unlock();
        }
    }

    /**
     * Compute optimal partial side size in world units, relative to world bounds and pixel dimensions
     */
    private void computePartialSideWu() {
        double coeff = renderedSizePx.getWidth() / partialSidePx;
        double worldWidth = worldEnvelope.getMaxX() - worldEnvelope.getMinX();
        setPartialSideWu(worldWidth / coeff);
    }

    /**
     * Adapt rendering parameters to render all map
     *
     * @param renderedSizePx
     */
    public void setParametersToRenderWholeMap(Dimension renderedSizePx) {

        this.renderedSizePx = renderedSizePx;
        worldEnvelope = project.getMaximumBounds();

        double worldWidth = worldEnvelope.getMaxX() - worldEnvelope.getMinX();
        double renderedSurfaceWidth = renderedSizePx.getWidth();

        setPartialSideWu(worldWidth * partialSidePx / renderedSurfaceWidth);
    }

    public ReferencedEnvelope getWorldEnvelope() {
        return worldEnvelope;
    }

    /**
     * Check if a minimum interval of time is respected between rendering operations, to avoid too many calls
     *
     * @return
     */
    private boolean checkMinimumRenderInterval() {

        // check last rendering time
        boolean render = System.currentTimeMillis() - lastRender > renderMinIntervalMs;

        // save time if needed
        if (render) {
            lastRender = System.currentTimeMillis();
        }

        return render;
    }

    /**
     * Return size of each partial rendered in pixel
     *
     * @return
     */
    public double getPartialSidePx() {
        return partialSidePx;
    }

    /**
     * If set to true, more information will be displayed
     *
     * @param debugMode
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public EventNotificationManager getNotificationManager() {
        return notifm;
    }

    /**
     * Get size of each partials in world unit
     *
     * @return
     */
    public double getPartialSideWu() {
        return partialSideWu;
    }

    /**
     * Get a coefficient between world unit and pixel unit
     *
     * @return
     */
    public double getScale() {
        return partialSideWu / partialSidePx;
    }

    /**
     * Set size of partial side and check if value is correct
     *
     * @param value
     */
    private void setPartialSideWu(double value) {

        partialSideWu = value;

        if (scaleLimited) {

            // check if value is not too small
            if (partialSideWu < minimumPartialSideWu) {
                partialSideWu = minimumPartialSideWu;
            }

            // check if value is not too big
            else if (partialSideWu > maximumPartialSideWu) {
                partialSideWu = maximumPartialSideWu;
            }

        }

    }


    /**
     * Block current thread until all work of rendering is done
     */
    public synchronized void waitForRendering() {

        // iterate all sets of partials
        Iterator<String> keys = currentPartials.keySet().iterator();

        while (keys.hasNext()) {
            String k = keys.next();
            RenderedPartialQueryResult v = currentPartials.get(k);

            // wait until all sets are ready
            while (v.isWorkDone() == false) {
                try {
                    wait(20);
                } catch (InterruptedException e) {
                    logger.error(e);
                }
            }

        }

    }

    /**
     * If set to true, scale will be limited
     *
     * @param scaleLimited
     */
    public void setScaleLimited(boolean scaleLimited) {
        this.scaleLimited = scaleLimited;
    }

    public double getMinimumPartialSideWu() {
        return minimumPartialSideWu;
    }

    public double getMaximumPartialSideWu() {
        return maximumPartialSideWu;
    }
}
