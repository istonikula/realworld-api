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
    .execute { runBlocking { fn() } }!!
}

fun <A> runWriteTx(txManager: PlatformTransactionManager, fn: suspend () -> A): A {
  return TransactionTemplate(txManager)
    .apply {
      isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED
      propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }
    .execute { runBlocking { fn() } }!!
}
