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

import builders.{AuthBuilder, RequestBuilder}
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOperation, TaxCodeIncome, Week1Month1BasisOperation}
import uk.gov.hmrc.tai.service.{CodingComponentService, EmploymentService, PersonService, TaxAccountService}

import scala.concurrent.Future
import scala.util.Random

class IncomeTaxComparisonControllerSpec extends PlaySpec
with FakeTaiPlayApplication
with MockitoSugar
with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "onPageLoad" must {
    "display the cy plus one page" in {
      val sut = createSut
      when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
      when(sut.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary)))
      when(sut.codingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(
        Future.successful(Seq.empty[CodingComponent]))
      when(sut.employmentService.employments(Matchers.any(),Matchers.eq(TaxYear()))(Matchers.any())).thenReturn(
        Future.successful(Seq(employment)))
      when(sut.employmentService.employments(Matchers.any(),Matchers.eq(TaxYear().next))(Matchers.any())).thenReturn(
        Future.successful(Seq(employment)))


      val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include (Messages("tai.incomeTaxComparison.heading"))

      verify(sut.employmentService, times(1)).employments(Matchers.any(),Matchers.eq(TaxYear()))(Matchers.any())
      verify(sut.employmentService, times(1)).employments(Matchers.any(),Matchers.eq(TaxYear().next))(Matchers.any())

    }

    "throw an error page" when {
      "not able to fetch comparision details" in {
        val sut = createSut
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiNotFoundResponse("Not Found")))
        when(sut.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary)))
        when(sut.codingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(
          Future.successful(Seq.empty[CodingComponent]))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

  }

  val nino: Nino = new Generator(new Random).nextNino
  val employment = Employment("employment1", None, new LocalDate(), None, Nil, "", "", 1, None, false, false)
  val taxAccountSummary = TaxAccountSummary(111,222, 333, 444, 111)

  val taxCodeIncomes = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "employment", Week1Month1BasisOperation, Live)
  )


  def createSut = new SUT()

  class SUT() extends IncomeTaxComparisonController {
    override val personService: PersonService = mock[PersonService]
    override val taxAccountService: TaxAccountService = mock[TaxAccountService]
    override val codingComponentService: CodingComponentService = mock[CodingComponentService]
    override val employmentService: EmploymentService = mock[EmploymentService]
    override val authConnector: AuthConnector = mock[AuthConnector]
    override val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override implicit def templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit def partialRetriever: FormPartialRetriever = MockPartialRetriever

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))
    when(authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData)
  }
}
