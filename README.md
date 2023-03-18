# Spring Boot Web Application Demo
Note: Several of the links in this document point to http://localhost:8080 and will work if this demo application is currently running.

---
## Pragmatic RESTful Web Application
The purpose of this project is to demonstrate functional code examples of Pragmatic REST practices, using Spring Boot to
degrease development time, increase testability, increase code and API consistency, and increase out-of-the-box support
for running in containers like Docker. 

### Built with:
* [Spring Boot Maven](https://docs.spring.io/spring-boot/docs/current/maven-plugin/reference/html/) as a framework
* [Spring Web](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#web) for the RESTful web layer
* [Spring Data JPA](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#data.sql.jpa-and-spring-data) for paging support and repository layer
* [Validation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#io.validation) for input validation handling
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#actuator) for monitoring, health, info, etc
* [Cloud Sleuth](https://docs.spring.io/spring-cloud-sleuth/docs/current/reference/htmlsingle/spring-cloud-sleuth.html) for request tracing
* [Prometheus](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#actuator.metrics.export.prometheus) for graphing/alerting integration
* [Thymeleaf](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#web.servlet.spring-mvc.template-engines) for page templating, like [Error](http://localhost:8080/error) and [Welcome](http://localhost:8080) pages
* [Springdoc](https://springdoc.org) for dynamically generated open-api [Swagger](http://localhost:8080/swagger-ui/index.html) and [Redoc](http://localhost:8080/redoc.html) documentation

### Running the Application:
* Build and run through IDE, or
* From command line:
  * Build:
    ```shell
    mvn package -DskipTests
    ```
  * Run:
    ```bash
    java -jar target/demo-spring-web-service-*.jar
    ```

---
## Guide for creating a pragmatic RESTful API:
This article is an excellent place for everyone to start. I agree with, and practice most of what the author suggests. 

* https://www.vinaysahni.com/best-practices-for-a-pragmatic-restful-api

### Some highlights that are included in this demo: 

#### [Use RESTful nouns/items and verbs/actions](https://www.vinaysahni.com/best-practices-for-a-pragmatic-restful-api#restful)
All endpoints will follow the path to the item(s) they return.
From the above example, `GET /tickets` will return all tickets, `GET /tickets/12/messages` will return the messages for the specified ticket.
When developing this path, try pretending that you're navigating through a giant JSON document that contains all of your systems' data.
Even though each node isn't necessarily accessible in a RESTful way, it can help to get the pathing in an order that can be easily expanded later.
Imagine a document for a given endpoint and if the document doesn't make sense, probably the endpoint path doesn't either.
For an endpoint like `GET /departments/sales/orders/13/comments/1`:
```
{ "departments": [
    { "sales": {
        "orders": [
          { "id": 13, "comments": [{ "id": 1, "content": "This is a great sale!" }]}
        ]}
    }
]}
```

#### [Versioning through request path](https://www.vinaysahni.com/best-practices-for-a-pragmatic-restful-api#versioning)
Spring makes this easy, since your `@RequestMapping` at the RestController class-level can use Spring Placeholder variables:
`@RequestMapping(path = "${server.rest.version}/greetings")` and will route paths accordingly. The annotation also takes an array of paths:
`@RequestMapping(path = {"/v1/greetings", "/v2/greetings"})`. Each method `@GetMapping`, `@PostMapping`, etc. also has the same features;
they can use Spring Placeholder variables and take an array of paths.

The Controllers/Endpoint-Methods can be tagged in [Micrometer](https://spring.io/blog/2018/03/16/micrometer-spring-boot-2-s-new-application-metrics-collector) to allow for differentiating between versions in graphs and alerts.
`@Timed(value = "http.greetings.requests", extraTags = {"version", "1"}, description = "/greetings")` 

#### [Sort, Filter, and Page through query parameters](https://www.vinaysahni.com/best-practices-for-a-pragmatic-restful-api#advanced-queries)
Spring Data JPA does some heavy-lifting for Paging, but is missing some support when building a pragmatic REST service.
Accepting paging query parameters is extremely easy. Add `Pageable pageable` parameter to any `@GetMapping` method and
`page=`, `sort=`, and `size=` query parameters will map to the `Pageable` input. The Pageable object can simply be passed
along to the JPA `PagingAndSortingRepository` with no additional work.

Returning a Page of data is where some support is missing. The Controller endpoint method can call `page.get()` and return
the array of data, which is nice, but the page values have been lost.

#### [Paginate with Link headers (RFC 8228)](https://www.vinaysahni.com/best-practices-for-a-pragmatic-restful-api#pagination)
At the time of this README, there isn't any built-in conversion from a Page return value to an Array of values with Link headers.
To use Paged results in a pragmatic way, this project includes
[UnwrappedPageHttpMessageConverter](src/main/java/org/watson/demos/converters/UnwrappedPageHttpMessageConverter.java) and
[UnwrappedPageResponseBodyAdvice](src/main/java/org/watson/demos/advice/UnwrappedPageResponseBodyAdvice.java). When enabled
by the property `server.response.unwrap.page=true`, then enable the developer to return a `Page<SomeModelClass>` from the controller endpoint method. The resulting response will
contain a JSON array of `SomeModelClass` and Link headers to control paging, in accordance with [RFC 8288](https://www.rfc-editor.org/rfc/rfc5988#section-5).

#### [Return a resource representation (RFC 2616)](https://www.vinaysahni.com/best-practices-for-a-pragmatic-restful-api#useful-post-responses)
Endpoints with a 201 CREATED status will automatically have their Location or Content-Location header written by the [LocationResponseBodyAdvice](/src/main/java/org/watson/demos/advice/LocationResponseBodyAdvice.java),
in accordance with [RFC 2612 14.14](https://www.rfc-editor.org/rfc/rfc2616#section-14.14) and [RFC 2612 14.30](https://www.rfc-editor.org/rfc/rfc2616#section-14.30).

---
## Spring Actuator
Services that have enabled Spring Actuator have access to built-in production-ready features, like health-checks, metrics, etc.
See the [Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/actuator-api/htmlsingle/) for details.

### Health
Health, Liveness, and Readiness probes have been enabled in this application through [application.properties](src/main/resources/application.properties).
* `management.endpoint.health.probes.enabled=true`
* `management.health.livenessState.enabled=true`
* `management.health.readinessState.enabled=true`

#### Health Probes
* [actuator/health](http://localhost:8080/actuator/health)
* [actuator/health/liveness](http://localhost:8080/actuator/health/liveness)
* [actuator/health/readiness](http://localhost:8080/actuator/health/readiness)

#### Availability Endpoints
For demonstration purposes, the [AvailabilityController](src/main/java/org/watson/demos/controllers/AvailabilityController.java) 
has been added to externally change the state of the health-probes. These endpoints can be accessed through [Swagger](http://localhost:8080/swagger-ui/index.html) or the [Welcome Page](http://localhost:8080).
* Health
  ```shell
  curl -X 'PATCH' http://localhost:8080/v1/availability/health/UP
  ```
  ```shell
  curl -X 'PATCH' http://localhost:8080/v1/availability/health/DOWN
  ```
  ```shell
  curl -X 'PATCH' http://localhost:8080/v1/availability/health/OUT_OF_SERVICE
  ```
  ```shell
  curl -X 'PATCH' http://localhost:8080/v1/availability/health/UNKNOWN
  ```
* Liveness
  ```shell
  curl -X 'PATCH' http://localhost:8080/v1/availability/liveness/CORRECT
  ```
  ```shell
  curl -X 'PATCH' http://localhost:8080/v1/availability/liveness/BROKEN
  ```
* Readiness
  ```shell
  curl -X 'PATCH' http://localhost:8080/v1/availability/readiness/ACCEPTING_TRAFFIC
  ```
  ```shell
  curl -X 'PATCH' http://localhost:8080/v1/availability/readiness/REFUSING_TRAFFIC
  ```

---
## Error Responses
This application uses Thymeleaf to create the `/error` [page template](src/main/resources/templates), featuring dynamic fields.
Spring Web Services can be configured to allow for additional dynamically added fields in error responses:
* `server.error.include-message=on_param`
* `server.error.include-binding-errors=on_param`
* `server.error.include-stacktrace=on_param`

The optional fields can be added to the error response by adding query parameters to the request.
* `&trace&message&errors`

---
## Additional Resources

### Spring Tutorials:
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Handling Form Submission](https://spring.io/guides/gs/handling-form-submission/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Validation](https://spring.io/guides/gs/validating-form-input/)


### Interesting Things To Add
* Request-scoped bean/user-cache
* WebClient with Wire-Mock-Style tests
* Imported dev properties, as described in: https://spring.io/blog/2020/08/14/config-file-processing-in-spring-boot-2-4
* Micrometer Custom Metrics
