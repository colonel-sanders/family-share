# family-share

_A demo shared vault app to explore the Web Authentication API (Webauthn) for password-free authentication_

## Prerequisites

- Java 17+

## Tech Stack

- [Spring Boot](https://spring.io/projects/spring-boot)
- [java-webauthn-server](https://github.com/Yubico/java-webauthn-server)
- Spring WebMVC + Mustache
- [H2 in-memory database](https://www.h2database.com/)
- [htmx](https://htmx.org/)
- [Picnic CSS](https://picnicss.com/)

## Getting Started

1. The client-side authentication operations require a secure context (https), so we need a TLS certificate for the embedded web server. A great approach to producing a trusted PKCS #12 cert is to install [mkcert](https://mkcert.dev/) and generate a certificate for localhost: ` mkcert -pkcs12 localhost`
2. Add the generated certificate to the classpath as `src/main/resources/localhost.p12`
3. Start the server: `./gradlew bootRun`
4. The embedded database is created with sample users. Register a new Webauthn device for this sample account by visiting `https://localhost:8080/register?name=Mom`
5. Complete device registration. You can use a physical device, a virtual authenticator such as Apple Touch ID, or an [emulated device](https://developer.chrome.com/docs/devtools/webauthn).
6. Once device registration completes, you'll be redirected to the home page. Login with your device to access the secured content.

## Operation Notes

- The app is currently logging a Hibernate error on startup due to [HHH-17612](https://hibernate.atlassian.net/browse/HHH-17612). The app still functions properly.
