package raft.core.scheduler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 空定时器
 * @author yiyewei
 * @create 2022/9/23 10:28
 **/
public class NullScheduler implements Scheduler{

    private static final Logger logger = LoggerFactory.getLogger(NullScheduler.class);

    @Override
    public LogReplicationTask scheduleLogReplicationTask(Runnable task) {
        logger.debug("schedule log replication task");
        return LogReplicationTask.NONE;
    }

    @Override
    public ElectionTimeout scheduleElectionTimeout(Runnable task) {
        logger.debug("schedule election timeout");
        return ElectionTimeout.NONE;
    }

    @Override
    public void stop() throws InterruptedException {

    }
}
