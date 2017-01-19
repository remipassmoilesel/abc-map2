package org.abcmap.core.wms;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.abcmap.core.configuration.ConfigurationConstants;
import org.abcmap.core.dao.DataModel;

import java.util.Objects;

/**
 * Allow to store WMS information on layers
 */
@DatabaseTable(tableName = ConfigurationConstants.SQL_TABLE_PREFIX + "WMS_INDEX")
public class WmsLayerEntry implements DataModel {

    private static final String ID_FIELD_NAME = "ID";
    private static final String URL_FIELD_NAME = "URL";
    private static final String WMS_NAME_FIELD_NAME = "WMS_LAYER_NAME";

    @DatabaseField(id = true, columnName = ID_FIELD_NAME)
    private String layerId;

    @DatabaseField(columnName = URL_FIELD_NAME)
    private String url;

    @DatabaseField(columnName = WMS_NAME_FIELD_NAME)
    private String wmsLayerName;

    public WmsLayerEntry() {

    }

    public WmsLayerEntry(String layerId, String url, String wmsLayerName) {
        this.layerId = layerId;
        this.url = url;
        this.wmsLayerName = wmsLayerName;
    }

    public String getLayerId() {
        return layerId;
    }

    public void setLayerId(String layerId) {
        this.layerId = layerId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getWmsLayerName() {
        return wmsLayerName;
    }

    public void setWMSLayerName(String layerName) {
        this.wmsLayerName = layerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WmsLayerEntry wmsEntry = (WmsLayerEntry) o;
        return Objects.equals(layerId, wmsEntry.layerId) &&
                Objects.equals(url, wmsEntry.url) &&
                Objects.equals(wmsLayerName, wmsEntry.wmsLayerName);
    }

    @Override
    public String toString() {
        return "WMSEntry{" +
                "layerId='" + layerId + '\'' +
                ", url='" + url + '\'' +
                ", WMSlayerName='" + wmsLayerName + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(layerId, url, wmsLayerName);
    }
}