package bgu.spl.net.srv.StompSrv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.api.StompMessagingProtocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;
import bgu.spl.net.srv.*;


public abstract class StompBaseServer implements Server<String> {

    private final int port;
    private final Supplier<StompMessagingProtocol<String>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<String>> encdecFactory;
    private ServerSocket sock;
    private UniqueIDGenerator conIdGen; //Object that handles generating unique ids from the free pool.
    private Connections<String> connections; //Connections object to handle connection management.

    public StompBaseServer(
            int port,
            Supplier<StompMessagingProtocol<String>> protocolFactory,
            Supplier<MessageEncoderDecoder<String>> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;
        this.conIdGen = new UniqueIDGenerator();
        this.connections = new ConnectionsManager<>();
    }

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();

                StompBlockingConnectionHandler handler = new StompBlockingConnectionHandler(
                        clientSock,
                        encdecFactory.get(),
                        protocolFactory.get(),
                        connections,
                        conIdGen.getNextId());

                        
                
                execute(handler);
            }
        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    protected abstract void execute(StompBlockingConnectionHandler handler);

}
