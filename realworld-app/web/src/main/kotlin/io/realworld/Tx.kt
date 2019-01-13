package io.realworld

import arrow.effects.IO
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

fun <A> IO<A>.runReadTx(txManager: PlatformTransactionManager): A = TransactionTemplate(txManager)
  .apply {
    isReadOnly = true
    isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED
    propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
  }
  .execute { this.unsafeRunSync() }!!

fun <A> IO<A>.runWriteTx(txManager: PlatformTransactionManager): A = TransactionTemplate(txManager)
  .apply {
    isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED
    propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
  }
  .execute { this.unsafeRunSync() }!!
