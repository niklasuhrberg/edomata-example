/*
 * Copyright 2023 Hossein Naderi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.hnaderi.example.accounts

import edomata.core.*
import edomata.syntax.all.*
import cats.implicits.*
import cats.data.ValidatedNec
import dev.hnaderi.example.accounts.Event.Opened_V1

enum Event {
  case Opened_V1(category: String)
  case Opened
  case Deposited(amount: BigDecimal)
  case Withdrawn(amount: BigDecimal)
  case Closed
}

enum Rejection {
  case ExistingAccount
  case NoSuchAccount
  case InsufficientBalance
  case NotSettled
  case AlreadyClosed
  case BadRequest
}

enum Account {
  case New(category: String)
  case Open(category: String, balance: BigDecimal)
  case Close

  def open(category:String): Decision[Rejection, Event, Open] = this
    .decide {
      case New(_) => Decision.accept(Event.Opened_V1(category))
      case _   => Decision.reject(Rejection.ExistingAccount)
    }
    .validate(_.mustBeOpen)

  def close: Decision[Rejection, Event, Account] =
    this.perform(mustBeOpen.toDecision.flatMap { account =>
      if account.balance == 0 then Event.Closed.accept
      else Decision.reject(Rejection.NotSettled)
    })

  def withdraw(amount: BigDecimal): Decision[Rejection, Event, Open] = this
    .perform(mustBeOpen.toDecision.flatMap { account =>
      if account.balance >= amount && amount > 0
      then Decision.accept(Event.Withdrawn(amount))
      else Decision.reject(Rejection.InsufficientBalance)
      // We can model rejections to have values, which helps a lot for showing error messages, but it's out of scope for this document
    })
    .validate(_.mustBeOpen)

  def deposit(amount: BigDecimal): Decision[Rejection, Event, Open] = this
    .perform(mustBeOpen.toDecision.flatMap { account =>
      if amount > 0 then Decision.accept(Event.Deposited(amount))
      else Decision.reject(Rejection.BadRequest)
    })
    .validate(_.mustBeOpen)

  private def mustBeOpen: ValidatedNec[Rejection, Open] = this match {
    case o @ Open(_, _) => o.validNec
    case New(_)         => Rejection.NoSuchAccount.invalidNec
    case Close       => Rejection.AlreadyClosed.invalidNec
  }
}

object Account extends DomainModel[Account, Event, Rejection] {
  def initial = New("uninitialized")
  def transition = {
    case Event.Opened_V1(category) => _ => Open(category = category, balance = BigDecimal.valueOf(0)).validNec
    case Event.Opened => _ => Open(category = ( upcast(Event.Opened).asInstanceOf[Event.Opened_V1]).category, balance = BigDecimal.valueOf(0)).validNec

    case Event.Withdrawn(b) =>
      _.mustBeOpen.map(s => s.copy(balance = s.balance - b))
    case Event.Deposited(b) =>
      _.mustBeOpen.map(s => s.copy(balance = s.balance + b))
    case Event.Closed => _ => Close.validNec
  }

  def upcast(event: Event): Event = event match {
    case Event.Opened => Opened_V1("default_category")
  }
}
