package com.jayway.restassured

import groovyx.net.http.HttpResponseException
import groovyx.net.http.HTTPBuilder

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.URLENC
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import groovyx.net.http.Method
import org.hamcrest.Matcher
import com.jayway.restassured.assertion.Assertion
import com.jayway.restassured.assertion.JSONAssertion

class RequestBuilder {

  String baseUri
  String path
  String port
  Method method;
  Map query

  private Assertion assertion;

  def thenAssertThat(String key, Matcher<?> matcher) {
    assertion = new JSONAssertion(key: key)
    def assertionClosure = { response, json -> matcher.matches(assertion.getResult(json)) }
    sendRequest(path, method, query, assertionClosure, { resp ->
      throw new RuntimeException("Unexpected error: ${resp.statusLine.statusCode} : ${resp.statusLine.reasonPhrase}")
    });
  }

  def then(Closure assertionClosure) {
    sendRequest(path, method, query, assertionClosure, { resp ->
      throw new RuntimeException("Unexpected error: ${resp.statusLine.statusCode} : ${resp.statusLine.reasonPhrase}")
    });
  }

  def withParameters(Map map) {
   return new RequestBuilder(baseUri: RestAssured.baseURI, path: path, port: port, method: POST, query: map) 
  }


  private def sendRequest(path, method, query, successHandler, failureHandler) {
    def http = new HTTPBuilder("$baseUri:$port")
    if(POST.equals(method)) {
      try {
        http.post( path: path, body: query,
                requestContentType: URLENC ) { response, json ->
          if(successHandler != null) {
            successHandler.call (response, json)
          }
        }
      } catch(HttpResponseException e) {
        if(failureHandler != null) {
          failureHandler.call(e.getResponse())
        } else {
          throw e;
        }
      }
    } else if(GET.equals(method)) {
      http.request(method, JSON ) {
        uri.path = path
        if(query != null) {
          uri.query = query
        }
        // response handler for a success response code:
        response.success = successHandler

        // handler for any failure status code:
        response.failure = failureHandler
      }
    } else {
      throw new IllegalArgumentException("Only GET and POST supported")
    }
  }
}
