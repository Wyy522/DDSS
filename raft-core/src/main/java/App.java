import com.google.common.eventbus.EventBus;
import org.omg.CORBA.Current;
import raft.core.node.NodeContext;
import raft.core.node.NodeImpl;
import raft.core.node.store.MemoryNodeStore;
import raft.core.scheduler.DefaultScheduler;
import raft.core.support.DirectTaskExecutor;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {

    public static void main(String[] args) {

    }
}