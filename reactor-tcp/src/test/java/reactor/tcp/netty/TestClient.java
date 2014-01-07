package reactor.tcp.netty;

/**
 * @author Stephane Maldini
 */
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;
import reactor.core.Environment;
import reactor.event.dispatch.RingBufferDispatcher;
import reactor.function.Consumer;
import reactor.io.encoding.StandardCodecs;
import reactor.tcp.TcpClient;
import reactor.tcp.TcpConnection;
import reactor.tcp.spec.TcpClientSpec;

public class TestClient {

	private final TcpClient<String, String> client;

	public TestClient(Environment env) throws Exception {
		TcpClient<String, String> client = new TcpClientSpec<String, String>(NettyTcpClient.class)
				.env(env)
				.dispatcher(new RingBufferDispatcher("test", 1024, ProducerType.SINGLE, new YieldingWaitStrategy()))
				.codec(StandardCodecs.STRING_CODEC)
				.connect("localhost", 15151)
				.get();

		TcpConnection<String, String> connection = client.open().await();

		connection.consume(new Consumer<String>() {

			@Override
			public void accept(String t) {
				System.out.println("received: " + t);
			}
		});

		connection.send("data-from-client").onSuccess(new Consumer<Void>() {

			@Override
			public void accept(Void data) {
				System.out.println("data sent");
			}

		}).onError(new Consumer<Throwable>() {

			@Override
			public void accept(Throwable t) {
				t.printStackTrace();
			}
		});

		this.client = client;
	}

	public TcpClient<String, String> client() {
		return client;
	}
}