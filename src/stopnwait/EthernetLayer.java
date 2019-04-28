package stopnwait;

import java.util.ArrayList;

public class EthernetLayer implements BaseLayer {
	private int nUpperLayerCount = 0;
	private String pLayerName = null;
	private BaseLayer p_UnderLayer = null;
	private ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	_ETHERNET_FRAME m_Frame = new _ETHERNET_FRAME();

	private class _ETHERNET_ADDR {
		private byte[] addr = new byte[6];

		public _ETHERNET_ADDR() {
			this.addr[0] = (byte) 0x00;
			this.addr[1] = (byte) 0x00;
			this.addr[2] = (byte) 0x00;
			this.addr[3] = (byte) 0x00;
			this.addr[4] = (byte) 0x00;
			this.addr[5] = (byte) 0x00;
		}
	}
	
	private class _ETHERNET_FRAME {
		_ETHERNET_ADDR enet_dstaddr;
		_ETHERNET_ADDR enet_srcaddr;
		byte[] enet_type;
		//byte[] enet_data;
		
		//Data type: 0x2016, ACK type: 0x1004
		public _ETHERNET_FRAME() {
			this.enet_dstaddr = new _ETHERNET_ADDR();
			this.enet_srcaddr = new _ETHERNET_ADDR();
			this.enet_type = new byte[2];
			this.enet_type[0] = 0x20;
			this.enet_type[1] = 0x16;
			//this.enet_data = null;
		}
	}
	
	private enum TransmissionType{
		DATA,
		ACK,
		NONE; 
	}
	
	public EthernetLayer(String pName) {
		pLayerName = pName;
	}
	
	public byte[] addHeader(byte[] input, int length) {
		byte[] buf = new byte[length+14];
		
		System.arraycopy(this.m_Frame.enet_dstaddr.addr, 0, buf, 0, 6);
		System.arraycopy(this.m_Frame.enet_srcaddr.addr, 0, buf, 6, 6);
		if (length == 4 && input[2] == (byte) 0x00) { // ACK
			buf[12] = 0x10;
			buf[13] = 0x04;
		} else { // DATA
			System.arraycopy(this.m_Frame.enet_type, 0, buf, 12, 2);
		}
		System.arraycopy(input, 0, buf, 14, length);

		return buf;
	}
	
	public boolean Send(byte[] input, int length) {
		System.out.println("send_ethernet_start");
		byte[] data = addHeader(input, length);
		GetUnderLayer().Send(data, length+14);
		System.out.println("send_ethernet_end");
		return true;
	}
	
	public byte[] removeHeader(byte[] input, int length) {
		byte[] data = new byte[length-14];
		System.arraycopy(input, 14, data, 0, length-14);
		return data;
	}

	public synchronized boolean Receive(byte[] input) {
		System.out.println("receive_ethernet_start");
		if ( !(isBroadCast(input) || isCorrespondingAddress(input)) ) {
			return false;
		}
		TransmissionType transmissionType = getTransmissionType(input);
		switch (transmissionType) {
		case DATA:
			byte[] data = removeHeader(input, input.length);
			GetUpperLayer(0).Receive(data);
			System.out.println("receive_ethernet_end");
			return true;
		case ACK:
			GetUpperLayer(0).Send(new byte[0], 0);
			System.out.println("receive_ethernet_end");
			return true;
		default: // DATA 타입과 ACK 타입이 아닌 경우: NONE (garbage)
			System.out.println("receive_ethernet_end");
			return false;
		}
	}
	
	private boolean isBroadCast(byte[] input) {
		for (int i = 0; i < 6; i++) {
			if (input[i] != (byte) 0xff)
				return false;
		}
		return true;
	}
	
	private boolean isCorrespondingAddress(byte[] input) {
		if (isToMySrc(input) && isFromMyDst(input))
			return true;
		return false;
	}
	
	private boolean isToMySrc(byte[] input) {
		byte[] temp_src = this.m_Frame.enet_srcaddr.addr;
		for (int i = 0; i < 6; i++) {
			if (input[i] != temp_src[i])
				return false;
		}
		return true;
	}
	
	private boolean isFromMyDst(byte[] input) {
		byte[] temp_dst = this.m_Frame.enet_dstaddr.addr;
		for (int i = 0; i < 6; i++) {
			if (input[i+6] != temp_dst[i])
				return false;
		}
		return true;
	}
	
	private TransmissionType getTransmissionType(byte[] input) {
		if (input[12] == 0x20 && input[13] == 0x16)
			return TransmissionType.DATA;
		else if (input[12] == 0x10 && input[13] == 0x04)
			return TransmissionType.ACK;
		else
			return TransmissionType.NONE;
	}
	
	public void setSrcAddr(byte[] srcAddr) {
		System.arraycopy(srcAddr, 0, this.m_Frame.enet_srcaddr.addr, 0, 6);
	}
	
	public void setDstAddr(byte[] dstAddr) {
		System.arraycopy(dstAddr, 0, this.m_Frame.enet_dstaddr.addr, 0, 6);
	}
	
	@Override
	public String GetLayerName() {
		// TODO Auto-generated method stub
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {
		// TODO Auto-generated method stub
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) {
		// TODO Auto-generated method stub
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		// TODO Auto-generated method stub
		if (pUnderLayer == null)
			return;
		this.p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		// TODO Auto-generated method stub
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);

	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}

}
