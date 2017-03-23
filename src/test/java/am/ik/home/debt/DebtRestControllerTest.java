package am.ik.home.debt;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.restassured3.operation.preprocess.RestAssuredPreprocessors.modifyUris;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.context.junit4.SpringRunner;

import am.ik.home.DebtApplicationTests;
import am.ik.home.Fixtures;
import am.ik.home.TestTokenGenerator;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.cloud.config.enabled=false",
		"api.authorization-url=http://localhost:" + DebtApplicationTests.UAA_PORT,
		"security.oauth2.resource.jwt.key-uri=http://localhost:"
				+ DebtRestControllerTest.UAA_PORT + "/token_key" })
public class DebtRestControllerTest {
	public final static int UAA_PORT = 12875;
	static String accessToken;

	@Rule
	public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation(
			"target/generated-snippets");
	RequestSpecification documentationSpec;
	@LocalServerPort
	int port;
	@MockBean
	DebtRepository debtRepository;

	@BeforeClass
	public static void init() throws Exception {
		TestTokenGenerator tokenGenerator = new TestTokenGenerator();
		accessToken = tokenGenerator.getAccessToken();
		tokenGenerator.initTokenKeyServer(UAA_PORT);
	}

	@Before
	public void setUp() throws Exception {
		this.documentationSpec = new RequestSpecBuilder()
				.addFilter(documentationConfiguration(this.restDocumentation)).build();
	}

	@Test
	public void getDebts() throws Exception {
		given(debtRepository.findAll()).willReturn(Fixtures.debts());

		RestAssured.given(this.documentationSpec).filter(document("api/get-debts",
				preprocessRequest(
						modifyUris().scheme("https").host("debt.ik.am").removePort()),
				responseFields(fieldWithPath("[].debtId").description("借金ID"),
						fieldWithPath("[].purpose").description("借金の目的"),
						fieldWithPath("[].amount").description("金額"),
						fieldWithPath("[].from").description("貸した人"),
						fieldWithPath("[].to").description("借りた人"),
						fieldWithPath("[].debtDate").description("借金日"),
						fieldWithPath("[].repayments").description("返済履歴"),
						fieldWithPath("[].repayments[].amount").description("返済金額"),
						fieldWithPath("[].repayments[].repaymentDate").description("返済日"),
						fieldWithPath("[].repayments[].createdAt").description("登録時刻"),
						fieldWithPath("[].createdAt").description("登録時刻"))))
				.when().log().all()
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.port(this.port).get("/v1/debts").then().log().all().assertThat()
				.statusCode(200);
	}

	@Test
	public void postDebts() throws Exception {
		given(debtRepository.save(Mockito.<Debt> any())).willReturn(Mono.just(1L));

		Debt debt = Fixtures.debt().block();
		Map<String, Object> request = new HashMap<>();
		request.put("purpose", debt.getPurpose());
		request.put("amount", debt.getAmount());
		request.put("from", debt.getFrom());
		request.put("to", debt.getTo());
		request.put("debtDate", debt.getDebtDate().toString());

		RestAssured.given(this.documentationSpec)
				.filter(document("api/post-debts",
						preprocessRequest(modifyUris().scheme("https").host("debt.ik.am")
								.removePort()),
						requestFields(fieldWithPath("purpose").description("借金の目的"),
								fieldWithPath("amount").description("金額"),
								fieldWithPath("from").description("貸した人"),
								fieldWithPath("to").description("借りた人"),
								fieldWithPath("debtDate").description("借金日")),
						responseFields(fieldWithPath("debtId").description("借金ID"),
								fieldWithPath("purpose").description("借金の目的"),
								fieldWithPath("amount").description("金額"),
								fieldWithPath("from").description("貸した人"),
								fieldWithPath("to").description("借りた人"),
								fieldWithPath("debtDate").description("借金日"),
								fieldWithPath("repayments").description("返済履歴"),
								fieldWithPath("createdAt").description("登録時刻"))))
				.when().log().all()
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(ContentType.JSON).body(request).port(this.port)
				.post("/v1/debts").then().log().all().assertThat().statusCode(201);
	}
}