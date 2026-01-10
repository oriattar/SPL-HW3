package bgu.spl.net.srv.StompSrv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class StompBlockingConnectionHandler implements Runnable, ConnectionHandler<String> {

        private final StompMessagingProtocol<String> protocol;
        private final MessageEncoderDecoder<String> encdec;
        private final Socket sock;
        private BufferedInputStream in;
        private BufferedOutputStream out;
        private volatile boolean connected = true;
        
        public StompBlockingConnectionHandler(Socket sock, MessageEncoderDecoder<String> reader, StompMessagingProtocol<String> protocol,Connections<String> connections,int conId)
        {
            this.sock = sock;
            this.encdec = reader;
            this.protocol = protocol;

            protocol.setConHandler(this); // set this connection handler in the protocol
            protocol.start(conId,connections);
        }



        @Override
        public void run() {
            try (Socket sock = this.sock) { //just for automatic closing
                int read;

                in = new BufferedInputStream(sock.getInputStream());
                out = new BufferedOutputStream(sock.getOutputStream());

                while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                    String nextMessage = encdec.decodeNextByte((byte) read);
                    if (nextMessage != null) {
                        protocol.process(nextMessage);
                    }
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }

        @Override
        public void close() throws IOException {
            connected = false;
            sock.close();
        }

        @Override
        public void send(String msg) {
            try{
                out.write(encdec.encode(msg));
                out.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }