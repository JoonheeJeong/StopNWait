package stopnwait;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class StopWaitDlg extends JFrame implements BaseLayer {

	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	private static LayerManager m_LayerMgr = new LayerManager();

	private JTextField ChattingWrite;

	Container contentPane;

	JTextArea ChattingArea;
	JTextArea srcMacAddr;
	JTextArea dstMacAddr;

	JLabel lblnic;
	JLabel lblsrc;
	JLabel lbldst;

	JButton Setting_Button;
	JButton Chat_send_Button;

	String Text;

	static JComboBox<String> NIComboBox;

	int adapterNumber = 0;
	
	static {
		try {
			System.load(new File("jnetpcap.dll").getAbsolutePath());
			System.out.println(new File("jnetpcap.dll").getAbsolutePath());
		} catch (UnsatisfiedLinkError e) {
			System.out.println("Native code library failed to load.\n" + e);
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		m_LayerMgr.AddLayer(new NILayer("NI"));
		m_LayerMgr.AddLayer(new EthernetLayer("Ethernet"));
		m_LayerMgr.AddLayer(new ChatAppLayer("Chat"));
		m_LayerMgr.AddLayer(new StopWaitDlg("GUI"));

		m_LayerMgr.ConnectLayers(" NI ( *Ethernet ( *Chat ( *GUI ) ) )");
	}

	public StopWaitDlg(String pName) {
		pLayerName = pName;
		
		setTitle("Stop & Wait Protocol");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(250, 250, 644, 425);
		contentPane = new JPanel();
		((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JPanel chattingPanel = new JPanel();// chatting panel
		chattingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "chatting",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		chattingPanel.setBounds(10, 5, 360, 276);
		contentPane.add(chattingPanel);
		chattingPanel.setLayout(null);

		JPanel chattingEditorPanel = new JPanel();// chatting write panel
		chattingEditorPanel.setBounds(10, 15, 340, 210);
		chattingPanel.add(chattingEditorPanel);
		chattingEditorPanel.setLayout(null);

		ChattingArea = new JTextArea();
		ChattingArea.setEditable(false);
		ChattingArea.setBounds(0, 0, 340, 210);
		chattingEditorPanel.add(ChattingArea);// chatting edit

		JPanel chattingInputPanel = new JPanel();// chatting write panel
		chattingInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		chattingInputPanel.setBounds(10, 230, 250, 20);
		chattingPanel.add(chattingInputPanel);
		chattingInputPanel.setLayout(null);

		ChattingWrite = new JTextField();
		ChattingWrite.setBounds(2, 2, 250, 20);// 249
		chattingInputPanel.add(ChattingWrite);
		ChattingWrite.setColumns(10);// writing area

		JPanel settingPanel = new JPanel();
		settingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "setting",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		settingPanel.setBounds(380, 5, 236, 371);
		contentPane.add(settingPanel);
		settingPanel.setLayout(null);

		///////////////////////// NIComboBox////////////////////
		NIComboBox = new JComboBox<String>();
		NILayer nil = (NILayer) m_LayerMgr.GetLayer("NI");
		for (int i = 0; i < nil.m_pAdapterList.size(); i++) {
			NIComboBox.addItem(nil.m_pAdapterList.get(i).getDescription());
		}
		NIComboBox.setSelectedIndex(0);
		NIComboBox.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		NIComboBox.setBounds(10, 46, 170, 30);
		NIComboBox.addActionListener(new setAddressListener());
		settingPanel.add(NIComboBox);
		NIComboBox.setLayout(null);
		
		lblnic = new JLabel("Network Interface Option");
		lblnic.setBounds(10, 25, 170, 20);
		settingPanel.add(lblnic);
		///////////////////////////////////////////////////////

		JPanel srcAddrPanel = new JPanel();
		srcAddrPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		srcAddrPanel.setBounds(10, 96, 170, 20);
		settingPanel.add(srcAddrPanel);
		srcAddrPanel.setLayout(null);

		lblsrc = new JLabel("Source Mac Address");
		lblsrc.setBounds(10, 75, 170, 20);
		settingPanel.add(lblsrc);

		srcMacAddr = new JTextArea();
		/////////////////////////////////////////////////////////
		byte[] mac = getMacAddress();
		srcMacAddr.setText(getStringFromByteMacAddress(mac));
		/////////////////////////////////////////////////////////
		srcMacAddr.setBounds(2, 2, 170, 20);
		srcAddrPanel.add(srcMacAddr);

		JPanel dstAddrPanel = new JPanel();
		dstAddrPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		dstAddrPanel.setBounds(10, 212, 170, 20);
		settingPanel.add(dstAddrPanel);
		dstAddrPanel.setLayout(null);

		lbldst = new JLabel("Destination Mac Address");
		lbldst.setBounds(10, 187, 170, 20);
		settingPanel.add(lbldst);

		dstMacAddr = new JTextArea();
		dstMacAddr.setBounds(2, 2, 170, 20);
		dstAddrPanel.add(dstMacAddr);

		Setting_Button = new JButton("Setting");
		Setting_Button.setBounds(80, 270, 100, 20);
		Setting_Button.addActionListener(new setAddressListener());
		settingPanel.add(Setting_Button);

		Chat_send_Button = new JButton("Send");
		Chat_send_Button.setBounds(270, 230, 80, 20);
		Chat_send_Button.addActionListener(new setAddressListener());
		chattingPanel.add(Chat_send_Button);// chatting send button

		setVisible(true);

	}

	class setAddressListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			///////////////////////////////////////////
			Object o = e.getSource();
			byte[] byteSrcMac = null;
			if (o == NIComboBox) {
				byteSrcMac = getMacAddress();
				srcMacAddr.setText(getStringFromByteMacAddress(byteSrcMac));
			///////////////////////////////////////////
			} else if (o == Setting_Button) {
				if (Setting_Button.getText() == "Reset") {
					srcMacAddr.setText("");
					dstMacAddr.setText("");
					Setting_Button.setText("Setting");
					srcMacAddr.setEditable(true);
					dstMacAddr.setEditable(true);
					/////////////진행중인 리씨브 쓰레드 중단시키기/////
					
					
					////////////////////////////////////
				} else {
					///////////// 소스 주소/////////////////
					EthernetLayer ethernetLayer = (EthernetLayer) m_LayerMgr.GetLayer("Ethernet");
					byteSrcMac = getMacAddress();
					ethernetLayer.setSrcAddr(byteSrcMac);
					////////////////////////////////////
					///////////// 목적 주소/////////////////
					String strDstMac = dstMacAddr.getText();
					byte[] byteDstMac = getByteFromStringMacAddress(strDstMac);
					ethernetLayer.setDstAddr(byteDstMac);
					////////////////////////////////////
					////////////// 어뎁터 설정///////////////
					NILayer networkInterfaceLayer = (NILayer) m_LayerMgr.GetLayer("NI");
					int index = NIComboBox.getSelectedIndex();
					networkInterfaceLayer.SetAdapterNumber(index);
					////////////////////////////////////
					Setting_Button.setText("Reset");
					srcMacAddr.setEditable(false);
					dstMacAddr.setEditable(false);
				}
			} else if (o == Chat_send_Button) {
				if (!Setting_Button.getText().equals("Reset")) {
					JOptionPane.showMessageDialog(null, "You must set mac address first.\n");
					return;
				}
				System.out.println("send_dlg");
				String sendingText = ChattingWrite.getText();
				ChattingWrite.setText("");
				ChattingArea.append("[SEND]:" + sendingText + "\n");
				byte[] data = sendingText.getBytes();
				GetUnderLayer().Send(data, data.length);
			}
		}
	}

	public boolean Receive(byte[] input) {
		System.out.println("receive_dlg");
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < input.length; i++) {
			if (input[i] < 0) { // 한글이면 3바이트
				byte[] temp = new byte[3];
				temp[0] = input[i];
				temp[1] = input[++i];
				temp[2] = input[++i];
				buf.append(new String(temp));
			} else { // 기타 영문, 문자, 기호이면 1바이트
				buf.append((char) input[i]);
			}
		}
		ChattingArea.append("[RECV]:" + buf + "\n");
		return true;
	}
	
	///////////////////////////////////////////
	private byte[] getMacAddress() {
		byte[] mac = null;
		try {
			NILayer nil = (NILayer) m_LayerMgr.GetLayer("NI");
			int index = NIComboBox.getSelectedIndex();
			mac = nil.m_pAdapterList.get(index).getHardwareAddress();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mac;
	}
	
	private String getStringFromByteMacAddress(byte[] mac) {
		mac = getMacAddress();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < mac.length; i++) {
			sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));		
		}
		return sb.toString();
	}
	
	private byte TokenToByte(String token) {
		byte result = 0x00;
		char c;
		for (int i = 0; i < 2; i++) {
			c = token.charAt(i);
			if (c >= '0' && c <= '9')
				result += c - '0';
			else if (c >= 'A' && c <= 'F')
				result += c - 'A' + 0xA;
			result = (byte) ((i == 0) ? (result << 4) : result);
		}
		
		return result;
	}
	
	private byte[] getByteFromStringMacAddress(String mac) {
		byte[] result = new byte[6];
		try {
			StringTokenizer st = new StringTokenizer(mac, "-");
			// AA-BB-CC-DD-EE-FF
			int i = 0;
			String token;
			while (st.hasMoreTokens()) {
				token = st.nextToken();
				result[i++] = TokenToByte(token);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	///////////////////////////////////////////

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
		// nUpperLayerCount++;
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
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);

	}

}
