package org.backmeup.index.integration;

import org.junit.Ignore;

@Ignore("only integration with running and deployed Indexer WAR")
public class IndexIntegrationTest {

}

// public class IndexIntegrationTest extends IntegrationTestBase {
//
// @Test
// public void testAddUser() {
// String username = "john.doe";
// String firstname = "John";
// String lastname = "Doe";
// String password = "password1";
// String email = "TestUser@trash-mail.com";
//
// String accessToken;
//
// UserDTO newUser = new UserDTO(username, firstname, lastname, password,
// email);
//
// ValidatableResponse response = null;
// try {
// response = given().log().all().header("Accept", "application/json")
// .body(newUser, ObjectMapperType.JACKSON_1).when()
// .post("/users/").then().log().all().statusCode(200)
// .body("username", equalTo(username))
// .body("firstname", equalTo(firstname))
// .body("lastname", equalTo(lastname))
// .body("password", equalTo(null))
// .body("email", equalTo(email))
// .body("activated", equalTo(true))
// .body(containsString("userId"));
// } finally {
// String userId = response.extract().path("userId").toString();
// accessToken = userId + ";" + password;
// BackMeUpUtils.deleteUser(accessToken, userId);
// }
// }
// }
