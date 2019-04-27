package stopnwait;


import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	private _CHAT_APP m_ChatApp = new _CHAT_APP();

	private class _CHAT_APP {
		byte[] capp_totlen;
		byte capp_type;
		byte capp_unused;
	//	byte[] data;

		public _CHAT_APP() {
			this.capp_totlen = new byte[2];
			this.capp_type = 0x00;
			this.capp_unused = 0x00;
		//	this.data = null;
		}
	}
	
	public ChatAppLayer(String pName) {
		// TODO Auto-generated constructor stub
		pLayerName = pName;
	}

	public byte[] ObjToByte(byte[] input, int length) {
		byte[] buf = new byte[length+4];
		
		buf[0] = this.m_ChatApp.capp_totlen[0];
		buf[1] = this.m_ChatApp.capp_totlen[1];
		buf[2] = this.m_ChatApp.capp_type;
		buf[3] = this.m_ChatApp.capp_unused;

		System.arraycopy(input, 0, buf, 4, length);

		return buf;
	}

	public boolean Send(byte[] input, int length) {
		System.out.println("send_chatapp");
		
		this.m_ChatApp.capp_totlen[0] = (byte) (length % 256);
		this.m_ChatApp.capp_totlen[1] = (byte) (length / 256);
		
		byte[] data = ObjToByte(input, length);
		GetUnderLayer().Send(data, length+4);
		return true;
	}

	public byte[] RemoveHeader(byte[] input, int length) {
		byte[] data = new byte[length-4];
		System.arraycopy(input, 4, data, 0, length-4);
		return data;
	}

	public synchronized boolean Receive(byte[] input) {
		System.out.println("Receive_chatapp");
		byte[] data = RemoveHeader(input, input.length);
		this.GetUpperLayer(0).Receive(data);
		return true;
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
