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
		
		//Data type: 0x2016, ACK type: 0x0102
		public _ETHERNET_FRAME() {
			this.enet_dstaddr = new _ETHERNET_ADDR();
			this.enet_srcaddr = new _ETHERNET_ADDR();
			this.enet_type = new byte[2];
			this.enet_type[0] = 0x20;
			this.enet_type[1] = 0x16;
			//this.enet_data = null;
		}
	}
	
	private enum Type{
		DATA,
		ACK,
		NONE; 
	}
	
	
	public EthernetLayer(String pName) {
		pLayerName = pName;
	}
	
	public byte[] ObjToByte(byte[] input, int length) {
		byte[] buf = new byte[length+14];
		
		System.arraycopy(this.m_Frame.enet_dstaddr.addr, 0, buf, 0, 6);
		System.arraycopy(this.m_Frame.enet_srcaddr.addr, 0, buf, 6, 6);
		System.arraycopy(this.m_Frame.enet_type, 0, buf, 12, 2);
		System.arraycopy(input, 0, buf, 14, length);

		return buf;
	}
	
	public boolean Send(byte[] input, int length) {
		System.out.println("send_ethernet");
		byte[] data = ObjToByte(input, length);
		//this.m_Frame.enet_data = data;
		GetUnderLayer().Send(data, length+14);
		return true;
	}
	
	public byte[] RemoveHeader(byte[] input, int length) {
		byte[] data = new byte[length-14];
		System.arraycopy(input, 14, data, 0, length-14);
		return data;
	}

	public synchronized boolean Receive(byte[] input) {
		System.out.println("Receive_ethernet");
		if ( !(isBroadCast(input) || isCorrespondingAddress(input)) ) {
			return false;
		}
		Type type = getType(input);
		switch (type) {
		case DATA:
			byte[] data = RemoveHeader(input, input.length);
			this.GetUpperLayer(0).Receive(data);
			return true;
		case ACK:
			this.GetUpperLayer(0).Send(new byte[0], 0);
			return true;
		case NONE:
		default:
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
	
	private Type getType(byte[] input) {
		if (input[12] == 0x20 && input[13] == 0x16)
			return Type.DATA;
		else if (input[12] == 0x01 && input[13] == 0x02)
			return Type.ACK;
		else
			return Type.NONE;
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
