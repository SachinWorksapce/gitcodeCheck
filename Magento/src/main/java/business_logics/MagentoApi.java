package business_logics;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.tyss.optimize.common.util.CommonConstants;
import com.tyss.optimize.nlp.util.Nlp;
import com.tyss.optimize.nlp.util.NlpException;
import com.tyss.optimize.nlp.util.NlpRequestModel;
import com.tyss.optimize.nlp.util.NlpResponseModel;
import com.tyss.optimize.nlp.util.annotation.ReturnType;

public class MagentoApi implements Nlp {

    @Override
    @ReturnType(name = "Magento Add To Cart Full Parameters", type = "java.util.Map")
    public NlpResponseModel execute(NlpRequestModel nlpRequestModel) throws NlpException {

        NlpResponseModel response = new NlpResponseModel();

        try {

            WebDriver driver = nlpRequestModel.getWebDriver();
            if (driver == null) {
                response.setStatus(CommonConstants.fail);
                response.setMessage("Driver is NULL.");
                return response;
            }

            Thread.sleep(2000);

            Map<String, Object> result = new HashMap<>();

            // =============================
            // 1️⃣ PRODUCT ID
            // =============================
            WebElement productElement =
                    driver.findElement(By.cssSelector("input[name='product']"));
            String productId = productElement.getAttribute("value");

            // =============================
            // 2️⃣ FORM KEY
            // =============================
            Cookie formKeyCookie = driver.manage().getCookieNamed("form_key");
            if (formKeyCookie == null) {
                response.setStatus(CommonConstants.fail);
                response.setMessage("form_key not found.");
                return response;
            }
            String formKey = formKeyCookie.getValue();

            // =============================
            // 3️⃣ SESSION COOKIE
            // =============================
            Cookie sessionCookie = driver.manage().getCookieNamed("PHPSESSID");
            if (sessionCookie == null) {
                sessionCookie = driver.manage().getCookieNamed("mage-cache-sessid");
            }

            String sessionId = sessionCookie != null ? sessionCookie.getValue() : "";

            // =============================
            // 4️⃣ UENC (Base64 Current URL)
            // =============================
            String currentUrl = driver.getCurrentUrl();
            String uenc = Base64.getEncoder()
                    .encodeToString(currentUrl.getBytes(StandardCharsets.UTF_8));

            // =============================
            // 5️⃣ SUPER ATTRIBUTES
            // =============================
            List<WebElement> superAttributes =
                    driver.findElements(By.cssSelector("select[name^='super_attribute']"));

            Map<String, String> attributeMap = new HashMap<>();

            for (WebElement element : superAttributes) {

                String name = element.getAttribute("name");
                String value = element.getAttribute("value");

                if (value != null && !value.isEmpty()) {
                    attributeMap.put(name, value);
                }
            }

            // =============================
            // 6️⃣ BUILD API URL
            // =============================
            String apiUrl =
                    "https://magento.winkapis.com/default/checkout/cart/add/"
                            + "uenc/" + uenc + "/"
                            + "product/" + productId + "/";

            // =============================
            // 7️⃣ COOKIE HEADER
            // =============================
            String cookieHeader =
                    "PHPSESSID=" + sessionId + "; form_key=" + formKey;

            // =============================
            // 8️⃣ FORM BODY
            // =============================
            StringBuilder formBody = new StringBuilder();
            formBody.append("product=").append(productId)
                    .append("&form_key=").append(formKey)
                    .append("&uenc=").append(uenc);

            for (Map.Entry<String, String> entry : attributeMap.entrySet()) {
                formBody.append("&")
                        .append(entry.getKey())
                        .append("=")
                        .append(entry.getValue());
            }

            // =============================
            // RETURN EVERYTHING
            // =============================
            result.put("api_url", apiUrl);
            result.put("cookie_header", cookieHeader);
            result.put("form_body", formBody.toString());
            result.put("product", productId);
            result.put("form_key", formKey);
            result.put("uenc", uenc);
            result.putAll(attributeMap);

            //response.setAttributes(result.toString());
            response.setStatus(CommonConstants.pass);
            response.setMessage("Magento Add-To-Cart parameters generated successfully.");

        } catch (Exception e) {

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            response.setStatus(CommonConstants.fail);
            response.setMessage(sw.toString());
        }

        return response;
    }
}