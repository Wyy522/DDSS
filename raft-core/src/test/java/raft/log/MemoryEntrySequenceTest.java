package raft.log;

import org.junit.Assert;
import org.junit.Test;
import raft.core.log.entry.Entry;
import raft.core.log.entry.EntryMeta;
import raft.core.log.entry.NoOpEntry;
import raft.core.log.sequence.memory.MemoryEntrySequence;

import java.util.Arrays;
import java.util.List;

public class MemoryEntrySequenceTest {

    @Test
    public void testAppendEntry(){
        MemoryEntrySequence sequence =new MemoryEntrySequence();
        sequence.append(new NoOpEntry(sequence.getNextLogIndex(),1));
        Assert.assertEquals(2,sequence.getNextLogIndex());
        Assert.assertEquals(1,sequence.getLastLogIndex());
    }

    //随机访问日志条目
    @Test
    public void testGetEntry(){

        MemoryEntrySequence sequence =new MemoryEntrySequence(2);
        sequence.append(Arrays.asList(new NoOpEntry(2,1),new NoOpEntry(3,1)));
        Assert.assertNull(sequence.getEntry(1));
        Assert.assertEquals(2,sequence.getEntry(2).getIndex());
        Assert.assertEquals(3,sequence.getEntry(3).getIndex());
        Assert.assertNull(sequence.getEntry(4));
    }

    //随机访问日志条目元信息
    @Test
    public void testGetEntryMeta(){
        MemoryEntrySequence sequence =new MemoryEntrySequence(2);
        Assert.assertNull(sequence.getEntry(2));
        sequence.append(new NoOpEntry(2,1));
        EntryMeta meta = sequence.getEntryMeta(2);
        Assert.assertNotNull(meta);
        Assert.assertEquals(2,meta.getIndex());
        Assert.assertEquals(1,meta.getTerm());
    }

    //子序列操作
    @Test
    public void testSubListOneElement(){
        MemoryEntrySequence sequence =new MemoryEntrySequence(2);
        sequence.append(Arrays.asList(
                new NoOpEntry(2,1),
                new NoOpEntry(3,1)
        ));

        List<Entry> subList = sequence.subList(2, 3);
        Assert.assertEquals(1,subList.size());
        Assert.assertEquals(1,subList.get(0).getTerm());
    }

    //移除操作
    @Test
    public void testRemoveAfterPartial(){
        MemoryEntrySequence sequence =new MemoryEntrySequence(2);
        sequence.append(Arrays.asList(
                new NoOpEntry(2,1),
                new NoOpEntry(3,1)
        ));
        sequence.removeAfter(2);
        Assert.assertEquals(2,sequence.getLastLogIndex());
        Assert.assertEquals(3,sequence.getNextLogIndex());
    }
}
