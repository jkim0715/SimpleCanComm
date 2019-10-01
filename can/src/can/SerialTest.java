package can;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

public class SerialTest implements SerialPortEventListener {

	private SerialPort serialPort;
	private CommPortIdentifier portIdentifier;
	private CommPort commPort;

	private BufferedInputStream bin;
	private InputStream in;
	private OutputStream out; // �޴����� �ڹٰ� �ƴ� �� ����

	public SerialTest() {

	}

	public SerialTest(String portName) throws NoSuchPortException {
		portIdentifier = CommPortIdentifier.getPortIdentifier(portName); // port�� ����ִ��� Ȯ��
		System.out.println("Connect Com Port!"); // comX�� �����̸�
		try { // connection
			connectSerial();
			System.out.println("Connect OK !!");
			(new Thread(new SerialWriter())).start(); // Serial�� write�ϰڴ�
		} catch (Exception e) {
			System.out.println("Connect Fail !!");
			e.printStackTrace();
		}
	}

	
	private class SerialWriter implements Runnable { // Thread
		String data;

		public SerialWriter() {
			this.data = ":G11A9\r";
		}

		public SerialWriter(String serialData) { // sendData���� ����� ��
			// W28 00000000 000000000000 //  ID+DATA
			// :W28 00000000 000000000000 53 \r 
			// �� ������ ��������, 53:checksum, \r�� �����ٴ� ǥ��
			String sdata = sendDataFormat(serialData); // ���� �ٲٴ� ��
			System.out.println(sdata);
			this.data = sdata; // run����
		}

		public String sendDataFormat(String serialData) {
			serialData = serialData.toUpperCase();
//			serialData = serialData.substring(4, serialData.length());
			System.out.println("serialData : " + serialData);
			char c[] = serialData.toCharArray();
			int cdata = 0;
			for (char cc : c) {
				cdata += cc;
			}
			cdata = (cdata & 0xFF); // checksum�� ����� &����

			String returnData = ":";
			returnData += serialData + Integer.toHexString(cdata).toUpperCase();
			// checksum�� Hex�� �ٲ㼭(53)
			returnData += "\r";
			return returnData;
		}

		public void run() {
			try {
				byte[] inputData = data.getBytes();
				out.write(inputData);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void connectSerial() throws Exception {

		if (portIdentifier.isCurrentlyOwned()) { // ������ �ִ��� Ȯ��, �ٸ� ������ ����������
			System.out.println("Error: Port is currently in use"); // ���������� ������
		} else {
			commPort = portIdentifier.open(this.getClass().getName(), 5000);
			if (commPort instanceof SerialPort) { // comPort�� SerialPort�̸�
				serialPort = (SerialPort) commPort;
				serialPort.addEventListener(this);
				// application <-> Serial <-> CAN / RealTime�� �ƴ�
				// app���� event�� �߻���Ű�� �׶� serialEvent�Լ��� �����ؼ� �����͸� inputStream���� �ޱ�����
				serialPort.notifyOnDataAvailable(true);
				serialPort.setSerialPortParams(921600, // ��żӵ�
						SerialPort.DATABITS_8, // ������ ��Ʈ
						SerialPort.STOPBITS_1, // stop ��Ʈ
						SerialPort.PARITY_NONE); // �и�Ƽ
				in = serialPort.getInputStream();
				bin = new BufferedInputStream(in);
				out = serialPort.getOutputStream();
			} else {
				System.out.println("Error: Only serial ports are handled by this example.");
			}
		}
	}

	public void sendData(String data) {
		SerialWriter sw = new SerialWriter(data);
		new Thread(sw).start();
	}
	
	public static void main(String[] args) {
		try {
			SerialTest st = new SerialTest("COM7");
			st.sendData("W2800000008000000000000000A"); // ������ ������
		} catch (NoSuchPortException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		// data �޴� ��
		switch (event.getEventType()) {
		case SerialPortEvent.BI:
		case SerialPortEvent.OE:
		case SerialPortEvent.FE:
		case SerialPortEvent.PE:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.RI:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			byte[] readBuffer = new byte[128];

			try {

				while (bin.available() > 0) {
					int numBytes = bin.read(readBuffer);
				}

				String ss = new String(readBuffer);
				boolean result = checkSerialData(ss);
				System.out.println("Result: " + result); // checkSum True Or False Ȯ��
				System.out.println("Receive Low Data:" + ss + "||");
				// ���� ������ �Ѹ���

			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
	}

	public boolean checkSerialData(String data) {
		// CheckSum �´��� Ȯ��
		boolean check = false;
		// :W28 0000 0008 0000 0000 0000 000A 
		String checkData = data.substring(1, 28);
		System.out.println("checkData: "+checkData);
		String checkSum = data.substring(28, 30);
		System.out.println("checkSum: "+checkSum);

		char c[] = checkData.toCharArray();
		int cdata = 0;
		for (char cc : c) {
			cdata += cc;
		}
		cdata = (cdata & 0xFF);
		String serialCheckSum = Integer.toHexString(cdata).toUpperCase();
		if (serialCheckSum.trim().equals(checkSum)) {
			check = true;
		}
		return check;
	}

}
