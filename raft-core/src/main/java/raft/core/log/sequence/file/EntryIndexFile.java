package raft.core.log.sequence.file;

import raft.core.support.RandomAccessFileAdapter;
import raft.core.support.SeekableFile;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class EntryIndexFile implements Iterable<EntryIndexItem>{

    //最大条目索引的偏移量`
    private static final long OFFSET_MAX_ENTRY_INDEX =Integer.BYTES;

    //单条日志条目元信息的长度
    private static final int LENGTH_ENTRY_INDEX_ITEM=16;

    private final SeekableFile seekableFile;

    //日志条目数
    private int entryIndexCount;

    //最小日志索引
    private int minEntryIndex;

    //最大日志索引
    private int maxEntryIndex;

    //从文件加载到内存中
    private Map<Integer,EntryIndexItem> entryIndexMap=new HashMap<>();

    //构造器,普通文件
    public EntryIndexFile(File file) throws IOException {
        this(new RandomAccessFileAdapter(file));

    }

    //构造器,seekableFile
    public EntryIndexFile(SeekableFile seekableFile) throws IOException {
        this.seekableFile = seekableFile;
        load();
    }

    //加载所有日志元信息
    private void load() throws IOException {
        if (seekableFile.size()==0L){
            entryIndexCount=0;
            return;
        }

        minEntryIndex = seekableFile.readInt();
        maxEntryIndex =seekableFile.readInt();
        updateEntryIndexCount();

        //逐条加载到内存中
        long offset;
        int kind;
        int term;
        for (int i = minEntryIndex; i <maxEntryIndex ; i++) {
            offset=seekableFile.readLong();
            kind=seekableFile.readInt();
            term=seekableFile.readInt();
            entryIndexMap.put(i,new EntryIndexItem(i,offset,kind,term));
        }
    }

    //更改日志条目数量
    private void updateEntryIndexCount(){
        entryIndexCount =maxEntryIndex -maxEntryIndex +1;
    }

    //追加日志条目元信息
    public void appendEntryIndex(int index,long offset,int kind,int term) throws IOException {
        if (seekableFile.size()==0L){

            //如果文件为空,则写入minEntryIndex
            seekableFile.writeInt(index);
            minEntryIndex=index;
        }else {
            //索引检查
            if (index != maxEntryIndex+1){
                throw  new IllegalArgumentException("index must be "+(maxEntryIndex+1)+", but was "+ index);
            }

            //跳过minEntryIndex
            seekableFile.seek(OFFSET_MAX_ENTRY_INDEX);
        }

        //写入maxEntryIndex
        seekableFile.writeInt(index);
        maxEntryIndex=index;
        updateEntryIndexCount();

        //移动到文件最后
        seekableFile.seek(getOffsetOfEntryIndexItem(index));
        seekableFile.writeLong(offset);
        seekableFile.writeInt(kind);
        seekableFile.writeInt(term);
        entryIndexMap.put(index,new EntryIndexItem(index,offset,kind,term));

    }

    //获取指定索引的日志的偏移
    private long getOffsetOfEntryIndexItem(int index){
        return (index - minEntryIndex) * LENGTH_ENTRY_INDEX_ITEM+Integer.BYTES*2;
    }

    //清除全部
    public void clear()throws IOException{
        seekableFile.truncate(0L);
        entryIndexCount=0;
        entryIndexMap.clear();
    }

    //移除某个索引之后的数据
    public void removeAfter(int newMaxEntryIndex)throws IOException{
        //判断是否为空
        if (isEmpty()||newMaxEntryIndex >=maxEntryIndex){
            return;
        }

        //判断新的maxEntryIndex是否比minEntryIndex小
        //如果是则全部移除
        if (newMaxEntryIndex<maxEntryIndex){
            clear();
            return;
        }

        //修改maxEntryIndex
        seekableFile.seek(OFFSET_MAX_ENTRY_INDEX);
        seekableFile.writeInt(newMaxEntryIndex);

        //裁剪文件
        seekableFile.truncate(getOffsetOfEntryIndexItem(newMaxEntryIndex+1));

        //移出缓存中的元信息
        for (int i = newMaxEntryIndex+1; i <maxEntryIndex ; i++) {
            entryIndexMap.remove(i);
        }

        maxEntryIndex=newMaxEntryIndex;
        entryIndexCount=newMaxEntryIndex-minEntryIndex+1;
    }

    public boolean isEmpty() {
        return entryIndexCount == 0;
    }

    //从外部遍历所有的日志条目信息
    @Override
    public Iterator<EntryIndexItem> iterator() {
       if (isEmpty()){
           return Collections.emptyIterator();
       }
       return new EntryIndexIterator(entryIndexCount,maxEntryIndex);
    }

   private class EntryIndexIterator implements Iterator<EntryIndexItem>{

       //条目总数
        private final int entryIndexCount;

        //当前索引
       private int currentEntryIndex;

       public EntryIndexIterator(int entryIndexCount, int currentEntryIndex) {
           this.entryIndexCount = entryIndexCount;
           this.currentEntryIndex = currentEntryIndex;
       }

       //是否存在下一条
       @Override
       public boolean hasNext() {
           checkModification();
           return currentEntryIndex<=maxEntryIndex;
       }

       //获取下一条
       @Override
       public EntryIndexItem next() {
           checkModification();
           return entryIndexMap.get(currentEntryIndex++);
       }

       //检查是否修改
       private void checkModification(){
           if (this.entryIndexCount!=EntryIndexFile.this.entryIndexCount){
               throw new IllegalStateException("entry index count changed");
           }
       }
   }

    public long getOffset(int entryIndex) {
        return get(entryIndex).getOffset();
    }

    @Nonnull
    public EntryIndexItem get(int entryIndex) {
        checkEmpty();
        if (entryIndex < minEntryIndex || entryIndex > maxEntryIndex) {
            throw new IllegalArgumentException("index < min or index > max");
        }
        return entryIndexMap.get(entryIndex);
    }

    private void checkEmpty() {
        if (isEmpty()) {
            throw new IllegalStateException("no entry index");
        }
    }


    public SeekableFile getSeekableFile() {
        return seekableFile;
    }

    public int getEntryIndexCount() {
        return entryIndexCount;
    }

    public void setEntryIndexCount(int entryIndexCount) {
        this.entryIndexCount = entryIndexCount;
    }

    public int getMinEntryIndex() {
        return minEntryIndex;
    }

    public void setMinEntryIndex(int minEntryIndex) {
        this.minEntryIndex = minEntryIndex;
    }

    public int getMaxEntryIndex() {
        return maxEntryIndex;
    }

    public void setMaxEntryIndex(int maxEntryIndex) {
        this.maxEntryIndex = maxEntryIndex;
    }

    public Map<Integer, EntryIndexItem> getEntryIndexMap() {
        return entryIndexMap;
    }

    public void setEntryIndexMap(Map<Integer, EntryIndexItem> entryIndexMap) {
        this.entryIndexMap = entryIndexMap;
    }
}
