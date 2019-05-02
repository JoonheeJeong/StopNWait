package stopnwait;

import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer {
	public int upperLayerCount = 0;
	public String layerName = null;
	public BaseLayer underLayer = null;
	public ArrayList<BaseLayer> upperLayerList = new ArrayList<BaseLayer>();
	
	private ChatApp chatApp = new ChatApp();
	private static final int FRAGMENTATION_SIZE = 10;
	private int bufferCount;

	private class ChatApp {
		private byte[] totlen;
		private byte type;
		private byte unused;
		private byte[] buffer;

		public ChatApp() {
			bufferCount = 0;
			totlen = new byte[2];
			type = 0x00;
			unused = 0x00;
			buffer = null;
		}
	}
	
	public ChatAppLayer(String layerName) {
		this.layerName = layerName;
	}

	public byte[] addHeader(byte type, byte[] input, int totlen) {
		byte[] buf = new byte[input.length+4];
		
		buf[0] = (byte) (totlen % 128);
		buf[1] = (byte) (totlen / 128);
		buf[2] = type;
		buf[3] = this.chatApp.unused;

		System.arraycopy(input, 0, buf, 4, input.length);

		return buf;
	}

	public boolean send(byte[] input, int length) {
		System.out.println("send_chatapp_start");
		
		int totlen = length;
		
		// No fragmentation
		if (totlen <= 10) {
			byte[] data = addHeader((byte) 0x00, input, totlen);
			getUnderLayer().send(data, totlen+4);
			return true;
		}
		
		// First fragment
		int fragmentCount = 0;
		byte[] rawFragment = fragmentation(input, fragmentCount, FRAGMENTATION_SIZE);
		byte[] fragmentWithHeader = addHeader((byte) 0x01, rawFragment, totlen);
		getUnderLayer().send(fragmentWithHeader, 14);
		
		// Middle fragments
		int theRestOfLength = totlen-10;
		while (theRestOfLength > 10) {
			rawFragment = fragmentation(input, ++fragmentCount, FRAGMENTATION_SIZE);
			fragmentWithHeader = addHeader((byte) 0x02, rawFragment, totlen);
			getUnderLayer().send(fragmentWithHeader, 14);
			theRestOfLength -= 10;
		}
		
		// Last fragment
		rawFragment = fragmentation(input, ++fragmentCount, theRestOfLength);
		fragmentWithHeader = addHeader((byte) 0x03, rawFragment, length);
		getUnderLayer().send(fragmentWithHeader, theRestOfLength+4);
		System.out.println("send_chatapp_end");
		return true;
	}

	private byte[] fragmentation(byte[] input, int fragmentCount, int fragmentationSize) {
		byte[] rawFragment = new byte[fragmentationSize];
		System.arraycopy(input, fragmentCount*FRAGMENTATION_SIZE, rawFragment, 0, fragmentationSize);
		return rawFragment;
	}
	
	private byte[] removeHeader(byte[] input, int length) {
		byte[] data = new byte[length-4];
		System.arraycopy(input, 4, data, 0, length-4);
		return data;
	}

	public synchronized boolean receive(byte[] input) {
		System.out.println("receive_chatapp_start");
		
		// Not fragment
		if (input[2] == this.chatApp.type) {			
			byte[] data = removeHeader(input, input.length);
			getUpperLayer(0).receive(data);
			return true;
		}
		
		// Buffer check
		if (input[2] == (byte) 0x01) {
			this.chatApp.totlen[0] = input[0];
			this.chatApp.totlen[1] = input[1];
			int realTotalLength = 128 * this.chatApp.totlen[1] + this.chatApp.totlen[0];
			this.chatApp.buffer = new byte[realTotalLength];
		}
		
		// Insert fragment into buffer
		byte[] fragment = removeHeader(input, input.length);
		insertIntoBuffer(fragment);
		
		// Upper Layer receive full message when the last fragment come in
		if (input[2] == (byte) 0x03) {
			if (!isAllDataInBuffer(fragment.length)) {
				System.out.println("[Error] Any data is not arrived.");
				System.out.println("receive_chatapp_end");
				return false;
			}
			getUpperLayer(0).receive(this.chatApp.buffer);
			this.chatApp = new ChatApp();
		}
		
		System.out.println("receive_chatapp_end");
		return true;
	}
	
	private void insertIntoBuffer(byte[] fragment) {
		System.arraycopy(fragment, 0, this.chatApp.buffer, FRAGMENTATION_SIZE*bufferCount++, fragment.length);
	}
	
	private boolean isAllDataInBuffer(int lastFragmentSize) {
		int totlen = 128 * this.chatApp.totlen[1] + this.chatApp.totlen[0];
		int totalDataSizeInBuffer = (bufferCount - 1) * FRAGMENTATION_SIZE + lastFragmentSize;
		if (totlen == totalDataSizeInBuffer)
			return true;
		return false;
	}
	
	@Override
	public String getLayerName() {
		return layerName;
	}

	@Override
	public BaseLayer getUnderLayer() {
		if (underLayer == null)
			return null;
		return underLayer;
	}

	@Override
	public BaseLayer getUpperLayer(int index) {
		if (index < 0 || index > upperLayerCount || upperLayerCount < 0)
			return null;
		return upperLayerList.get(index);
	}

	@Override
	public void setUnderLayer(BaseLayer underLayer) {
		if (underLayer == null)
			return;
		this.underLayer = underLayer;
	}

	@Override
	public void setUpperLayer(BaseLayer upperLayer) {
		if (upperLayer == null)
			return;
		this.upperLayerList.add(upperLayerCount++, upperLayer);
	}

	@Override
	public void setUpperUnderLayer(BaseLayer layer) {
		this.setUpperLayer(layer);
		layer.setUnderLayer(this);
	}

}
