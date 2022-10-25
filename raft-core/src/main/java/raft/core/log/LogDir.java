package raft.core.log;

import java.io.File;

/**
 * 获取指定文件的地址
 * @author yiyewei
 * @create 2022/9/26 23:01
 **/
public interface LogDir {
    //初始化目录
    void initialize();

    //是否存在
    boolean exists();

    //获取EntryIndexFile对应的文件
    File getEntriesFile();


    File getEntryOffsetIndexFile();

    //获取目录
    File get();

    //重命名目录
    boolean renameTo(LogDir logDir);
}
