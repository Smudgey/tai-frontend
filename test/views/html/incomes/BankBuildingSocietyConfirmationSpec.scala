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

package views.html.incomes

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BankBuildingSocietyConfirmationSpec extends TaiViewSpec {
  override def view: Html = views.html.incomes.bbsi.bank_building_society_confirmation()

  "Confirmation View" must {

    behave like pageWithTitle(messages("tai.bbsi.confirmation.heading"))

    "display return button" in {
      doc(view) must haveLinkWithText(messages("tai.bbsi.confirmation.back"))
      doc(view).getElementById("returnToAccounts") must haveLinkURL(controllers.income.bbsi.routes.BbsiController.accounts().url)
    }

  }
}
