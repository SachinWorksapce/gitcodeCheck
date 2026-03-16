package business_logics;

import static io.restassured.RestAssured.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Base64;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.tyss.optimize.common.util.CommonConstants;
import com.tyss.optimize.nlp.util.Nlp;
import com.tyss.optimize.nlp.util.NlpException;
import com.tyss.optimize.nlp.util.NlpRequestModel;
import com.tyss.optimize.nlp.util.NlpResponseModel;
import com.tyss.optimize.nlp.util.annotation.InputParam;
import com.tyss.optimize.nlp.util.annotation.ReturnType;

import io.restassured.RestAssured;
import io.restassured.response.Response;

public class Session implements Nlp {

    @InputParam(name = "Product ID", type = "java.lang.String")
    @ReturnType(name = "API Response", type = "java.lang.String")

    @Override
    public NlpResponseModel execute(NlpRequestModel request) throws NlpException {

        NlpResponseModel response = new NlpResponseModel();

        try {

            WebDriver driver = request.getWebDriver();
            String productId = request.getAttributes().get("Product ID").toString();

            if (driver == null) {
                response.setStatus(CommonConstants.fail);
                response.setMessage("WebDriver is null. Launch browser first.");
                return response;
            }

            // ====== WAIT FOR COOKIES ======
            Cookie sessionCookie = null;
            Cookie privateContentCookie = null;

            for (int i = 0; i < 15; i++) {
                sessionCookie = driver.manage().getCookieNamed("PHPSESSID");
                privateContentCookie = driver.manage().getCookieNamed("private_content_version");

                if (sessionCookie != null && privateContentCookie != null)
                    break;

                Thread.sleep(1000);
            }

            if (sessionCookie == null || privateContentCookie == null) {
                response.setStatus(CommonConstants.fail);
                response.setMessage("Required Magento cookies not found.");
                return response;
            }

            String sessionId = sessionCookie.getValue();
            String privateContent = privateContentCookie.getValue();

            // ====== GET FORM KEY ======
            String formKey = driver.findElement(By.name("form_key"))
                    .getAttribute("value");

            // ====== GENERATE UENC ======
            String currentUrl = driver.getCurrentUrl();
            String uenc = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(currentUrl.getBytes());

            // ====== API CALL ======
            RestAssured.baseURI = "https://magento.winkapis.com";

            Response apiResponse =
                given()
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Origin", "https://magento.winkapis.com")
                    .header("User-Agent",
                        ((RemoteWebDriver) driver)
                            .getCapabilities()
                            .getBrowserName())
                    .cookie("PHPSESSID", sessionId)
                    .cookie("private_content_version", privateContent)
                    .multiPart("product", productId)
                    .multiPart("form_key", formKey)
                    .multiPart("uenc", uenc)
                .when()
                    .post("/default/checkout/cart/add/")
                .then()
                    .extract()
                    .response();

            if (apiResponse.statusCode() == 200) {

                driver.navigate().refresh();

                response.setStatus(CommonConstants.pass);
                response.setMessage("Product added successfully.");
                response.getAttributes().put("API Response", apiResponse.asString());

            } else {

                response.setStatus(CommonConstants.fail);
                response.setMessage("Failed. Status Code: " + apiResponse.statusCode());
            }

        } catch (Exception e) {

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            response.setStatus(CommonConstants.fail);
            response.setMessage(sw.toString());
        }

        return response;
    }
}
