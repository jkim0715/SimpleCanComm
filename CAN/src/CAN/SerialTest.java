package CAN;

// ENDPOINT 
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

//1. Connect
//2. Receive
//3. Send

public class SerialTest implements SerialPortEventListener {
	private BufferedInputStream bin;
	private InputStream in;
	private OutputStream out;
	private SerialPort serialPort;
	private CommPortIdentifier portIdentifier;
	private CommPort commPort;

	private static String id;
	private static String data;

	private static SerialTest st;

	private class SendThread implements Runnable {
		public SendThread() {
		}

		public void run() {
			while (true) {
				try {
					System.out.println("id : " + id);
					System.out.println("data : " + data);

					Thread.sleep(1000);

					if (data.equals("0000000000000001")) {
						st.sendData("W28" + id + data);
					}

					else if (data.equals("0000000000000010")) {
						st.sendData("W28" + id + data);
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}// END SendThread

	public SerialTest() {
	}

	// 1-2 Income Port.
	public SerialTest(String portName) throws NoSuchPortException {
		portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		System.out.println("Connect Com Port!");
		try {
			id = "00000000";
			data = "0000000000000000";
			connectSerial();
			System.out.println("Connect OK !!");

			new Thread(new SerialWriter()).start();
			new Thread(new SendThread()).start();
		} catch (Exception e) {
			System.out.println("Connect Fail !!");
			e.printStackTrace();
		}
	} // END SerialTest

	private class SerialWriter implements Runnable {
		String data;

		public SerialWriter() {
			this.data = ":G11A9\r"; // Send Start Signal to CANBUS
		}

		public SerialWriter(String serialData) {
			// W28 00000000 000000000000 ���� �޽�����
			// :W28 00000000 000000000000 53 \r �̷��� ������ �ǰ� :������ \r�� ������ 53��
			// checksum
			String sdata = sendDataFormat(serialData);
//			System.out.println(sdata);
			this.data = sdata;
		}// Constructor

		public String sendDataFormat(String serialData) {
			serialData = serialData.toUpperCase();
			char c[] = serialData.toCharArray();
			int cdata = 0;
			for (char cc : c) {
				cdata += cc;
			}
			cdata = (cdata & 0xFF);
			String returnData = ":";
			returnData += serialData + Integer.toHexString(cdata).toUpperCase();
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
	}// SerialWriter Thread END

	public void connectSerial() throws Exception {
		// Double check connection. ������ �Ǿ������� port�� �ٸ� ģ���� ���� ������.
		if (portIdentifier.isCurrentlyOwned()) {
			System.out.println("Error: Port is currently in use");
		} else {
			commPort = portIdentifier.open(this.getClass().getName(), 5000);
			if (commPort instanceof SerialPort) {
				serialPort = (SerialPort) commPort;
				serialPort.addEventListener(this);
				serialPort.notifyOnDataAvailable(true);
				serialPort.setSerialPortParams(921600, // ��żӵ�
						SerialPort.DATABITS_8, // ������ ��Ʈ
						SerialPort.STOPBITS_1, // stop ��Ʈ
						SerialPort.PARITY_NONE); // �и�Ƽ
				// Serial���ٰ� Stream�� ����.

				in = serialPort.getInputStream();
				bin = new BufferedInputStream(in);

				out = serialPort.getOutputStream(); // �� format�� ����� ���� �� ����.
			} else {
				System.out.println("Error: Only serial ports are handled by this example.");
			}
		}
	}// Connect Serial END

	public void sendData(String data) {
		SerialWriter sw = new SerialWriter(data);
		new Thread(sw).start();
	}// END sendData

	public static void main(String[] args) {
		try {
			// 1-1. Create Connection
			st = new SerialTest("COM7");
		} catch (NoSuchPortException e) {
			e.printStackTrace();
		}

	}// END Main

	// Implemented Method... Event �߻��� ����. Data�� ���Ƶ��̴� �κ�.
	@Override
	public void serialEvent(SerialPortEvent event) {
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

				if (ss.charAt(1) == 'U') {
					id = ss.substring(4, 12);
					data = ss.substring(12, 28);

					// ss�� �߸����� ��� ����. checksum�����ͷ� ��
					boolean result = checkSerialData(ss.trim());
					System.out.println("Receive Low Data:" + ss + "||");
					System.out.println("Result:" + result);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
	}// END SerialEvent

	public boolean checkSerialData(String data) {
		boolean check = false;
		// :U28 00 00 00 50 0000 0000 0000 0020 46
		System.out.println("data size : " + data.length());
		System.out.println(data);
		String checkData = data.substring(1, 28);
		String checkSum = data.substring(28, 30);
		System.out.println("checkdata:" + checkData);
		System.out.println("checksum:" + checkSum);
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
	}// END checkSerialData

}