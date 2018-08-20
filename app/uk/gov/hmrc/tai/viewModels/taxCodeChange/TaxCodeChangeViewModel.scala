/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels.taxCodeChange

import uk.gov.hmrc.tai.model.domain.{TaxCodePair, TaxCodeRecord}

//case class TaxCodeChangeViewModel(allTaxCodePairsOrdered: Seq[TaxCodePair]){
//
//}
//
//case class TaxCodeChange(previous: Seq[TaxCodeRecord], current: Seq[TaxCodeRecord]){}
//
//case class TaxCodeChange(taxCodeChange: TaxCodeChange){}
//
//
//case class TaxCodeChange(primary: Seq[TaxCodePair],
//                         secondary: Seq[TaxCodeRecord],
//                         unmatchedPrevious: Seq[TaxCodePair],
//                         unmatchedCurrent: Seq[TaxCodePair]
//                        ) {
//
//
//}

case class TaxCodePair(previous: Option[TaxCodeRecord], current: Option[TaxCodeRecord])

class TaxCodePairs(previous: Seq[TaxCodeRecord], current: Seq[TaxCodeRecord]) {
  val pairs: Seq[TaxCodePair] = {
    primaryPairs ++ secondaryPairs ++ unpairedPreviousCodes ++ unpairedCurrentCodes
  }

  lazy private val primaryPairs: Seq[TaxCodePair] = {
    taxCodePairs.filter(taxCodeRecordPair => taxCodeRecordPair.current.exists(_.primary))
  }

  lazy private val secondaryPairs: Seq[TaxCodePair] = {
    taxCodePairs.filterNot(taxCodeRecordPair => taxCodeRecordPair.current.exists(_.primary))
  }

  lazy private val unpairedCurrentCodes: Seq[TaxCodePair] = {
    val unpairedRecords = current.filterNot(record => taxCodePairs.map(_.current).contains(Some(record)))

    unpairedRecords.map(record => TaxCodePair(None, Some(record)))
  }

  lazy private val unpairedPreviousCodes: Seq[TaxCodePair] = {
    val unpairedRecords = previous.filterNot(record => taxCodePairs.map(_.previous).contains(Some(record)))

    unpairedRecords.map(record => TaxCodePair(Some(record), None))
  }

  lazy private val taxCodePairs: Seq[TaxCodePair] = {
    for {
      p <- previous
      c <- current
      if p.employmentId == c.employmentId
    } yield TaxCodePair(Some(p), Some(c))
  }
}






