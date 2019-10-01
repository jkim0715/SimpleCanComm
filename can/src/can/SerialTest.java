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
	private OutputStream out; // 받는쪽은 자바가 아닐 수 있음

	public SerialTest() {

	}

	public SerialTest(String portName) throws NoSuchPortException {
		portIdentifier = CommPortIdentifier.getPortIdentifier(portName); // port가 살아있는지 확인
		System.out.println("Connect Com Port!"); // comX가 정상이면
		try { // connection
			connectSerial();
			System.out.println("Connect OK !!");
			(new Thread(new SerialWriter())).start(); // Serial에 write하겠다
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

		public SerialWriter(String serialData) { // sendData에서 여기로 옴
			// W28 00000000 000000000000 //  ID+DATA
			// :W28 00000000 000000000000 53 \r 
			// 이 구조로 보내야함, 53:checksum, \r이 끝났다는 표시
			String sdata = sendDataFormat(serialData); // 구조 바꾸는 곳
			System.out.println(sdata);
			this.data = sdata; // run으로
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
			cdata = (cdata & 0xFF); // checksum을 만드는 &연산

			String returnData = ":";
			returnData += serialData + Integer.toHexString(cdata).toUpperCase();
			// checksum을 Hex로 바꿔서(53)
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

		if (portIdentifier.isCurrentlyOwned()) { // 문제가 있는지 확인, 다른 곳에서 쓰고있으면
			System.out.println("Error: Port is currently in use"); // 문제있으면 하지마
		} else {
			commPort = portIdentifier.open(this.getClass().getName(), 5000);
			if (commPort instanceof SerialPort) { // comPort가 SerialPort이면
				serialPort = (SerialPort) commPort;
				serialPort.addEventListener(this);
				// application <-> Serial <-> CAN / RealTime은 아님
				// app에서 event를 발생시키면 그때 serialEvent함수가 동작해서 데이터를 inputStream으로 받기위함
				serialPort.notifyOnDataAvailable(true);
				serialPort.setSerialPortParams(921600, // 통신속도
						SerialPort.DATABITS_8, // 데이터 비트
						SerialPort.STOPBITS_1, // stop 비트
						SerialPort.PARITY_NONE); // 패리티
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
			st.sendData("W2800000008000000000000000A"); // 데이터 보내기
		} catch (NoSuchPortException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		// data 받는 곳
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
				System.out.println("Result: " + result); // checkSum True Or False 확인
				System.out.println("Receive Low Data:" + ss + "||");
				// 받은 데이터 뿌리기

			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
	}

	public boolean checkSerialData(String data) {
		// CheckSum 맞는지 확인
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
