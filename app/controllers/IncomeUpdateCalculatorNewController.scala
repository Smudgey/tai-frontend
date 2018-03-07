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

import controllers.ServiceChecks.CustomRule
import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTai, WithAuthorisedForTaiLite}
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.forms._
import uk.gov.hmrc.tai.model.{EmploymentAmount, IncomeCalculation, SessionData}
import uk.gov.hmrc.tai.service.TaiService.IncomeIDPage
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.{FormHelper, JourneyCacheConstants, TaxSummaryHelper}
import views.html.incomes.howToUpdate

import scala.concurrent.Future

trait IncomeUpdateCalculatorNewController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with JourneyCacheConstants {

  def taiService: TaiService

  def journeyCacheService: JourneyCacheService

  def employmentService: EmploymentService

  def activityLoggerService: ActivityLoggerService

  val incomeService: IncomeService

  def handleChooseHowToUpdate: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("processChooseHowToUpdate")
        HowToUpdateForm.createForm().bindFromRequest().fold(
          formWithErrors => {
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
            } yield {
              BadRequest(views.html.incomes.howToUpdate(formWithErrors, id, Some(employerName)))
            }
          },
          formData => {
            formData.howToUpdate match {
              case Some("incomeCalculator") => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.workingHoursPage()))
              case _ => Future.successful(Redirect(routes.IncomeController.viewIncomeForEdit()))
            }
          }
        )
  }

  def workingHoursPage: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("getWorkingHours")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
        } yield {
          Ok(views.html.incomes.workingHours(HoursWorkedForm.createForm(), id, Some(employerName)))
        }
  }

  def handleWorkingHours: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("processWorkedHours")
        HoursWorkedForm.createForm().bindFromRequest().fold(
          formWithErrors => {
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
            } yield {
              BadRequest(views.html.incomes.workingHours(formWithErrors, id, Some(employerName)))
            }
          },
          formData => {
            formData.workingHours match {
              case Some("same") => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.payPeriodPage()))
              case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.calcUnavailablePage()))
            }
          }
        )
  }

  def payPeriodPage: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("getPayPeriodPage")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
        } yield {
          Ok(views.html.incomes.payPeriod(PayPeriodForm.createForm(None), id, employerName = Some(employerName)))
        }
  }

  def handlePayPeriod: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("processPayPeriod")
        val payPeriod: Option[String] = request.body.asFormUrlEncoded.flatMap(m => m.get("payPeriod").flatMap(_.headOption))

        PayPeriodForm.createForm(None, payPeriod).bindFromRequest().fold(
          formWithErrors => {
            val isNotDaysError = formWithErrors.errors.filter { error => error.key == "otherInDays" }.isEmpty
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
            } yield {
              BadRequest(views.html.incomes.payPeriod(formWithErrors, id, isNotDaysError, Some(employerName)))
            }
          },
          formData => {
            journeyCacheService.cache(incomeService.cachePayPeriod(formData)).map { _ =>
              Redirect(routes.IncomeUpdateCalculatorController.payslipAmountPage())
            }
          }
        )
  }

  def payslipAmountPage: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("getPayslipAmountPage")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
          payPeriod <- journeyCacheService.mandatoryValue(UpdateIncome_PayPeriodKey)
        } yield {
          Ok(views.html.incomes.payslipAmount(PayslipForm.createForm(), payPeriod, id, Some(employerName)))
        }
  }

  def handlePayslipAmount: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("processPayslipAmount")
        PayslipForm.createForm().bindFromRequest().fold(
          formWithErrors => {
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
              payPeriod <- journeyCacheService.mandatoryValue(UpdateIncome_PayPeriodKey)
            } yield {
              BadRequest(views.html.incomes.payslipAmount(formWithErrors, payPeriod, id, Some(employerName)))
            }
          },
          formData => {
            formData match {
              case PayslipForm(Some(value)) => journeyCacheService.cache(UpdateIncome_TotalSalaryKey, value).map { _ =>
                Redirect(routes.IncomeUpdateCalculatorController.payslipDeductionsPage())
              }
              case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.payslipDeductionsPage()))
            }
          }
        )
  }

  def taxablePayslipAmountPage: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("getTaxablePayslipAmountPage")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
          payPeriod <- journeyCacheService.mandatoryValue(UpdateIncome_PayPeriodKey)
        } yield {
          Ok(views.html.incomes.taxablePayslipAmount(TaxablePayslipForm.createForm(), payPeriod, id, Some(employerName)))
        }
  }

  def handleTaxablePayslipAmount: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("processTaxablePayslipAmount")

        journeyCacheService.currentValue(UpdateIncome_TotalSalaryKey) flatMap { cacheTotalSalary =>
          val totalSalary = FormHelper.stripNumber(cacheTotalSalary)
          TaxablePayslipForm.createForm(totalSalary).bindFromRequest().fold(
            formWithErrors => {
              for {
                id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
                employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
                payPeriod <- journeyCacheService.mandatoryValue(UpdateIncome_PayPeriodKey)
              } yield {
                BadRequest(views.html.incomes.taxablePayslipAmount(formWithErrors, payPeriod, id, Some(employerName)))
              }
            },
            formData => {
              formData.taxablePay match {
                case Some(taxablePay) => journeyCacheService.cache(UpdateIncome_TaxablePayKey, taxablePay) map { _ =>
                  Redirect(routes.IncomeUpdateCalculatorController.bonusPaymentsPage())
                }
                case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.bonusPaymentsPage()))
              }
            }
          )
        }
  }

  def payslipDeductionsPage: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("getPayslipDeductionsPage")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
        } yield {
          Ok(views.html.incomes.payslipDeductions(PayslipDeductionsForm.createForm(), id, Some(employerName)))
        }
  }

  def handlePayslipDeductions: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("processPayslipDeductions")

        PayslipDeductionsForm.createForm().bindFromRequest().fold(
          formWithErrors => {
            for {
              id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
              employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
            } yield {
              BadRequest(views.html.incomes.payslipDeductions(formWithErrors, id, Some(employerName)))
            }
          },
          formData => {
            formData.payslipDeductions match {
              case Some(payslipDeductions) if payslipDeductions == "Yes" =>
                journeyCacheService.cache(UpdateIncome_PayslipDeductionsKey, payslipDeductions).map { _ =>
                  Redirect(routes.IncomeUpdateCalculatorController.taxablePayslipAmountPage())
                }
              case Some(payslipDeductions) => journeyCacheService.cache(UpdateIncome_PayslipDeductionsKey, payslipDeductions) map { _ =>
                Redirect(routes.IncomeUpdateCalculatorController.bonusPaymentsPage())
              }
              case _ => Future.successful(Redirect(routes.IncomeUpdateCalculatorController.bonusPaymentsPage()))
            }
          }
        )
  }

  def bonusPaymentsPage: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        sendActingAttorneyAuditEvent("getBonusPaymentsPage")
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
          paySlipDeductions <- journeyCacheService.mandatoryValue(UpdateIncome_PayslipDeductionsKey)
        } yield {
          val isPaySlipDeductions = paySlipDeductions == "Yes"
          Ok(views.html.incomes.bonusPayments(BonusPaymentsForm.createForm(), id, isPaySlipDeductions, false, Some(employerName)))
        }
  }

  def calcUnavailablePage: Action[AnyContent] = authorisedForTai(taiService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        for {
          id <- journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
          employerName <- journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
        } yield {
          Ok(views.html.incomes.calcUnavailable(id, Some(employerName)))
        }
  }
}

object IncomeUpdateCalculatorNewController extends IncomeUpdateCalculatorNewController with AuthenticationConnectors {
  override val taiService: TaiService = TaiService
  override val activityLoggerService: ActivityLoggerService = ActivityLoggerService
  override val journeyCacheService = JourneyCacheService(UpdateIncome_JourneyKey)
  override val employmentService: EmploymentService = EmploymentService
  override val incomeService: IncomeService = IncomeService

  override implicit def templateRenderer: TemplateRenderer = LocalTemplateRenderer

  override implicit def partialRetriever: PartialRetriever = TaiHtmlPartialRetriever

}
