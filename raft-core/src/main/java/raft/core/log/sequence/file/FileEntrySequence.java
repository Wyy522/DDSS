package raft.core.log.sequence.file;

import raft.core.log.LogDir;
import raft.core.log.execption.LogException;
import raft.core.log.entry.Entry;
import raft.core.log.entry.EntryFactory;
import raft.core.log.entry.EntryMeta;
import raft.core.log.sequence.AbstractEntrySequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 基于文件的日志序列
 * @author yiyewei
 * @create 2022/9/26 22:23
 **/
public class FileEntrySequence extends AbstractEntrySequence {

    private final EntryFactory entryFactory =new EntryFactory();
    private final EntriesFile entriesFile;
    private final EntryIndexFile entryIndexFile;
    private final LinkedList<Entry> pendingEntries = new LinkedList<>();

    //raft算法中定义初始commitIndex为0,和日志是否持久化无关
    private int commitIndex=0;

    // 构造器，指定目录
    public FileEntrySequence(LogDir logDir, int logIndexOffset) {
        super(logIndexOffset);
        try {
            this.entriesFile = new EntriesFile(logDir.getEntriesFile());
            this.entryIndexFile = new EntryIndexFile(logDir.getEntryOffsetIndexFile());
            initialize();
        } catch (IOException e) {
            throw new LogException("failed to open entries file or entry index file", e);
        }
    }

    // 构造器，指定文件
    public FileEntrySequence(EntriesFile entriesFile,EntryIndexFile entryIndexFile,int logIndexOffset) {

        //默认logIndexOffset由外部决定
        super(logIndexOffset);
        this.entriesFile = entriesFile;
        this.entryIndexFile = entryIndexFile;

    }

    //初始化
    private void initialize(){
        if (entryIndexFile.isEmpty()){
            return;
        }

        //使用日志索引文件的minEntryIndex作为logIndexOffset
       logIndexOffset= entryIndexFile.getMinEntryIndex();

        //使用日志索引文件的maxEntryIndex加1作为nextLogOffset
        nextLogIndex =entryIndexFile.getMaxEntryIndex()+1;
    }

    //获取commitIndex
    public int getCommitIndex(){
        return commitIndex;
    }

    @Override
    protected List<Entry> doSubList(int fromIndex, int toIndex) {

        //结果分为来自文件的与来自缓冲的两部分
       List<Entry> result =new ArrayList<>();

       //从文件中获取日志条目
       if (!entryIndexFile.isEmpty()&&fromIndex<= entryIndexFile.getMaxEntryIndex()){
           int maxIndex = Math.min(entryIndexFile.getMaxEntryIndex() + 1, toIndex);
           for (int i = fromIndex; i < maxIndex; i++) {
               result.add(getEntryInFile(i));
           }
       }

       //从日志缓冲中获取日志条目
        if (!pendingEntries.isEmpty()&&toIndex>pendingEntries.getFirst().getIndex()){
            Iterator<Entry> iterator = pendingEntries.iterator();
            Entry entry;
            int index;
            while(iterator.hasNext()){
               entry= iterator.next();
               index =entry.getIndex();
               if (index>=toIndex){
                   break;
               }
               if (index>=fromIndex){
                   result.add(entry);
               }
            }
        }
        return result;
    }

    private Entry getEntryInFile(int index){
        long offset =entryIndexFile.getOffset(index);
        try {
            return entriesFile.loadEntry(offset,entryFactory);
        } catch (IOException e) {
            throw  new LogException("failed to load entry "+index ,e);
        }
    }

    //获取指定位置的日志条目
    @Override
    protected Entry doGetEntry(int index) {
        if (!pendingEntries.isEmpty()){
            int firstPendingEntryIndex =pendingEntries.getFirst().getIndex();
            if (index >= firstPendingEntryIndex){
                return pendingEntries.get(index -firstPendingEntryIndex);
            }
        }
        assert !entryIndexFile.isEmpty();
        return getEntryInFile(index);
    }

    //获取日志元信息
    public EntryMeta getEntryMeta(int index){
        if (!isEntryPresent(index)){
            return null;
        }
        if (!pendingEntries.isEmpty()){
            int firstPendingEntryIndex= pendingEntries.getFirst().getIndex();
            if (index>=firstPendingEntryIndex){
                return pendingEntries.get(index -firstPendingEntryIndex).getMeta();
            }
        }
        return entryIndexFile.get(index).toEntryMeta();
    }

    //获取最后一条日志
    public Entry getLastEntry(){
        if (isEmpty()){
            return null;
        }
        if (!pendingEntries.isEmpty()){
            return pendingEntries.getLast();
        }
        assert !entryIndexFile.isEmpty();
        return getEntryInFile(entryIndexFile.getMaxEntryIndex());
    }


    //追加日志条目
    @Override
    protected void doAppend(Entry entry) {
        pendingEntries.add(entry);
    }

    //移出指定索引之后的日志条目
    @Override
    protected void doRemoveAfter(int index) {
        if (!pendingEntries.isEmpty()&&index>=pendingEntries.getFirst().getIndex()-1){
            //移除指定数量的日志条目
            //循环方向是从小到大,但是移除是从后往前
            //最终移除指定数量的日志条目
            for (int i = index+1; i <doGetLastLogIndex() ; i++) {
                pendingEntries.removeLast();
            }
            nextLogIndex=index+1;
            return;
        }
        try {
            if (index >= doGetFirstLogIndex()){
                //索引比日志缓冲中的第一条日志小
                pendingEntries.clear();
                entriesFile.truncate(entryIndexFile.getOffset(index +1));
                entryIndexFile.removeAfter(index);
                nextLogIndex=index+1;
                commitIndex=index;
            }else {

                //如果索引比第一条日志的索引都小,则清除所有数据
                pendingEntries.clear();
                entriesFile.clear();
                entryIndexFile.clear();
                nextLogIndex=logIndexOffset;
                commitIndex=logIndexOffset-1;
            }
        } catch (IOException e) {
           throw new LogException(e);
        }
    }

    @Override
    public void commit(int index) {

        //检查commitIndex
        if (index < commitIndex) {
            throw new IllegalArgumentException("commit index < " + commitIndex);
        }

        if (index == commitIndex) {
            return;
        }

        //如果commitIndex在文件内,则只要更新commitIndex
        if (!entryIndexFile.isEmpty() && index <= entryIndexFile.getMaxEntryIndex()) {
            commitIndex = index;
            return;
        }

        //检查commitIndex是否在日志缓冲的区间内
        if (pendingEntries.isEmpty() ||
                pendingEntries.getFirst().getIndex() > index ||
                pendingEntries.getLast().getIndex() < index) {
            throw new IllegalArgumentException("no entry to commit or commit index exceed");
        }
        long offset;
        Entry entry = null;
        try {
            for (int i = pendingEntries.getFirst().getIndex(); i < index; i++) {
                entry = pendingEntries.removeFirst();
                offset = entriesFile.appendEntry(entry);
                entryIndexFile.appendEntryIndex(i, offset, entry.getKind(), entry.getTerm());
                commitIndex = i;
            }
        } catch (IOException e) {
            throw new LogException("failed to commit entry " + entry, e);
        }
    }

}
