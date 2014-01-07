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

import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;
import reactor.core.Environment;
import reactor.event.dispatch.RingBufferDispatcher;
import reactor.function.Consumer;
import reactor.io.encoding.StandardCodecs;
import reactor.tcp.TcpConnection;
import reactor.tcp.TcpServer;
import reactor.tcp.spec.TcpServerSpec;

import java.util.concurrent.CountDownLatch;

/**
 * @author Stephane Maldini
 */
public class TestServer {

	private static final int NUMBER_OF_REPLIES = 2000;

	private final TcpServer<String, String> server;
	private final CountDownLatch latch = new CountDownLatch(NUMBER_OF_REPLIES);

	public TestServer(Environment env) throws Exception {

		Consumer<TcpConnection<String, String>> serverConsumer = new Consumer<TcpConnection<String, String>>() {

			@Override
			public void accept(final TcpConnection<String, String> connection) {

				connection.in().consume(new Consumer<String>() {

					@Override
					public void accept(String data) {
						System.out.println("Received data from client -> " + data);

						//send data
						for (int i = 0; i < NUMBER_OF_REPLIES; i++) {
							final String msg = "msg-" + i;

							connection.send(msg).onSuccess(new Consumer<Void>() {

								@Override
								public void accept(Void data) {
									System.out.println("sent:" + msg);
									latch.countDown();
								}

							}).onError(new Consumer<Throwable>() {

								@Override
								public void accept(Throwable t) {
									t.printStackTrace();
								}
							});
						}
					}
				});
			}
		};

		// server
		TcpServer<String, String> server = new TcpServerSpec<String, String>(NettyTcpServer.class)
				.env(env)
				.dispatcher(new RingBufferDispatcher("test", 1024, ProducerType.SINGLE, new YieldingWaitStrategy()))
				.listen("localhost", 15151)
				.codec(StandardCodecs.STRING_CODEC)
				.consume(serverConsumer).get();


		server.start();
		this.server = server;
	}

	public TcpServer<String, String> server() {
		return server;
	}

	public CountDownLatch latch() {
		return latch;
	}
}