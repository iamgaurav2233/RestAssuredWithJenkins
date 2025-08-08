package com.example;

import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// Static imports for REST Assured's BDD syntax and Hamcrest matchers
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contains advanced API tests for the CarWale negotiation endpoint.
 */
public class CarWaleApiTest {

        @BeforeAll
        static void setup() {
                // Set the base URI for all tests in this class
                RestAssured.baseURI = "https://www.carwale.com/api/stocks/negotiate/price";

                // Add a common User-Agent header to mimic a browser and avoid potential 403
                // errors
                RestAssured.requestSpecification = RestAssured.given()
                                .header("User-Agent",
                                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        }

        @Test
        @DisplayName("Test 1: Basic Response Validation")
        void testResponseStatusCodeAndBasicStructure() {
                given()
                                // No additional setup needed due to @BeforeAll
                                .when()
                                .get("/799999")
                                .then()
                                .log().ifError()
                                // 1. Verify the HTTP status is 200 OK
                                .statusCode(200)
                                // 2. Verify the response is JSON
                                .contentType("application/json")
                                // 3. Verify the root JSON object has the 'offerList' key
                                .body("$", hasKey("offerList"))
                                // 4. Verify the 'offerList' is not empty
                                .body("offerList", not(empty()));
        }

        @Test
        @DisplayName("Test 2: GPath for Data Content Validation")
        void testOfferListContentUsingGPath() {
                given()
                                .when()
                                .get("/799999")
                                .then()
                                .statusCode(200)
                                // 1. Verify the first offer's key matches the requested price
                                .body("offerList[0].key", equalTo("799999"))
                                // 2. Verify the first offer's value
                                .body("offerList[0].val", equalTo("₹ 8 Lakh"))
                                // 3. Use a GPath closure to find a specific offer and check its key
                                // 'it' refers to each element in the 'offerList' collection
                                .body("offerList.find { it.val == '₹ 7 Lakh' }.key", equalTo("700000"))
                                // 4. Check the total number of offers in the list
                                .body("offerList.size()", equalTo(12));
        }

        @Test
        @DisplayName("Test 3: Advanced Data Integrity - Prices are in Descending Order")
        void testPriceKeysAreInDescendingOrder() {
                // 1. Extract the list of price 'keys' from the JSON response
                List<String> priceKeysAsString = when()
                                .get("/799999")
                                .then()
                                .statusCode(200)
                                .extract().path("offerList.key");

                // 2. Convert the list of strings to a list of integers
                List<Integer> prices = priceKeysAsString.stream()
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());

                // 3. Create a sorted copy of the list for comparison
                List<Integer> sortedPrices = prices.stream()
                                .sorted(Collections.reverseOrder())
                                .collect(Collectors.toList());

                // 4. Assert that the original list is the same as the sorted list
                // This confirms the data from the API is already correctly ordered.
                assertThat(prices).isEqualTo(sortedPrices);
        }

        // --- POJO Deserialization Approach (Cleaner & More Maintainable) ---

        // Helper class representing a single offer
        public static class Offer {
                public String key;
                public String val;
        }

        // Helper class to wrap the list for correct deserialization
        public static class OfferList {
                public List<Offer> offerList;
        }

        @Test
        @DisplayName("Test 4: Deserialization to POJOs for Type-Safe Assertions")
        void testResponseDeserialization() {
                // 1. Extract the entire body and map it to our wrapper OfferList class
                OfferList responseData = when()
                                .get("/799999")
                                .then()
                                .statusCode(200)
                                .extract().body().as(OfferList.class);

                // 2. Now get the list from the wrapper object
                List<Offer> offers = responseData.offerList;

                // 3. Now perform assertions on the Java objects, which is much cleaner
                assertThat(offers).isNotNull();
                assertThat(offers.size()).isEqualTo(12);

                // 4. Assert properties of the first offer
                Offer firstOffer = offers.get(0);
                assertThat(firstOffer.key).isEqualTo("799999");
                assertThat(firstOffer.val).isEqualTo("₹ 8 Lakh");

                // 5. Use Java streams to find a specific offer
                Offer specificOffer = offers.stream()
                                .filter(o -> o.val.equals("₹ 7 Lakh"))
                                .findFirst()
                                .orElse(null);

                assertThat(specificOffer).isNotNull();
                assertThat(specificOffer.key).isEqualTo("700000");
        }
}
