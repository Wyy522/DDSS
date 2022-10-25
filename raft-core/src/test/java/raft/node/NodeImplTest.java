package raft.node;

import org.junit.Assert;
import org.junit.Test;
import raft.core.node.*;
import raft.core.node.role.CandidateNodeRole;
import raft.core.node.role.FollowerNodeRole;
import raft.core.node.role.LeaderNodeRole;
import raft.core.rpc.MockConnector;
import raft.core.rpc.message.*;
import raft.core.scheduler.NullScheduler;
import raft.core.support.DirectTaskExecutor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NodeImplTest {

    private NodeBuilder newNodeBuilder(NodeId selfId, NodeEndpoint... endpoints){
        return new NodeBuilder(Arrays.asList(endpoints),selfId)
                .setScheduler(new NullScheduler())
                .setConnector(new MockConnector())
                .setTaskExecutor(new DirectTaskExecutor());
    }

    @Test
    public void testStart(){
        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"), new NodeEndpoint("A", "localhost",2333)).build();
        node.start();
        FollowerNodeRole role =(FollowerNodeRole) node.getRole();
        Assert.assertEquals(0,role.getTerm());
        Assert.assertNull(role.getVotedFor());
    }

    @Test
    public void testElectionTimeoutWhenFollowers(){
        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A", "localhost",2333),
                new NodeEndpoint("B", "localhost",2334),
                new NodeEndpoint("C", "localhost",2335)
                ).build();
        node.start();
        node.electionTimout();
        CandidateNodeRole role =(CandidateNodeRole) node.getRole();
        Assert.assertEquals(1,role.getTerm());
        Assert.assertEquals(1,role.getVotesCount());
        MockConnector mockConnector = (MockConnector) node.getContext().connector();
        RequestVoteRpc rpc = (RequestVoteRpc) mockConnector.getRpc();
        Assert.assertEquals(1,rpc.getTerm());
        Assert.assertEquals(NodeId.of("A"),rpc.getCandidateId());
        Assert.assertEquals(0,rpc.getLastLogIndex());
        Assert.assertEquals(0,rpc.getLastLogTerm());
    }

    @Test
    public void testOnReceiveRequestVoteRpcFollower(){
        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A", "localhost",2333),
                new NodeEndpoint("B", "localhost",2334),
                new NodeEndpoint("C", "localhost",2335)
        ).build();
        node.start();
        RequestVoteRpc rpc = new RequestVoteRpc();
        rpc.setTerm(1);
        rpc.setCandidateId(NodeId.of("C"));
        rpc.setLastLogIndex(0);
        rpc.setLastLogTerm(0);
        node.onReceiveRequestVoteRpc(new RequestVoteRpcMessage(rpc,NodeId.of("C"),null));
        MockConnector mockConnector = (MockConnector) node.getContext().connector();
        RequestVoteResult result = (RequestVoteResult) mockConnector.getResult();
        System.out.println(mockConnector.getMessages().toString());
//        Assert.assertEquals(1,result.getTerm());
//        Assert.assertTrue(result.isVoteGranted());
//        Assert.assertEquals(NodeId.of("C"), ((FollowerNodeRole) node.getRole()).getVotedFor());
    }

    @Test
    public void testOnReceiveRequestVoteResult(){
        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A", "localhost",2333),
                new NodeEndpoint("B", "localhost",2334),
                new NodeEndpoint("C", "localhost",2335)
        ).build();
        node.start();
        node.electionTimout();
        node.onReceiveRequestVoteResult(new RequestVoteResult(1,true));
        LeaderNodeRole role = (LeaderNodeRole) node.getRole();
        Assert.assertEquals(1,role.getTerm());
    }

    @Test
    public void testReplicateLog(){
        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A", "localhost",2333),
                new NodeEndpoint("B", "localhost",2334),
                new NodeEndpoint("C", "localhost",2335)
        ).build();
        node.start();
        node.electionTimout();
        node.onReceiveRequestVoteResult(new RequestVoteResult(1,true));
        node.replicateLog();
        MockConnector mockConnector = (MockConnector) node.getContext().connector();
        Assert.assertEquals(3,mockConnector.getMessageCount());
        List<MockConnector.Message> messages = mockConnector.getMessages();
        Set<NodeId> destinationNodeIds = messages.subList(1, 3).stream()
                .map(MockConnector.Message::getDestinationNodeId)
                .collect(Collectors.toSet());
        System.out.println(destinationNodeIds);
        Assert.assertEquals(2,destinationNodeIds.size());
        Assert.assertTrue(destinationNodeIds.contains(NodeId.of("B")));
        Assert.assertTrue(destinationNodeIds.contains(NodeId.of("C")));
        AppendEntriesRpc rpc = (AppendEntriesRpc) messages.get(2).getRpc();
        System.out.println(messages.get(2).toString());
        Assert.assertEquals(1,rpc.getTerm());

    }

    @Test
    public void testOnReceiveAppendEntriesRpcFollower(){
        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A", "localhost",2333),
                new NodeEndpoint("B", "localhost",2334),
                new NodeEndpoint("C", "localhost",2335)
        ).build();
        node.start();
        AppendEntriesRpc rpc = new AppendEntriesRpc();
        rpc.setTerm(1);
        rpc.setLeaderId(NodeId.of("B"));
        node.onReceiveAppendEntriesRpc(new AppendEntriesRpcMessage(rpc,NodeId.of("B"),null));
        MockConnector connector = (MockConnector) node.getContext().getConnector();
        AppendEntriesResult result = (AppendEntriesResult) connector.getResult();
        Assert.assertEquals(1,result.getTerm());
        Assert.assertTrue(result.isSuccess());
        FollowerNodeRole role = (FollowerNodeRole) node.getRole();
        Assert.assertEquals(1,role.getTerm());
        Assert.assertEquals(NodeId.of("B"),role.getLeaderId());
    }

    @Test
    public void testOnReceiveAppendEntriesNormal(){
//        NodeImpl node = (NodeImpl) newNodeBuilder(
//                NodeId.of("A"),
//                new NodeEndpoint("A", "localhost",2333),
//                new NodeEndpoint("B", "localhost",2334),
//                new NodeEndpoint("C", "localhost",2335)
//        ).build();
//        node.start();
//        node.electionTimout();
//        node.onReceiveRequestVoteResult(new RequestVoteResult(1,true));
//        node.replicateLog();
//        node.onReceiveAppendEntriesResult(new AppendEntriesResultMessage(
//                new AppendEntriesResult(1,true),
//                NodeId.of("B"),
//                new AppendEntriesRpc()
//        ));
    }
}
