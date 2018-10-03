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

package controllers

import controllers.audit.Auditable
import controllers.auth.WithAuthorisedForTaiLite
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.{AuditConstants, TaiConstants}
import uk.gov.hmrc.tai.viewModels.{FullTaxSummaryForYearResponse, TaxAccountSummaryViewModel}
import views.html.incomeTaxSummary

import scala.concurrent.Future

trait TaxAccountSummaryController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with AuditConstants {

  def personService: PersonService
  def auditService: AuditService
  def taxAccountService: TaxAccountService

  def onPageLoad: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {

            val nino = Nino(user.getNino)
            auditService.createAndSendAuditEvent(TaxAccountSummary_UserEntersSummaryPage, Map("nino" -> user.getNino))

            taxAccountService.fullTaxAccountSummary(nino, TaxYear()).flatMap {
              case TaiTaxAccountFailureResponse(message) if message.toLowerCase.contains(TaiConstants.NpsTaxAccountDataAbsentMsg) ||
                message.toLowerCase.contains(TaiConstants.NpsNoEmploymentForCurrentTaxYear) =>
                Future.successful(Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
              case TaiSuccessResponseWithPayload(taxAccountSummary: FullTaxSummaryForYearResponse) =>
                Future successful Ok(incomeTaxSummary(TaxAccountSummaryViewModel(taxAccountSummary)))
              case _ => throw new RuntimeException("Failed to fetch tax account summary details")
            }
          }
  }
}
// $COVERAGE-OFF$
object TaxAccountSummaryController extends TaxAccountSummaryController with AuthenticationConnectors {
  override val personService: PersonService = PersonService
  override val auditService: AuditService = AuditService
  override val taxAccountService: TaxAccountService = TaxAccountService
  override implicit def templateRenderer: LocalTemplateRenderer.type = LocalTemplateRenderer
  override implicit def partialRetriever: TaiHtmlPartialRetriever.type = TaiHtmlPartialRetriever
}
// $COVERAGE-ON$
