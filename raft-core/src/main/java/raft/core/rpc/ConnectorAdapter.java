package raft.core.rpc;



import raft.core.node.NodeEndpoint;
import raft.core.rpc.message.*;

import javax.annotation.Nonnull;
import java.util.Collection;

public abstract class ConnectorAdapter implements Connector {

    @Override
    public void initialize() {
    }

//    @Override
//    public void sendPreVote(@Nonnull PreVoteRpc rpc, @Nonnull Collection<NodeEndpoint> destinationEndpoints) {
//    }
//
//    @Override
//    public void replyPreVote(@Nonnull PreVoteResult result, @Nonnull PreVoteRpcMessage rpcMessage) {
//    }

    @Override
    public void sendRequestVote( RequestVoteRpc rpc,  Collection<NodeEndpoint> destinationEndpoints) {
    }

    @Override
    public void replyRequestVote( RequestVoteResult result,  RequestVoteRpcMessage rpcMessage) {
    }

    @Override
    public void sendAppendEntries( AppendEntriesRpc rpc,  NodeEndpoint destinationEndpoint) {
    }

    @Override
    public void replyAppendEntries( AppendEntriesResult result,  AppendEntriesRpcMessage rpcMessage) {
    }

//    @Override
//    public void sendInstallSnapshot(@Nonnull InstallSnapshotRpc rpc, @Nonnull NodeEndpoint destinationEndpoint) {
//    }
//
//    @Override
//    public void replyInstallSnapshot(@Nonnull InstallSnapshotResult result, @Nonnull InstallSnapshotRpcMessage rpcMessage) {
//    }

    @Override
    public void resetChannels() {
    }

    @Override
    public void close() {
    }

}
