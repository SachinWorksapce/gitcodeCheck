package business_logics;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

public class MagentoHybridAddToCart implements Nlp {

    @Override
    @ReturnType(name = "Magento Curl Required Parameters", type = "java.util.Map")
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
            // 1️⃣ PARENT PRODUCT ID
            // =============================
            WebElement productElement =
                    driver.findElement(By.cssSelector("input[name='product']"));
            String parentId = productElement.getAttribute("value");

            // =============================
            // 2️⃣ FORM KEY
            // =============================
            WebElement formKeyElement =
                    driver.findElement(By.cssSelector("input[name='form_key']"));
            String formKey = formKeyElement.getAttribute("value");

            // =============================
            // 3️⃣ SUPER ATTRIBUTES (SIZE & COLOR)
            // =============================
            Map<String, String> superAttributes = new HashMap<>();

            for (WebElement element :
                    driver.findElements(By.cssSelector("[id^='option-label-']"))) {

                String id = element.getAttribute("id");

                if (id != null && element.getAttribute("class").contains("selected")) {

                    String[] parts = id.split("-");

                    String attributeId = parts[3];
                    String optionId = parts[5];

                    if (id.contains("size")) {
                        result.put("SIZE_ATTR_ID", attributeId);
                        result.put("SIZE_OPTION_ID", optionId);
                    }

                    if (id.contains("color")) {
                        result.put("COLOR_ATTR_ID", attributeId);
                        result.put("COLOR_OPTION_ID", optionId);
                    }
                }
            }

            // =============================
            // 4️⃣ BUILD FULL COOKIE STRING
            // =============================
            StringBuilder cookieBuilder = new StringBuilder();
            Set<Cookie> cookies = driver.manage().getCookies();

            for (Cookie cookie : cookies) {
                cookieBuilder.append(cookie.getName())
                        .append("=")
                        .append(cookie.getValue())
                        .append("; ");
            }

            String allCookies = cookieBuilder.toString().trim();

            // =============================
            // RETURN REQUIRED DATA ONLY
            // =============================
         // =============================
         // RETURN REQUIRED DATA ONLY
         // =============================
         result.put("PARENT_ID", parentId);
         result.put("FORM_KEY", formKey);
         result.put("COOKIES", allCookies);

         response.getAttributes().put("Magento Curl Required Parameters", result);
         response.setStatus(CommonConstants.pass);
         response.setMessage("Magento curl parameters fetched successfully.");

        } catch (Exception e) {

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            response.setStatus(CommonConstants.fail);
            response.setMessage(sw.toString());
        }

        return response;
    }
}