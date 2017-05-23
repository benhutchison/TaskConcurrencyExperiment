package concur

object Main extends App {
  import org.atnos.eff._
  import org.atnos.eff.all._
  import org.atnos.eff.syntax.all._
  import monix.eval.Task
  import org.atnos.eff.addon.monix.task._
  import org.atnos.eff.syntax.addon.monix.task._
  import cats.implicits._
  import monix.eval._
  import monix.execution._
  import monix.execution.schedulers._

  val n = 20
  val pool = java.util.concurrent.Executors.newScheduledThreadPool(n)

  //varying the batch size affects whether the program completes or hangs
  //small batch sizes allow the latch count downs to spread across threads sufficiently
  //larger batch sizes make execution too synchronous for the latch to countdown
  
//  implicit val s = monix.execution.Scheduler(pool, ExecutionModel.BatchedExecution(4))
  implicit val s = monix.execution.Scheduler(pool, ExecutionModel.BatchedExecution(16))

  type S = Fx.fx1[Task]

  val latch = new java.util.concurrent.CountDownLatch(n)

  def countdown = taskSuspend(Task.eval({println("countDown"); latch.countDown(); latch.await()}.pureEff[S]))

  println("Begin...")
  val task = Eff.traverseA((1 to 10).toList)(_ => countdown)
  task.runAsync.runAsync
  println("Await...")
  latch.await()
  println("Talk less, listen more")
  pool.shutdown()
}
