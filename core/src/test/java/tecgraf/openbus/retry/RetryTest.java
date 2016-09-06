package tecgraf.openbus.retry;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.util.concurrent.ListenableFuture;

@SuppressWarnings("javadoc")
public class RetryTest {

  private final RetryTaskPool taskPool = new RetryTaskPool(20);
  private final RetryContext retryContext = new RetryContext(1, TimeUnit
    .SECONDS);

  @Test
  public void retryPoolTest() throws InterruptedException, ExecutionException {
    ListenableFuture<Void> future = taskPool.doTask(new Runnable() {
      int n = 0;

      @Override
      public void run() {
        if (n < 3) {
          n++;
          throw new RuntimeException("Need to retry");
        }
        // Ok!
      }
    }, retryContext);
    Assert.assertFalse(future.isCancelled());
    Assert.assertFalse(future.isDone());
    future.get();
    Assert.assertTrue(future.isDone());
  }

  @Test
  public void failTest() throws InterruptedException, ExecutionException {
    ListenableFuture<Void> future = taskPool.doTask(new Runnable() {
      int n = 0;

      @Override
      public void run() {
        if (n < 3) {
          n++;
          throw new RuntimeException("Need to retry");
        }
        // Ok!
      }
    }, new RetryContext(1, TimeUnit.SECONDS) {
      @Override
      public boolean shouldRetry() {
        return (getRetryCount() <= 2);
      }
    });

    Assert.assertFalse(future.isDone());
    try {
      future.get();
    }
    catch (ExecutionException e) {
      if (!(e.getCause() instanceof RuntimeException)) {
        Assert.fail();
      }
    }
    Assert.assertTrue(future.isDone());
  }
}
