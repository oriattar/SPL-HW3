package bgu.spl.net.srv.StompSrv;
import java.nio.ByteBuffer;

import bgu.spl.net.api.MessageEncoderDecoder;

public class StompMsgEncDec implements MessageEncoderDecoder<String> {

    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

    public String decodeNextByte(byte nextByte)
    {
        byteBuffer.put(nextByte);
        if(nextByte == 0 || !this.byteBuffer.hasRemaining())
        {
            String result = new String(byteBuffer.array());
            byteBuffer.clear();
            return result;
        }
        return null;
    }

    public byte[] encode(String message)
    {
        return message.getBytes();
    }
    
}
