package stopnwait;

import java.util.ArrayList;

public class EthernetLayer implements BaseLayer {
	private int upperLayerCount = 0;
	private String layerName = null;
	private BaseLayer p_UnderLayer = null;
	private ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	private Frame frame = new Frame();
	private boolean isAckReceived = true;

	private class Frame {
		byte[] dstMacAddress;
		byte[] srcMacAddress;
		byte[] type;
		//byte[] enet_data;
		
		//Data type: 0x2016, ACK type: 0x1004
		public Frame() {
			this.dstMacAddress = new byte[6];
			this.srcMacAddress = new byte[6];
			this.type = new byte[2];
			this.type[0] = 0x20;
			this.type[1] = 0x16;
			//this.enet_data = null;
		}
	}
	
	private enum TransmissionType{
		DATA,
		ACK,
		NONE;
	}
	
	public EthernetLayer(String layerName) {
		this.layerName = layerName;
	}
	
	public byte[] addHeader(byte[] input, int length, boolean isACK) {
		byte[] buf = new byte[length+14];
		
		System.arraycopy(this.frame.dstMacAddress, 0, buf, 0, 6);
		System.arraycopy(this.frame.srcMacAddress, 0, buf, 6, 6);
		if (isACK) { // ACK
			buf[12] = 0x10;
			buf[13] = 0x04;
		} else { // DATA
			System.arraycopy(this.frame.type, 0, buf, 12, 2);
		}
		System.arraycopy(input, 0, buf, 14, length);

		return buf;
	}
	
	public boolean send(byte[] input, int length) {
		System.out.println("send_ethernet_start");
		byte[] data = addHeader(input, length, false);
		getUnderLayer().send(data, length+14);
		
		this.isAckReceived = false; // 송신 후 ACK 수신 대기
		synchronized(this) {
			while (!isAckReceived) {
				try {
					wait();
				} catch (InterruptedException e) {}
			}
		}
		System.out.println("send_ethernet_end");
		return true;
	}
	
	public byte[] removeHeader(byte[] input, int length) {
		int realLength = length;
		if (realLength == 60) { // 수신 패킷 최소 크기(60)에 대한 보정
			int lastIndex = 59;
			for (; lastIndex > 17; lastIndex--) {
				if (input[lastIndex] != (byte) 0x00)
					break;
			}
			realLength = lastIndex + 1;
		}
		int reducedLength = realLength - 14;
		byte[] data = new byte[reducedLength];
		System.arraycopy(input, 14, data, 0, reducedLength);
		return data;
	}

	public synchronized boolean receive(byte[] input) {
		System.out.println("receive_ethernet_start");
		if ( !isReceivable(input) ) {
			System.out.println("receive_ethernet_end");
			return false;
		}
		TransmissionType transmissionType = getTransmissionType(input);
		switch (transmissionType) {
		case DATA:
			byte[] data = removeHeader(input, input.length);
			getUpperLayer(0).receive(data);
			sendAck(generateAck());
			System.out.println("receive_ethernet_end");
			return true;
		case ACK: // Wake up waiting send thread
			this.isAckReceived = true;
			notify();
			System.out.println("receive_ethernet_end");
			return true;
		default: // DATA 타입과 ACK 타입이 아닌 경우: NONE (noise)
			System.out.println("receive_ethernet_end");
			return false;
		}
	}
	
	private boolean isReceivable(byte[] input) {
		if (isMyPacket(input))
			return false;
		
		if (isBroadCast(input))
			return true;
		if (isSentToMe(input))
			return true;
		
		return false;
	}
	
	private TransmissionType getTransmissionType(byte[] input) {
		if (input[12] == 0x20 && input[13] == 0x16)
			return TransmissionType.DATA;
		if (input[12] == 0x10 && input[13] == 0x04)
			return TransmissionType.ACK;
		return TransmissionType.NONE;
	}
	
	private byte[] generateAck() {
		return addHeader(new byte[0], 0, true);
	}
	
	private void sendAck(byte[] ACK) {
		getUnderLayer().send(ACK, 14);
	}
	
	private boolean isBroadCast(byte[] input) {
		for (int i = 0; i < 6; i++) {
			if (input[i] != (byte) 0xff)
				return false;
		}
		return true;
	}
	
	private boolean isSentToMe(byte[] input) {
		byte[] temp_src = this.frame.srcMacAddress;
		for (int i = 0; i < 6; i++) {
			if (temp_src[i] != input[i])
				return false;
		}
		return true;
	}
	
//	private boolean isSentFromMyDst(byte[] input) {
//		byte[] temp_dst = this.m_Frame.enet_dstaddr.addr;
//		for (int i = 0; i < 6; i++) {
//			if (input[i+6] != temp_dst[i])
//				return false;
//		}
//		return true;
//	}
	
	private boolean isMyPacket(byte[] input) {
		byte[] temp_src = this.frame.srcMacAddress;
		for (int i = 0; i < 6; i++) {
			if (temp_src[i] != input[i+6])
				return false;
		}
		return true;
	}
	
	public void setSrcAddr(byte[] srcAddr) {
		System.arraycopy(srcAddr, 0, this.frame.srcMacAddress, 0, 6);
	}
	
	public void setDstAddr(byte[] dstAddr) {
		System.arraycopy(dstAddr, 0, this.frame.dstMacAddress, 0, 6);
	}
	
	@Override
	public String getLayerName() {
		return layerName;
	}

	@Override
	public BaseLayer getUnderLayer() {
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}

	@Override
	public BaseLayer getUpperLayer(int index) {
		if (index < 0 || index > upperLayerCount || upperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(index);
	}

	@Override
	public void setUnderLayer(BaseLayer underLayer) {
		if (underLayer == null)
			return;
		this.p_UnderLayer = underLayer;
	}

	@Override
	public void setUpperLayer(BaseLayer upperLayer) {
		if (upperLayer == null)
			return;
		this.p_aUpperLayer.add(upperLayerCount++, upperLayer);
	}

	@Override
	public void setUpperUnderLayer(BaseLayer layer) {
		this.setUpperLayer(layer);
		layer.setUnderLayer(this);
	}

}
