/*
 * Copyright (c) 2011-2013 GoPivotal, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.tcp.netty;

import org.junit.Test;
import reactor.core.Environment;
import reactor.tcp.TcpClient;
import reactor.tcp.TcpServer;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;

/**
 * @author Stephane Maldini
 */
public class ClientServerTests {

	final private Environment environment = new Environment();

	@Test
	public void simpleRequestReplyWithNettyTcp() throws Exception {
		TcpServer server = null;
		TcpClient client = null;
		try{
			TestServer testServer = new TestServer(environment);
			server = testServer.server();
			client = new TestClient(environment).client();

			testServer.latch().await();

			assertEquals("All messages successfully replied from server", testServer.latch().getCount(), 0);

		} finally {
			if(client != null){
				client.close();
			}
			if(server != null){
				server.shutdown().await();
			}
		}
	}
}
