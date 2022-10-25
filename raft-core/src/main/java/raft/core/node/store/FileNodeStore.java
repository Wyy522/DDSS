package raft.core.node.store;

import com.google.common.io.Files;
import raft.core.node.NodeId;
import raft.core.support.RandomAccessFileAdapter;
import raft.core.support.SeekableFile;

import java.io.File;
import java.io.IOException;

/**
 * 基于文件的状态存储
 *
 * @author yiyewei
 * @create 2022/9/20 23:57
 **/
public class FileNodeStore implements NodeStore {

    //文件名称
    public static final String FILE_NAME = "node.bin";

    //term偏移量
    private static final long OFFSET_TERM = 0;

    //votedFor偏移量
    private static final long OFFSET_VOTED_FOR = 4;

    //文件操作句柄
    private final SeekableFile seekableFile;

    private int term = 0;
    private NodeId votedFor = null;

    //从文件读取
    public FileNodeStore(File file) {
        try {
            //如果不存在就创建一个
            if (!file.exists()) {
                Files.touch(file);
            }

            //创建句柄
            seekableFile = new RandomAccessFileAdapter(file);

            initializeOrLoad();
        } catch (IOException e) {
            throw new NodeStoreException(e);
        }
    }

    //从模拟文件读取,用于测试
    public FileNodeStore(SeekableFile seekableFile) {
        this.seekableFile = seekableFile;
        try {
            initializeOrLoad();
        } catch (IOException e) {
            throw new NodeStoreException(e);
        }
    }

    //初始化或加载
    private void initializeOrLoad() throws IOException {

        //如果大小为空就初始化,不为空说明有数据则进行加载
        if (seekableFile.size() == 0) {
            //8字节空间
            seekableFile.truncate(8L);

            //设置偏移量为0
            seekableFile.seek(0);

            //写入4字节作为term
            seekableFile.writeInt(0);

            //写入4字节作为votedFor的length
            seekableFile.writeInt(0);
        } else {
            //读取4字节作为term
            term = seekableFile.readInt();

            //读取4字节作为votedFor的length
            int length = seekableFile.readInt();

            //如果不为空则将读取length长度字节封装成NodeId(内容是按字节存储)
            if (length > 0) {
                byte[] bytes = new byte[length];
                seekableFile.read(bytes);
                votedFor = new NodeId(new String(bytes));
            }
        }
    }

    @Override
    public int getTerm() {
        return term;
    }

    @Override
    public void setTerm(int term) {

        try {
            //设置term偏移量
            seekableFile.seek(OFFSET_TERM);

            //读取term
            seekableFile.writeInt(term);
        } catch (IOException e) {
            throw new NodeStoreException(e);
        }

        //赋值term
        this.term = term;
    }

    @Override
    public NodeId getVotedFor() {
        return votedFor;
    }

    @Override
    public void setVotedFor(NodeId votedFor) {

        try {
            //设置votedFor偏移量
            seekableFile.seek(OFFSET_VOTED_FOR);

            //如果不为空则将votedFor转换成bytes存储
            if (votedFor == null) {
                seekableFile.writeInt(0);
                seekableFile.truncate(8L);
            } else {
                byte[] bytes = votedFor.getValue().getBytes();
                seekableFile.writeInt(bytes.length);
                seekableFile.write(bytes);
            }
        } catch (IOException e) {
            throw new NodeStoreException(e);
        }

        //赋值votedFor
        this.votedFor = votedFor;
    }

    @Override
    public void close() {
        try {
            seekableFile.close();
        } catch (IOException e) {
            throw new NodeStoreException(e);
        }
    }
}
