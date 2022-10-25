package raft.core.rpc;

import raft.core.node.NodeEndpoint;
import raft.core.node.NodeId;
import raft.core.rpc.message.AppendEntriesResult;
import raft.core.rpc.message.AppendEntriesRpc;
import raft.core.rpc.message.RequestVoteResult;
import raft.core.rpc.message.RequestVoteRpc;

import java.util.*;

/**
 * 测试模拟Rpc请求
 * @author yiyewei
 * @create 2022/9/23 10:26
 **/
public class MockConnector extends ConnectorAdapter {

    //存放信息的链表
    private LinkedList<Message> messages=new LinkedList<>();

    @Override
    public void initialize() {

    }

    //发送投票请求Rpc给多个节点,群发,所以是个集合
    @Override
    public void sendRequestVote(RequestVoteRpc rpc, Collection<NodeEndpoint> destinationEndpoints) {

        Message m = new Message();
        m.rpc=rpc;
        messages.add(m);

    }

    //回复投票请求Rpc的结果给单个节点
//    @Override
    public void replyRequestVote(RequestVoteResult result, NodeEndpoint destinationEndpoint) {

        Message m = new Message();
        m.result=result;
        m.destinationNodeId=destinationEndpoint.getId();
        messages.add(m);

    }

    //发送日志追加请求给单个节点
    @Override
    public void sendAppendEntries(AppendEntriesRpc rpc, NodeEndpoint destinationEndpoint) {

        Message m = new Message();
        m.rpc=rpc;
        m.destinationNodeId=destinationEndpoint.getId();
        messages.add(m);
    }

    //回复日志追加请求结构给单个节点
//    @Override
    public void replyAppendEntries(AppendEntriesResult result, NodeEndpoint destinationEndpoint) {

        Message m = new Message();
        m.result=result;
        m.destinationNodeId=destinationEndpoint.getId();
        messages.add(m);

    }

    @Override
    public void close() {

    }

    //获取最后一条消息
    public Message getLastMessage(){
        return messages.isEmpty()?null:messages.getLast();
    }

    //获取最后一条消息或者空消息
    public Message getLastMessageOrDefault(){
        return messages.isEmpty()?new Message():messages.getLast();
    }

    //获取最后一条信息的rpc消息
    public Object getRpc(){
        return getLastMessageOrDefault().rpc;
    }

    //获取最后一条信息的rpc消息
    public Object getResult(){
        return getLastMessageOrDefault().result;
    }

    //获取最后一条信息的目标节点Id
    public NodeId getDestinationNodeId(){
        return getLastMessageOrDefault().destinationNodeId;
    }

    //获取消息数量
    public int getMessageCount(){
        return messages.size();
    }

    //获取所有消息
    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    //清空消息
    public void clearMessage(){
        messages.clear();
    }

    //封装可rpc请求和响应
    public static class Message{
        private Object rpc;
        private NodeId destinationNodeId;
        private Object result;

        public Object getRpc() {
            return rpc;
        }

        public NodeId getDestinationNodeId() {
            return destinationNodeId;
        }

        public Object getResult() {
            return result;
        }

        @Override
        public String toString() {
            return "Message{" +
                    "rpc=" + rpc +
                    ", destinationNodeId=" + destinationNodeId +
                    ", result=" + result +
                    '}';
        }
    }
}
