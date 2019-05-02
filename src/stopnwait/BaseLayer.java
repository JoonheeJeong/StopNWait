package stopnwait;

import java.util.ArrayList;

interface BaseLayer {
	public final int upperLayerCount = 0;
	public final String pLayerName = null;
	public final BaseLayer underLayer = null;
	public final ArrayList<BaseLayer> upperLayerList = new ArrayList<BaseLayer>();

	public String getLayerName();

	public BaseLayer getUnderLayer();

	public BaseLayer getUpperLayer(int index);

	public void setUnderLayer(BaseLayer underLayer);

	public void setUpperLayer(BaseLayer upperLayer);

	public default void setUnderUpperLayer(BaseLayer layer) {
	}

	public void setUpperUnderLayer(BaseLayer layer);

	public default boolean send(byte[] input, int length) {
		return false;
	}

	public default boolean send(String filename) {
		return false;
	}

	public default boolean receive(byte[] input) {
		return false;
	}

	public default boolean receive() {
		return false;
	}
}
