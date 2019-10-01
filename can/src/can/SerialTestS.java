package can;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
//import can.SerialTestS.Receiver;
//import can.SerialTestS.Sender;

public class SerialTestS implements SerialPortEventListener {
	private SerialPort serialPort;
	private CommPortIdentifier portIdentifier;
	private CommPort commPort;
	
	private BufferedInputStream bin;
	private InputStream in;
	private OutputStream out;

	private Socket socket;
	boolean rflag = true;
	
	public SerialTestS() {
	}

	public SerialTestS(String portName, String ip, int port) throws Exception {
		setPort(portName);
		setIp(ip, port);
	}
	
	public void setPort(String portName) throws Exception {
		portIdentifier = CommPortIdentifier.getPortIdentifier(portName); // port가 살아있는지 확인
		System.out.println("Connect Com Port!"); // comX가 정상이면
		
		try { // connection
			connectSerial();
			System.out.println("Connect OK !!");
			new Thread(new SerialWriter()).start(); // Serial에 write하겠다
		} catch (Exception e) {
			System.out.println("Connect Fail !!");
			e.printStackTrace();
		}
	}
	
	public void setIp(String ip, int port) throws Exception {
		boolean flag = true;
		
		while (flag) {
			try {
				socket = new Socket(ip, port);
				if (socket != null && socket.isConnected()) {
					break;
				}
			} catch (Exception e) { // 서버가 안켜져있으면
				System.out.println("Re-Try");
				
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		} // End while
		
		System.out.println("Socket Connect OK");
		new Receiver(socket).start();// Pad -> IoT -> CAN -> LattePanda
	// Pad <- IoT <- CAN <- LattePanda
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	//--------Client----------------------
	public void sendMsg(String msg) throws IOException {
		Sender sender = null;
		
		if (socket != null && !socket.isClosed()) {
			sender = new Sender(socket);
			sender.setMsg(msg);
			sender.start();
		}
		
		else {
			System.out.println("Socket is closed already...");
		}
	}
	
	public void start() throws Exception {
		Scanner sc = new Scanner(System.in);
		boolean sflag = true;
		while (sflag) {
			System.out.println("Input Msg.");
			String str = sc.next();
			sendMsg(str);
			if(str.equals("q")) {
				rflag = false;
				break;
			}
		}
		sc.close();
	}
	
	class Sender extends Thread {
//		Socket socket;
		OutputStream out;
		DataOutputStream dout;
		String msg;

		public Sender(Socket socket) throws IOException {
			out = socket.getOutputStream();
			dout = new DataOutputStream(out);
		}
		
		public void setMsg(String msg) {
			this.msg = msg;
		}
		
		public void run() {
			if(dout != null) {
				try {
					dout.writeUTF(msg);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	class Receiver extends Thread { // Pad -> IoT -> CAN 
		Socket socket;
		InputStream in;
		DataInputStream din;

		public Receiver(Socket socket) throws IOException {
			this.socket = socket;
			in = socket.getInputStream();
			din = new DataInputStream(in);
		}
		
		public void run() {
			try {
				while (rflag) {
					String str = din.readUTF();
					System.out.println(str);
					if(str.equals("Test : android is send 2")) {
						sendData("W28000000000000000000000001");
					} else if(str.equals("Test : android is send 3")){
						sendData("W28000000000000000000000010");
					}
					
//					if(str.equals("1th send")) {
//						st.sendData("W28000000000000000000000001");
//					} else if(str.equals("2th send")){
//						st.sendData("W28000000000000000000000010");
//					} else if(str.equals("3th send")){
//						st.sendData("W280000000100000000ABCDABCD");
//					}
				}
			} catch (Exception e) {
			}
		}
	}
	//--------Client----------------------
	
	
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
				
				System.out.println("Result:" + result);
				System.out.println("Receive Low Data:" + ss + "||");
				
				if(ss.substring(1, 2).equals("U")) { // IoT <- CAN
					System.out.println("Send to server");
					sendMsg(ss);
				}else if(ss.substring(1, 2).equals("W")) { // IoT -> CAN
					System.out.println("Send Low Data:" + ss + "||");
				}
			} catch (Exception e) {
				e.printStackTrace();
				
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
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
