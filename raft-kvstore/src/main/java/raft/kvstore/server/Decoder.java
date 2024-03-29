package raft.kvstore.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import raft.kvstore.MessageConstants;
import raft.kvstore.Protos;
import raft.kvstore.message.*;

import java.util.List;

public class Decoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //等待数据
        if (in.readableBytes()<8){
            return;
        }
        in.markReaderIndex();
        int messageType = in.readInt();
        int payloadLength = in.readInt();
        if (in.readableBytes() < payloadLength) {
            in.resetReaderIndex();
            return;
        }
        //单条数据就绪
        byte[] payload = new byte[payloadLength];
        in.readBytes(payload);
        switch (messageType) {
            case MessageConstants.MSG_TYPE_SUCCESS:
                // 成功
                out.add(Success.INSTANCE);
                break;
            case MessageConstants.MSG_TYPE_FAILURE:
                // 失败
                Protos.Failure protoFailure = Protos.Failure.parseFrom(payload);
                out.add(new Failure(protoFailure.getErrorCode(), protoFailure.getMessage()));

                break;
            case MessageConstants.MSG_TYPE_REDIRECT:
                // 重定向
                Protos.Redirect protoRedirect = Protos.Redirect.parseFrom(payload);
                out.add(new Redirect(protoRedirect.getLeaderId()));
                System.out.println("已重定向置Leader节点");
                break;
//            case MessageConstants.MSG_TYPE_ADD_SERVER_COMMAND:
//                Protos.AddNodeCommand protoAddServerCommand = Protos.AddNodeCommand.parseFrom(payload);
//                out.add(new AddNodeCommand(protoAddServerCommand.getNodeId(), protoAddServerCommand.getHost(), protoAddServerCommand.getPort()));
//                break;
//            case MessageConstants.MSG_TYPE_REMOVE_SERVER_COMMAND:
//                Protos.RemoveNodeCommand protoRemoveServerCommand = Protos.RemoveNodeCommand.parseFrom(payload);
//                out.add(new RemoveNodeCommand(protoRemoveServerCommand.getNodeId()));
//                break;
            case MessageConstants.MSG_TYPE_GET_COMMAND:
                // get命令
                Protos.GetCommand protoGetCommand = Protos.GetCommand.parseFrom(payload);
                out.add(new GetCommand(protoGetCommand.getKey()));
                break;
            case MessageConstants.MSG_TYPE_GET_COMMAND_RESPONSE:
                // get响应
                Protos.GetCommandResponse protoGetCommandResponse = Protos.GetCommandResponse.parseFrom(payload);
                out.add(new GetCommandResponse(protoGetCommandResponse.getFound(), protoGetCommandResponse.getValue().toByteArray()));
                break;
            // set命令
            case MessageConstants.MSG_TYPE_SET_COMMAND:
                Protos.SetCommand protoSetCommand = Protos.SetCommand.parseFrom(payload);
                out.add(new SetCommand(protoSetCommand.getKey(), protoSetCommand.getValue().toByteArray()));
                break;
            default:
                throw new IllegalStateException("unexpected message type " + messageType);
        }
    }
}
