package io.realworld

import kotlinx.coroutines.runBlocking
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

fun <A> runReadTx(txManager: PlatformTransactionManager, fn: suspend () -> A): A {
  return TransactionTemplate(txManager)
    .apply {
      isReadOnly = true
      isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED
      propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }
    .execBlocking(fn)
}

fun <A> runWriteTx(txManager: PlatformTransactionManager, fn: suspend () -> A): A {
  return TransactionTemplate(txManager)
    .apply {
      isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED
      propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }
    .execBlocking(fn)
}

private fun <A> TransactionTemplate.execBlocking(fn: suspend () -> A): A {
  // TransactionTemplate#execute is annotated as @Nullable which is used by Kotlin to infer nullability.
  // However, the return value of the given fn is the one that defines whether result is nullable or not.
  //
  // fn supports implicit nullability as it's defined to return A (<A> is implicitly <A : Any?>), leaving the final
  // decision on to the calling code that provides the fn implementation and typing.
  @Suppress("UNCHECKED_CAST")
  return execute { runBlocking { fn() } } as A
}
