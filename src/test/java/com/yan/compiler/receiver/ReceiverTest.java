/**
 * 
 */
package com.yan.compiler.receiver;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.yan.compiler.config.Config;

/**
 * @author code62
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PackageFactory.class)
public class ReceiverTest {

	public static Config config;

	public Receiver receiver;

	public MsgQueue queue;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		config = Config.factory(true);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		receiver = Receiver.factory();
		queue = MsgQueue.factory();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		receiver.stop();
	}

	/**
	 * Test method for {@link com.yan.compiler.receiver.Receiver#start()}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testStartListener() throws IOException {
		// Using Mockito
		final String textMessage = "Hello World!!!";
		final DatagramSocket mockSocket = Mockito.mock(DatagramSocket.class);
		receiver.initListener(mockSocket);

		// Mockito.when(mockSocket.receive(receivePacket))
		Mockito.doAnswer(new Answer<DatagramPacket>() {
			public DatagramPacket answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				DatagramPacket dp = (DatagramPacket) args[0];
				dp.setData(textMessage.getBytes());
				return null;
			}
		}).when(mockSocket).receive(Mockito.any(DatagramPacket.class));

		receiver.startListener();

		String reply = queue.getMsg();
		assertEquals(textMessage, reply);
	}

	/**
	 * Test method for {@link com.yan.compiler.receiver.Receiver#start()}.
	 */
	@Test
	public void testStartDeliver() {
//		String msg = "hello World!!!";
//		queue.addMsg(msg);
//
//		final FileBasePackage pkg = Mockito.mock(FileBasePackage.class);
//		Mockito.when(pkg.getTaskId()).thenReturn(990);
//		Mockito.doCallRealMethod().when(pkg).chkPackage();
//
//		PowerMockito.mockStatic(PackageFactory.class);
//		Mockito.when(PackageFactory.factory(msg)).thenReturn(pkg);
//
//		receiver.initDeliver();
//		receiver.startDeliver();
//
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		Mockito.verify(pkg).deliver();
	}

}
