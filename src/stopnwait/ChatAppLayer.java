package stopnwait;

import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	private _CHAT_APP m_ChatApp = new _CHAT_APP();
	private int bufferCount = 0;

	private class _CHAT_APP {
		byte[] capp_totlen;
		byte capp_type;
		byte capp_unused;
		byte[] data;

		public _CHAT_APP() {
			this.capp_totlen = new byte[2];
			this.capp_type = 0x00;
			this.capp_unused = 0x00;
			this.data = null;
		}
	}
	
	public ChatAppLayer(String pName) {
		// TODO Auto-generated constructor stub
		pLayerName = pName;
	}

	public byte[] addHeader(byte type, byte[] input, int totlen) {
		byte[] buf = new byte[input.length+4];
		
		buf[0] = (byte) (totlen % 128);
		buf[1] = (byte) (totlen / 128);
		buf[2] = type;
		buf[3] = this.m_ChatApp.capp_unused;

		System.arraycopy(input, 0, buf, 4, input.length);

		return buf;
	}

	public boolean Send(byte[] input, int length) {
		System.out.println("send_chatapp_start");
		
		// No fragmentation
		if (length <= 10) {
			byte[] data = addHeader((byte) 0x00, input, length);
			GetUnderLayer().Send(data, length+4);
			return true;
		}
		
		// First fragment
		int i = 0;
		byte[] fragment = new byte[10];
		System.arraycopy(input, 0, fragment, 0, 10);
		byte[] data = addHeader((byte) 0x01, fragment, length);
		GetUnderLayer().Send(data, 14);
		
		// Middle fragments
		int restLength = length-10;
		for (i = 1; restLength > 10; i++) {
			System.arraycopy(input, 10*i, fragment, 0, 10);
			data = addHeader((byte) 0x02, fragment, length);
			GetUnderLayer().Send(data, 14);
			restLength -= 10;
		}
		
		// Last fragment
		fragment = new byte[restLength];
		System.arraycopy(input, 10*i, fragment, 0, restLength);
		data = addHeader((byte) 0x03, fragment, length);
		GetUnderLayer().Send(data, restLength+4);
		System.out.println("send_chatapp_end");
		return true;
	}

	public byte[] removeHeader(byte[] input, int length) {
		byte[] data = new byte[length-4];
		System.arraycopy(input, 4, data, 0, length-4);
		return data;
	}

	public synchronized boolean Receive(byte[] input) {
		System.out.println("receive_chatapp_start");
		
		// Not fragment
		if (input[2] == this.m_ChatApp.capp_type) {			
			byte[] data = removeHeader(input, input.length);
			GetUpperLayer(0).Receive(data);
			return true;
		}
		
		// Buffer check
		if (this.m_ChatApp.data == null) {
			this.m_ChatApp.capp_totlen[0] = input[0];
			this.m_ChatApp.capp_totlen[1] = input[1];
			this.m_ChatApp.data = new byte[128 * this.m_ChatApp.capp_totlen[1] + this.m_ChatApp.capp_totlen[0]];
		}
		
		// Insert fragment into buffer
		byte[] fragment = removeHeader(input, input.length);
		System.arraycopy(fragment, 0, this.m_ChatApp.data, 10*this.bufferCount++, fragment.length);
		if (input[2] == (byte) 0x03) {
			if (this.m_ChatApp.data[this.m_ChatApp.data.length-1] == (byte) 0x00) {
				System.out.println("Buffer Error");
				return false;
			}
			GetUpperLayer(0).Receive(this.m_ChatApp.data);
			this.m_ChatApp = new _CHAT_APP();
			this.bufferCount = 0;
		}
		System.out.println("receive_chatapp_end");
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
