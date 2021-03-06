/*
 * Copyright the State of the Netherlands
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package nl.aerius.taskmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

import nl.aerius.metrics.MetricFactory;
import nl.aerius.taskmanager.TaskScheduler.TaskSchedulerFactory;
import nl.aerius.taskmanager.adaptor.AdaptorFactory;
import nl.aerius.taskmanager.domain.PriorityTaskQueue;
import nl.aerius.taskmanager.domain.PriorityTaskSchedule;

/**
 * Test class for {@link TaskManager}.
 */
public class TaskManagerTest {

  private static ExecutorService executor;
  private final PriorityTaskSchedulerFileHandler handler = new PriorityTaskSchedulerFileHandler();
  private PriorityTaskSchedule schedule;
  private TaskManager<PriorityTaskQueue, PriorityTaskSchedule> taskManager;

  @Before
  public void setUp() throws IOException, InterruptedException {
    executor = Executors.newCachedThreadPool();
    MetricFactory.init(new Properties(), "test");
    final AdaptorFactory factory = new MockAdaptorFactory();
    final TaskSchedulerFactory<PriorityTaskQueue, PriorityTaskSchedule> schedulerFactory = new FIFOTaskScheduler.FIFOSchedulerFactory();
    taskManager = new TaskManager<>(executor, factory, schedulerFactory);
    schedule = handler.read(new File(getClass().getClassLoader().getResource("queue/priority-task-scheduler.ops.json").getFile()));
  }

  @After
  public void after() throws InterruptedException {
    taskManager.shutdown();
    executor.shutdownNow();
    executor.awaitTermination(10, TimeUnit.MILLISECONDS);
    MetricFactory.getMetrics().removeMatching((name, metric) -> true);
  }

  @Test
  public void testAddScheduler() throws IOException, InterruptedException {
    assertTrue("TaskScheduler running", taskManager.updateTaskScheduler(schedule));
    taskManager.removeTaskScheduler(schedule.getWorkerQueueName());
  }

  @Test
  public void testModifyQueue() throws IOException, InterruptedException {
    assertTrue("TaskScheduler running", taskManager.updateTaskScheduler(schedule));
    schedule.getTaskQueues().get(0).setPriority(30);
    assertTrue("TaskScheduler updated", taskManager.updateTaskScheduler(schedule));
    taskManager.removeTaskScheduler(schedule.getWorkerQueueName());
  }

  @Test
  public void testRemoveQueue() throws IOException, InterruptedException {
    assertTrue("TaskScheduler running", taskManager.updateTaskScheduler(schedule));
    schedule.getTaskQueues().remove(0);
    assertTrue("TaskScheduler updated", taskManager.updateTaskScheduler(schedule));
    taskManager.removeTaskScheduler(schedule.getWorkerQueueName());
  }

  @Test
  public void testMetricAvailable() throws IOException, InterruptedException {
    assertTrue("TaskScheduler running", taskManager.updateTaskScheduler(schedule));
    final MetricRegistry metrics = MetricFactory.getMetrics();
    assertEquals("There should be 3 gauges in a scheduler.", 3, metrics.getGauges((name, metric) -> name.startsWith("OPS")).size());
  }
}
