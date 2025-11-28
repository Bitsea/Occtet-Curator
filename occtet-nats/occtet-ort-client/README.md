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
    <version>...</version>
</dependency>
``` 

Then you can use the generated API classes to interact with the ORT Server.
For example, to get a list of versions of the ORT Server, you can do the following:

```java
OrtClientService ortClientService = new OrtClientService("http://localhost:8080");
AuthService authService = new AuthService("http://localhost:8081/realms/master/protocol/openid-connect/token");

TokenResponse tokenResponse = authService.requestToken("ort-server","ort-admin","password","offline_access");

ApiClient apiClient = ortClientService.createApiClient(tokenResponse);

VersionsApi versionsApi = new VersionsApi(apiClient);
Map<String, String> versions = versionsApi.getVersions();
```
See OrtClientServiceTest.java from which this code is.
Note that the tokenResponse has a validity for (by default) 600 seconds only, so you need to get a new one when it is expired.
Just use tokenResponse.isValid() to check.
