/*
 * Modified Album Store API
 *
 * Based on the Swagger Codegen template for the Album Store API.
 *
 * Original Details:
 * CS6650 Fall 2023
 * API version: 1.0.0
 * Contact: i.gorton@northeasern.edu
 * Generated by: Swagger Codegen (https://github.com/swagger-api/swagger-codegen.git)
 */
package swagger

import (
	"net/http"
	"encoding/json"
    "io/ioutil"
    "github.com/gorilla/mux"
    "fmt"
)

// Global DAO (Data Access Object) instance for Album operations
var dao *AlbumDAO

// Initializes the DAO for album operations on server start
func init() {
    dao = NewAlbumDAO()
}

// Handles the HTTP request to retrieve an album's information based on its albumID.
// The albumID is expected to be provided as a URL parameter.
//
// Parameters:
//   - w: The HTTP response writer.
//   - r: The HTTP request object
//
func GetAlbumByKey(w http.ResponseWriter, r *http.Request) {
    // Extract URL variables from the request.
	vars := mux.Vars(r)

    // Attempt to retrieve the albumID from the URL parameters.
    albumId, ok := vars["albumID"]
    // Check if albumID is missing or empty.
    if !ok || albumId == ""{
        // If it is, write an error response and return.
        writeErrorResponse(w, http.StatusBadRequest, "missing parameters")
        return
    }

    // Fetch album information (currently constant information).
    albumInfo := dao.GetAlbumByKey(albumId)
    
    // Convert the retrieved album information to JSON format.
	respData, err := json.Marshal(albumInfo)
    // Handle potential JSON marshalling error.
	if err != nil {
		writeErrorResponse(w, http.StatusInternalServerError, "Error marshalling JSON")
		return
	}
    // Set response headers and write the JSON response.
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	w.WriteHeader(http.StatusOK)
    w.Write(respData)
}

// Writes a standardized error message in JSON format to the provided response writer.

// Parameters:
//   - w: The HTTP response writer to which the error message will be written.
//   - statusCode: The HTTP status code to set for the error response.
//   - message: A string describing the error to be included in the JSON response.
func writeErrorResponse(w http.ResponseWriter, statusCode int, message string) {
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	w.WriteHeader(statusCode)
	resp := map[string]string{
		"error": message,
	}
	respData, _ := json.Marshal(resp)
	w.Write(respData)
}

// Handles the HTTP request to create a new album. It expects a form submission with an image with an album profile.
//
// Parameters:
//   - w: The HTTP response writer to which the result (success or error) will be written.
//   - r: The HTTP request object
//
func NewAlbum(w http.ResponseWriter, r *http.Request) {
    // Parse the submitted form to extract file parts, considering a max size of 10MB for the form data.
    err := r.ParseMultipartForm(10 << 20) 
    if err != nil {
        http.Error(w, "Unable to parse form", http.StatusBadRequest)
        return
    }

    // Retrieve the image part from the form.
    imagePart, _, err := r.FormFile("image")
    if err != nil {
        http.Error(w, "Unable to get image", http.StatusBadRequest)
        return
    }
    // Ensure the imagePart stream gets closed after processing.
    defer imagePart.Close()
    // Read the entire content of the image file into memory.
    imageData, _ := ioutil.ReadAll(imagePart)

    // Create a response containing the ID of the new album (currently constant albumID) and the size of the uploaded image.
    albumPostResponse := map[string]interface{}{
        "ID":       "newAlbumId",
        "ImageSize" : fmt.Sprintf("%d", len(imageData)),
    }

    // Convert the response data into JSON format.
    respData, err := json.Marshal(albumPostResponse)
    if err != nil {
        http.Error(w, "Error converting response to JSON", http.StatusInternalServerError)
        return
    }

    // Set the response headers and write the JSON response to the client.
    w.Header().Set("Content-Type", "application/json; charset=UTF-8")
    w.WriteHeader(http.StatusOK)
    w.Write(respData)
}
