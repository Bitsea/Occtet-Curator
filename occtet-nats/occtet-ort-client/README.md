# Occtet ORT Client Library

Use this to connect to an ORT Server (https://github.com/eclipse-apoapsis/ort-server).

## Building

To build the project and install it in your local .m2 repository, execute:

`mvn clean install`

## How it works

This library contains generated code from the ORT Server OpenAPI specification ONLY.
Do NOT add any custom code here, because it will be overwritten when the code is regenerated.

The code is generated using the OpenAPI Generator Maven Plugin based on the openapi.json file in the resources directory.

When the ORT API changes and you need to regenerate the client code, checkout the ORT server project and execute:

`./gradlew :core:generateOpenApiSpec`

## VERY VERY IMPORTANT!!!

Then you need to tweak the generated code because it does not work out of the box!

Replace all instances of  `"null", "object"` by `"object"` (all in `"type" : [...]` constructs).

(Reason: when the `"null"` is included, the generator does not create a Map<> but a ModelNull<> which does not exist)

## Usage

To use the library in your project, add the dependency to your pom.xml:

```xml
<dependency>
    <groupId>eu.occtet.boc</groupId>
    <artifactId>occtet-ort-client</artifactId>
    <version>2025.10.1</version>
</dependency>
``` 

Then you can use the generated API classes to interact with the ORT Server.
For example, to get a list of projects:

```java
import eu.occtet.boc.ort.client.ApiClient;
import eu.occtet.boc.ort.client.ApiException;
import eu.occtet.boc.ort.client.api.ProjectsApi;
import eu.occtet.boc.ort.client.model.ProjectList;  

public class OrtClientExample {
    public static void main(String[] args) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://localhost:8082/api"); // Set the base path to your ORT server

        ProductsApi api = new ProductsApi(apiClient);

        try {
            PagedSearchResponseProductVulnerabilityVulnerabilityForRunsFilters result = api.getProductVulnerabilities(...);
            ...
        } catch (ApiException e) {
            
            e.printStackTrace();    
        }
    }
}
```

Please note that the above example is untested.