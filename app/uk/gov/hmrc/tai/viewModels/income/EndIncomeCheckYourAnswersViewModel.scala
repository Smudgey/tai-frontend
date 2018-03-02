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

package uk.gov.hmrc.tai.viewModels.income

import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.tai.util.DatePatternConstants

case class EndIncomeCheckYourAnswersViewModel(preHeading: String,
                                              employmentName: String,
                                              employmentId: Int,
                                              employmentEndDate: String,
                                              contactableByPhone: String,
                                              phoneNumber: Option[String],
                                              backLinkUrl: String) extends DatePatternConstants {

  def journeyConfirmationLines: Seq[CheckYourAnswersConfirmationLine] = {

    val mandatoryLines = Seq(
      CheckYourAnswersConfirmationLine(Messages("tai.addEmployment.cya.q2"), readable(employmentEndDate), controllers.employments.routes.EndEmploymentController.endEmploymentPage(employmentId).url),
      CheckYourAnswersConfirmationLine(Messages("tai.addEmployment.cya.q4"), contactableByPhone, controllers.employments.routes.EndEmploymentController.addTelephoneNumber().url)
    )

    val optionalPhoneNoLine = phoneNumber map { phoneNo =>
      Seq(CheckYourAnswersConfirmationLine(Messages("tai.phoneNumber"), phoneNo, controllers.employments.routes.EndEmploymentController.addTelephoneNumber.url))
    }

    if(optionalPhoneNoLine.isDefined) mandatoryLines ++ optionalPhoneNoLine.get else mandatoryLines
  }

  private def readable(isoDateString: String) : String = {
    LocalDate.parse(isoDateString).toString(datePatternWithFullMonthName)
  }
}