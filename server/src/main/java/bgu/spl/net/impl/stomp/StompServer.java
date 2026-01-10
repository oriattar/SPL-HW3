package bgu.spl.net.impl.stomp;

import java.util.function.Supplier;

import bgu.spl.net.srv.Server;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.impl.echo.EchoProtocol;
import bgu.spl.net.impl.echo.LineMessageEncoderDecoder;
import bgu.spl.net.impl.rci.ObjectEncoderDecoder;
import bgu.spl.net.srv.StompSrv.StompMsgEncDec;
import bgu.spl.net.srv.StompSrv.StompMsgProtocol;

public class StompServer {

    public static void main(String[] args) {
        
        Supplier<StompMessagingProtocol<String>> protocolSupplier =
        () -> new StompMsgProtocol();

        Supplier<MessageEncoderDecoder<String>> encdecSupplier =
        () -> new StompMsgEncDec();
        Server.StompThreadPerClient(7777, protocolSupplier, encdecSupplier
        ).serve();
    }
}
