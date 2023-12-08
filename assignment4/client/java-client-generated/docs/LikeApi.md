# LikeApi

All URIs are relative to *https://virtserver.swaggerhub.com/IGORTON/AlbumStore/1.1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**review**](LikeApi.md#review) | **POST** /review/{likeornot}/{albumID} | 

<a name="review"></a>
# **review**
> review(likeornot, albumID)



like or dislike album

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.LikeApi;


LikeApi apiInstance = new LikeApi();
String likeornot = "likeornot_example"; // String | like or dislike album
String albumID = "albumID_example"; // String | albumID
try {
    apiInstance.review(likeornot, albumID);
} catch (ApiException e) {
    System.err.println("Exception when calling LikeApi#review");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **likeornot** | **String**| like or dislike album |
 **albumID** | **String**| albumID |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

